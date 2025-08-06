
import os
import json
import redis.asyncio as redis
import uvicorn
from fastapi import FastAPI, HTTPException, status, WebSocket, WebSocketDisconnect
import asyncio
from pydantic import BaseModel
import traci
import time
from typing import List, Optional
from event_manager import EventManager
import traceback
from junction_data_processor import JunctionDataProcessor

# Read JSON configuration file
with open('config.json', 'r') as f:
    config = json.load(f)
# SUMO
sumo_config = config.get('sumo_settings', {})
sumoBinary = sumo_config.get('binary_path')
sumo_cfg_file = sumo_config.get('config_file_path')
TRACI_PORT = 8813
sumoCmd = [sumoBinary, "-c", sumo_cfg_file, "--start"]

# Get Redis configuration from the parsed dictionary
redis_config = config.get('redis', {})
REDIS_HOST = redis_config.get('host')
REDIS_PORT = redis_config.get('port')
REDIS_DB = redis_config.get('db')

SQL_FILE_PATH = "junction_flow_relations.sql"
TARGET_EDGE_ID_TO_MONITOR = "542295429#6"

# Pydantic Models for Request Bodies
# This tells FastAPI how to validate and parse the incoming JSON
class DurationPayload(BaseModel):
    junctionId: str
    duration: int

class StateDurationPayload(BaseModel):
    junctionId: str
    state: str
    duration: int
    lightIndex: int

# Create FastAPI application and global variables
app = FastAPI(
    title="TraCI service",
    description="A model to provide SUMO simulation data and control",
    version="1.0.0"
)

connection_status = {"sumo_connected": False, "redis_connected": False, "message": "Initializing..."}
TRACI_LOCK = asyncio.Lock()
redis_client: redis.Redis = None
# Expiration time for cached data in Redis (seconds)
REDIS_EXPIRATION_SECONDS = 600

# Structure: { "tls_id": {"state": "AWAITING_VERIFICATION", "data": {...}, "execution_time": ...}, ... }
TASK_SCHEDULER = {}
simulation_task = None
stop_simulation_event = asyncio.Event()
VERIFICATION_STATE = {}
verification_events = {}

# Redis Key Constants
# Use constants to manage Redis key names for easier maintenance
KEY_SIM_TIME = "sumo:simulation_time"
KEY_ALL_EDGES = "sumo:edge"
KEY_ALL_TLS = "sumo:tls"
KEY_EMERGENCY_VEHICLES = "sumo:emergency_vehicles"
KEY_ALL_JUNCTIONS = "sumo:junction"

# Global variables to store static data generated at startup
junction_names_map = {}
tls_conflict_maps = {}
junction_to_tls_map = {}
junction_processor: Optional[JunctionDataProcessor] = None

# Create a global instance of EventManager
event_manager = EventManager()


# Advance Simulation
async def simulation_loop(processor):
    print("Background scheduler loop has started.")

    while not stop_simulation_event.is_set():
        print("[SimLoop] Loop started, waiting for lock...")

        events_to_set = []
        edges_to_cache = {}
        tls_to_cache = {}
        junctions_to_cache = {}
        emergency_vehicles_to_cache = {}
        redis_ev_keys_to_delete = []

        # Add a flag to determine whether TraCI step succeeded
        traci_step_success = False

        async with TRACI_LOCK:
            print("[SimLoop] Lock acquired.")
            try:
                # 1. Advance the simulation by one step
                print("[SimLoop] Calling traci.simulationStep()...")
                traci.simulationStep()
                current_time = traci.simulation.getTime()
                print(f"current time is {current_time}")
                sim_time_to_cache = current_time

                # 2. Check and execute tasks in the scheduler
                if TASK_SCHEDULER:
                    print(f"[SimLoop] Found tasks: {list(TASK_SCHEDULER.keys())}")
                    for tls_id, task in list(TASK_SCHEDULER.items()):
                        task_state = task.get("state")
                        print(f"[SimLoop] > Processing task '{tls_id}'，state: '{task_state}'")

                        verification_event = task.get("verification_event")
                        verification_result = task.get("verification_result", {})

                        if task_state == "AWAITING_VERIFICATION":
                            expected_state = task["data"]["state"]
                            print(f"[SimLoop] > > Task '{tls_id}' awaiting verification...")
                            verified_state = traci.trafficlight.getRedYellowGreenState(tls_id)
                            print(
                                f"[SimLoop] > > > Expected state: '{expected_state}', actual state retrieved: '{verified_state}'")

                            if verified_state == task["data"]["state"]:
                                print(f"Task '{tls_id}' ready be verified.")
                                verification_result["status"] = "VERIFIED_AND_RUNNING"
                                verification_result["detail"] = f"State for {tls_id} successfully set and verified."
                                traci.trafficlight.setPhaseDuration(tls_id, task["data"]["duration"] - 1)
                                task["state"] = "RUNNING_MANUAL_PHASE"
                                task["execution_time"] = current_time + task["data"]["duration"] - 1
                            else:
                                error_message = f"Expected state '{task['data']['state']}' but got '{verified_state}'"
                                verification_result["status"] = "FAILED_VERIFICATION"
                                verification_result["detail"] = error_message
                                print(f"Task [{tls_id}]: Verification failed! {error_message}")
                                del TASK_SCHEDULER[tls_id]

                            if verification_event:
                                print(f"[SimLoop] > > > Adding '{tls_id}' to the to-be-set list")
                                events_to_set.append(verification_event)

                        elif task_state == "RUNNING_MANUAL_PHASE":
                            if current_time >= task.get("execution_time", float('inf')):
                                traci.trafficlight.setProgram(tls_id, "0")
                                print(
                                    f"Task[{tls_id}]: Time's up. Default program has been restored silently. Task lifecycle ended.")
                                del TASK_SCHEDULER[tls_id]

                # 3. Check special events
                print("[SimLoop] Starting checking special events...")
                await event_manager.check_for_expired_events()

                # 4. Update data to Redis
                print("[SimLoop] Starting data update...")

                for edgeID in traci.edge.getIDList():
                    waiting_vehicle_count = traci.edge.getLastStepHaltingNumber(edgeID)
                    waiting_time = traci.edge.getWaitingTime(edgeID)
                    avg_waiting_time = (waiting_time / waiting_vehicle_count) if waiting_vehicle_count > 0 else 0.0
                    edge_data = {
                        "edgeID": edgeID,
                        "edgeName": traci.edge.getStreetName(edgeID) or "",
                        "timestamp": current_time,
                        "laneNumber": traci.edge.getLaneNumber(edgeID) or 0,
                        "speed": traci.edge.getLastStepMeanSpeed(edgeID) or 0.0,
                        'vehicleCount': traci.edge.getLastStepVehicleNumber(edgeID) or 0,
                        'vehicleIDs': list(traci.edge.getLastStepVehicleIDs(edgeID) or []),
                        'waitingTime': avg_waiting_time or 0.0,
                        'occupancy': traci.edge.getLastStepOccupancy(edgeID) or 0.0,
                        "waitingVehicleCount": waiting_vehicle_count or 0
                    }
                    edges_to_cache[edgeID] = json.dumps(edge_data)

                for tlsID in traci.trafficlight.getIDList():
                    # Provide default for correct_junction_id
                    correct_junction_id = tlsID
                    for junc_id, t_id in junction_to_tls_map.items():
                        if t_id == tlsID:
                            correct_junction_id = junc_id
                            break
                    junction_name = junction_names_map.get(correct_junction_id,
                                                           f"Unknown Junction ({correct_junction_id})")
                    nextSwitch = traci.trafficlight.getNextSwitch(tlsID)
                    nextSwitchTime = nextSwitch - current_time
                    tls_data = {
                        "tlsID": tlsID, "junction_id": correct_junction_id, "junction_name": junction_name,
                        "timestamp": current_time, "phase": traci.trafficlight.getPhase(tlsID) or 0,
                        "state": traci.trafficlight.getRedYellowGreenState(tlsID) or "",
                        "duration": traci.trafficlight.getPhaseDuration(tlsID) or 0.0,
                        "connection": traci.trafficlight.getControlledLinks(tlsID) or [],
                        "spendTime": traci.trafficlight.getSpentDuration(tlsID) or 0.0,
                        "nextSwitchTime": nextSwitchTime or 0.0
                    }
                    tls_to_cache[tlsID] = json.dumps(tls_data)

                print("[SimLoop] Starting junction data collection...")
                junctions_to_cache = processor.calculate_all_junctions_metrics(current_time)

                print("[SimLoop] Starting emergency vehicle datat update .....")
                emergency_vehicles_to_cache, redis_ev_keys_to_delete = \
                    await event_manager.track_active_emergency_vehicles(current_time, tls_to_cache, junction_to_tls_map)

                traci_step_success = True

            except traci.TraCIException as step_error:
                print(f"[SimLoop] Connection to SUMO lost during simulation step: {step_error}")
                print("[SimLoop] Will attempt to reconnect in the next loop...")
                connection_status["sumo_connected"] = False
                connection_status["message"] = "SUMO connection lost during simulation."

        # 5. Send notifications immediately
        if events_to_set:
            print(f"[SimLoop] Sending {len(events_to_set)} notifications immediately...")
            for event in events_to_set:
                event.set()

        # Only update Redis if TraCI step succeeded
        if traci_step_success:
            try:
                print("[SimLoop] Background data packaging complete, starting Redis cache update...")
                async with redis_client.pipeline(transaction=False) as pipe:
                    pipe.set(KEY_SIM_TIME, sim_time_to_cache, ex=REDIS_EXPIRATION_SECONDS)
                    if edges_to_cache:
                        pipe.hset(KEY_ALL_EDGES, mapping=edges_to_cache)
                        pipe.expire(KEY_ALL_EDGES, REDIS_EXPIRATION_SECONDS)
                    if tls_to_cache:
                        pipe.hset(KEY_ALL_TLS, mapping=tls_to_cache)
                        pipe.expire(KEY_ALL_TLS, REDIS_EXPIRATION_SECONDS)
                    if junctions_to_cache:
                        pipe.hset(KEY_ALL_JUNCTIONS, mapping=junctions_to_cache)
                        pipe.expire(KEY_ALL_JUNCTIONS, REDIS_EXPIRATION_SECONDS)
                    if emergency_vehicles_to_cache:
                        pipe.hset(KEY_EMERGENCY_VEHICLES, mapping=emergency_vehicles_to_cache)
                        pipe.expire(KEY_EMERGENCY_VEHICLES, REDIS_EXPIRATION_SECONDS)
                    if redis_ev_keys_to_delete:
                        pipe.hdel(KEY_EMERGENCY_VEHICLES, *redis_ev_keys_to_delete)

                    print(
                        "[SimLoop] Background process starting to send all commands to the redis server for execution...")
                    start_time = time.monotonic()
                    await pipe.execute()
                    end_time = time.monotonic()
                    print(f"[SimLoop] pipe.execute() finished, duration: {end_time - start_time:.4f} seconds")
                print("[SimLoop] Redis cache update complete.")

            except redis.RedisError as e:
                print(f"[SimLoop] Redis Error: {e}")

        await asyncio.sleep(0.1)

#Start and connect to SUMO and Redis
@app.on_event("startup")
async def start_simulation_and_connect():
    global redis_client
    # Connect to Redis
    try:
        redis_url = f"redis://{REDIS_HOST}:{REDIS_PORT}/{REDIS_DB}"
        print(f"[Start Up] Connecting to Redis ({redis_url})...")
        redis_client = redis.from_url(redis_url, decode_responses=True)
        await redis_client.ping()
        connection_status["redis_connected"] = True
        print("[Start Up] Successfully connected to Redis。")
    except redis.RedisError as e:
        connection_status["message"] = f"Failed to connect to Redis: {e}"
        print(connection_status["message"])
        # If Redis connection fails, we can choose not to proceed with starting SUMO
        return

    # 2. Use traci.start to launch and connect to SUMO
    try:
        print("[Start Up] Launching SUMO process using traci.start...")
        # traci.start will launch the process defined in sumoCmd and automatically connect to the specified port
        traci.start(sumoCmd, port=TRACI_PORT)
        connection_status["sumo_connected"] = True
        connection_status["message"] = "Service is ready"
        print(f"[Start Up] Successfully launched and connected to SUMO (Port: {TRACI_PORT})。")

        # After SUMO starts, immediately execute internal initialization tasks
        print("[Start Up] SUMO connected, starting one-time initialization tasks...")
        generate_and_send_junction_names()
        build_all_conflict_maps()
        build_junction_tls_maps()
        verify_official_junction_names()
        print("[Start Up] Initializing Junction Data Processor...")
        junction_processor = JunctionDataProcessor(SQL_FILE_PATH, junction_to_tls_map)
        junction_processor.load_and_process_data()
        print("[Start Up] One-time initialization tasks completed.")
    except Exception as e:
        connection_status["message"] = f"Failed to start SUMO: {e}"
        print(connection_status["message"])
        # If SUMO fails to start, close the established Redis connection
        if redis_client:
            await redis_client.close()
        return

    # 3. Start the background simulation task
    stop_simulation_event.clear()
    simulation_task = asyncio.create_task(simulation_loop(junction_processor))
    print("[FastAPI] Background simulation task has been created.")

"""Check if the service is ready"""
@app.get("/status", summary="Check if the service is ready")
async def get_status():
    return {"connection": connection_status}

"""Get the complete status of a specific edge"""
async def get_edge_status(edgeID):
    # Get data for the specified edgeID from the 'sumo:edges' hash
    edge_data_json = await redis_client.hget(KEY_ALL_EDGES, edgeID)
    if edge_data_json:
        return {"edgeID": edgeID, "edgeData": json.loads(edge_data_json)}
    else:
        # If the edge is not found in the hash, return null data
        return {"edgeID": edgeID, "edgeData": None}


"""Read the latest status of a specific traffic light"""
async def get_tls_status(tlsID):
    # Get data for the specified tlsID from the 'sumo:tls' hash
    light_data_json = await redis_client.hget(KEY_ALL_TLS, tlsID)
    if light_data_json:
        return {"tlsID": tlsID, "lightData": json.loads(light_data_json)}
    else:
        # If the traffic light is not found in the hash, return null data
        return {"tlsID": tlsID, "lightData": None}

# Mode 1: Modify duration only
@app.post("/trafficlight/set_duration", summary="Restore the default state of a specific traffic light")
async def modify_tls_duration(payload: DurationPayload):
    tls_id = junction_to_tls_map.get(payload.junctionId)
    async with TRACI_LOCK:
        print(f"[API {tls_id}] Received request: duration={payload.duration}")
        traci.trafficlight.setPhaseDuration(tls_id, payload.duration)
        return {"status": "success", "junctionId": payload.junctionId, "duration_set": payload.duration}


# Mode 2: Modify both state and duration
@app.post("/trafficlight/set_state_duration", summary="Modify the state of a specific traffic light")
async def modify_tls_state_duration(payload: StateDurationPayload):
    junctionId = payload.junctionId
    state = payload.state
    duration = payload.duration
    index = payload.lightIndex
    tls_id = junction_to_tls_map.get(junctionId)
    print(f"[API {tls_id}] Received request: state='{state}', duration={duration}, index={index}")

    current_state = await get_tls_status(tls_id)
    current_state_str = current_state["lightData"].get("state")
    print(f"[API {tls_id}] Retrieved current state: {current_state_str}")

    state_list = list(current_state_str)

    if index >= len(state_list):
        return {
            "status": "error",
            "message": f"Index  {index} is out of bounds. Traffic light '{tls_id}' has {len(state_list)} signals."
        }

    # 2. Look up the conflict map
    conflict_map = tls_conflict_maps.get(tls_id)
    if not conflict_map:
        return {"status": "error", "message": f"Conflict map for traffic light '{tls_id}' not found."}

    verification_event = asyncio.Event()
    verification_result = {}  # Mutable dictionary to hold the result

    async with TRACI_LOCK:
        print(f"[API {tls_id}] Lock acquired.")
        try:
            if state.lower() == 'g':
                state_list[index] = 'G'
                conflicting_indices = conflict_map.get(index, set())
                for conflict_idx in conflicting_indices:
                    if conflict_idx < len(state_list):
                        state_list[conflict_idx] = 'r'
            elif state.lower() == 'r':
                state_list[index] = 'r'
            else:
                return {"status": "error", "message": "Invalid state. Must be 'G' or 'r'."}

            new_state_string = "".join(state_list)
            print(f"[API {tls_id}] Calculated new state: {new_state_string}")

            traci.trafficlight.setRedYellowGreenState(tls_id, new_state_string)
            print(f"[API {tls_id}] setRedYellowGreenState command has been sent.")

            # Create task in the scheduler
            TASK_SCHEDULER[tls_id] = {
                "state": "AWAITING_VERIFICATION",
                "data": {"state": new_state_string, "duration": duration},
                "verification_event": verification_event,
                "verification_result": verification_result,
            }
            print(f"API: Task ready created for {tls_id}.")
        except traci.TraCIException as e:
            raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                                detail=f"TraCI command execution failed: {e}")
        print(f"[API {tls_id}] Lock released.")

    print(f"[API {tls_id}] Starting to wait for event signal (timeout: 40s)...")
    # Wait for the event to be set by the loop
    try:
        await asyncio.wait_for(verification_event.wait(), timeout=40.0)
    except asyncio.TimeoutError:
        print(f"[API] Waiting for task “{tls_id}” validation timeout!")
        # Clean up the task to prevent it from running later
        if tls_id in TASK_SCHEDULER:
            del TASK_SCHEDULER[tls_id]
        raise HTTPException(status_code=504, detail="Verification timed out. The task might not have been processed in time.")

    return verification_result


@app.get("/junction/exists", summary="Check for the existence of junction")
async def check_junction_exists(junctionId:str):
    tls_id = junction_to_tls_map.get(junctionId)
    print(tls_id)
    async with TRACI_LOCK:
        exists = tls_id in traci.trafficlight.getIDList()
    return {"exists": exists}


"""Close all connections"""
@app.on_event("shutdown")
async def shutdown_connections():
    # 1. stop
    if simulation_task:
        print("[FastAPI] Application shutting down, sending stop signal to background task...")
        stop_simulation_event.set()
        await simulation_task
        print("[FastAPI] Background task has stopped successfully.")

    # 2. Use traci.close to close the SUMO connection and process
    # This is a necessary step in traci.start mode, as it terminates the SUMO subprocess we launched
    if connection_status.get("sumo_connected"):
        traci.close()
        connection_status["sumo_connected"] = False
        print("[FastAPI] TraCI connection closed, SUMO process terminated.")

    # 3. Close the Redis connection
    if redis_client:
        await redis_client.close()
        connection_status["redis_connected"] = False
        print("[FastAPI] Redis connection closed")


@app.websocket("/ws/events")
async def websocket_event_handler(websocket: WebSocket):
    """
    Handles WebSocket connections from the backend and continuously listens for event trigger commands.
    """
    await websocket.accept()
    print("[Special/Emergency Event] WebSocket client from backend connected.")
    try:
        while True:
            # 1. Wait for and receive messages from the backend
            data = await websocket.receive_text()
            print(f"[WebSocket RAW DATA] Received raw command: {data}")

            try:
                # 2. Parse the JSON command
                command = json.loads(data)
                event_type = command.get("event_type")
                event_id = command.get("event_id")
                duration = command.get("duration")

                if not all([event_type, event_id]):
                    raise ValueError("[Special/Emergency Event] Command missing required fields: event_type, event_id")

                print(f"[Special/Emergency Event] Received WebSocket command: event_id={event_id}, type={event_type}")

                # 3. Call different event manager methods based on event_type
                result = {}
                async with TRACI_LOCK:
                    if event_type == "vehicle_breakdown":
                        result = await event_manager.trigger_vehicle_breakdown(event_id, duration)
                    elif event_type == "vehicle_collision":
                        result = await event_manager.trigger_vehicle_collision(event_id, duration)
                    elif event_type == "lane_closure":
                        lane_ids = command.get("lane_ids", [])
                        if not lane_ids:
                            raise ValueError("[Special/Emergency Event] lane_closure 事件缺少 lane_ids 参数")
                        result = await event_manager.trigger_lane_closure(event_id, duration, lane_ids)
                    elif event_type == "emergency_event":
                        result = await event_manager.trigger_emergency_vehicle(command)
                    else:
                        raise ValueError(f"[Special/Emergency Event] Unknown event type: {event_type}")

                # 4. Send the result back to the backend via WebSocket
                if result.get("success"):
                    response_payload = {"status": "success", "event_id": event_id, **result.get("details", {})}
                    await websocket.send_text(json.dumps(response_payload))
                else:
                    response_payload = {"status": "fail", "event_id": event_id, **result.get("details", {})}
                    await websocket.send_text(json.dumps(response_payload))


            except json.JSONDecodeError:
                await websocket.send_text(json.dumps({"status": "fail", "message": "Invalid JSON format."}))
            except ValueError as e:
                await websocket.send_text(json.dumps({"status": "fail", "message": str(e)}))
            except traci.TraCIException as e:
                # Catch TraCI-related execution errors and return as a failure message
                print(f"[Special/Emergency Event] TraCI execution error: {e}")
                # Try to extract event_id from the command for the response
                try:
                    failed_event_id = json.loads(data).get("event_id", "unknown")
                except:
                    failed_event_id = "unknown"
                await websocket.send_text(json.dumps({"status": "fail","event_id": failed_event_id,"message": f"TraCI command failed: {e}"
                }))
            except Exception as e:
                # Catch any other unexpected errors, print stack trace and continue the loop
                print(f"[Special/Emergency Event] Unknown error while processing WebSocket message: {e}")
                traceback.print_exc()

    except WebSocketDisconnect:
        print("[[Special/Emergency Event] WebSocket client from backend disconnected.")
    except Exception as e:
        # This outer except will only be triggered by more serious connection-level errors
        print(f"[Special/Emergency Event] Unknown error at WebSocket connection level: {e}")
        traceback.print_exc()


def generate_and_send_junction_names():
    # 1. Get all Junction IDs
    junction_ids = traci.junction.getIDList()

    for jid in junction_ids:
        # Internal SUMO junctions often start with ':', which we usually don't care about, so we can skip them
        if jid.startswith(':'):
            continue

        # 2. Get the incoming and outgoing Edge IDs for this Junction
        incoming_edges = traci.junction.getIncomingEdges(jid)
        outgoing_edges = traci.junction.getOutgoingEdges(jid)

        # Use a set to automatically handle duplicate street names
        street_names = set()

        # 3. Get the street names of all related Edges
        for edge_id in (incoming_edges + outgoing_edges):
            # Check if the edge is not an internal junction edge (these start with ':')
            if not edge_id.startswith(':'):
                street_name = traci.edge.getStreetName(edge_id)
                if street_name:  # Ensure the name is not empty
                    street_names.add(street_name)

        # 4. Consolidate the names
        if street_names:
            # Sort to ensure consistent name order, then join with " - "
            junction_name = "-".join(sorted(list(street_names))[:2])
            junction_names_map[jid] = junction_name
        else:
            # If a Junction has no connecting edges with names, give it a default name
            junction_names_map[jid] = f"Unnamed Junction ({jid})"

    if not junction_names_map:
        print("[Start Up] Failed to generate any junction names, task ending.")
        return

    print(f"[Start Up] Successfully generated names for {len(junction_names_map)} junctions...")
    return junction_names_map


# Analyzes the traffic light logic in the simulation to build a conflict map for each light.
# This function should be called once after the simulation starts.
def build_all_conflict_maps():
    print("[Start Up] Building conflict relationship maps for traffic lights...")
    tls_ids = traci.trafficlight.getIDList()
    for tlsID in tls_ids:
        try:
            logics = traci.trafficlight.getCompleteRedYellowGreenDefinition(tlsID)
            if not logics:
                print(f"[Start Up] Warning: No logic definition found for traffic light '{tlsID}'.")
                continue
            program = logics[0]
            if not program.phases:
                print(f"[Start Up] Warning: No phases found in the program for traffic light '{tlsID}'.")
                continue
            num_links = len(program.phases[0].state)
            is_compatible = [[False] * num_links for _ in range(num_links)]
            for phase in program.phases:
                green_indices = [i for i, char in enumerate(phase.state) if char.lower() == 'g']
                for i in green_indices:
                    for j in green_indices:
                        is_compatible[i][j] = True
            conflict_map_for_tls = {}
            for i in range(num_links):
                conflicts = set()
                for j in range(num_links):
                    if not is_compatible[i][j]:
                        conflicts.add(j)
                conflict_map_for_tls[i] = conflicts
            tls_conflict_maps[tlsID] = conflict_map_for_tls
        except traci.TraCIException as e:
            print(f"[Start Up] Error building conflict map for {tlsID}: {e}")
    # print("-----------------------------------------------------\n")
    # print_conflict_maps_with_details(tls_conflict_maps)
    # print("-----------------------------------------------------\n")
    print(f"[Start Up] Successfully built conflict maps for {len(tls_conflict_maps)} traffic lights.")


def print_conflict_maps_with_details(maps_dict):
    print("\n" + "=" * 80)
    print("Traffic Light Conflict Map Detailed Report")
    print("=" * 80)

    for tls_id, conflict_map in maps_dict.items():
        print(f"\n--- Traffic Light ID: {tls_id} ---")

        try:
            # Get connection definitions to provide readable descriptions for indexes
            links = traci.trafficlight.getControlledLinks(tls_id)
            link_descriptions = {
                i: f"From {traci.lane.getEdgeID(l[0][0])} to {traci.lane.getEdgeID(l[0][1])}"
                for i, l in enumerate(links)
            }
        except traci.TraCIException:
            link_descriptions = {}  # If query fails, leave empty

        if not conflict_map:
            print("(No conflict map information for this traffic light)")
            continue

        # Sort keys by index to ensure consistent output order
        for index in sorted(conflict_map.keys()):
            conflicts = sorted(list(conflict_map[index]))
            description = f"({link_descriptions.get(index, 'Unknown connection')})"

            # Use f-string alignment to make output more readable
            # {index:<2} means left-aligned, width 2 characters
            # {description:<55} means left-aligned, width 55 characters
            print(f"  Signal flow {index:<2} {description:<55} -> Conflicts: {conflicts}")
    print("\n" + "=" * 80)


def build_junction_tls_maps():
    """
    Builds a map to translate from junction IDs to their corresponding traffic light IDs.
    This is necessary because the tlsID might have a prefix (e.g., "GS_") while the
    junctionID does not. This function should be called once after the simulation starts.
    """
    print("Building Junction ID -> TLS ID map")
    prefix = "GS_"

    try:
        all_tls_ids = traci.trafficlight.getIDList()
        # Get all real Junction IDs at once for subsequent validation
        all_junction_ids = set(traci.junction.getIDList())

        for tls_id in all_tls_ids:
            potential_junction_id = ""
            if tls_id.startswith(prefix):
                potential_junction_id = tls_id[len(prefix):]
            else:
                # If there is no prefix, assume the IDs are the same
                potential_junction_id = tls_id

            # Validate whether the inferred junction_id actually exists
            if potential_junction_id in all_junction_ids:
                junction_to_tls_map[potential_junction_id] = tls_id
            else:
                print(f"[Start Up] Warning: The Junction ID '{tls_id}'inferred for '{potential_junction_id}' based on rules does not exist in the network and has been skipped.")

    except traci.TraCIException as e:
        print(f"[Start Up] TraCI error occurred while building Junction/TLS map: {e}")

    if not junction_to_tls_map:
        print("[Start Up] Warning: Failed to build any Junction ID -> TLS ID mappings. The API may not be able to find traffic lights by Junction ID")
    else:
        print(f"[Start Up] Successfully built Junction->TLS map for {len(junction_to_tls_map)} junctions.")
        # For debugging, print the entire map
        # For debugging, print the entire map
        #print("Completed map: ", junction_to_tls_map)

# Checks if all junctions that have a corresponding traffic light in junction_to_tls_map
def verify_official_junction_names():
    print("[Start Up] Verifying that all controlled junctions have official names...")
    unnamed_junctions_found = []

    # Iterate through all junction IDs that have a mapped traffic light
    for junction_id in junction_to_tls_map.keys():
        # Get the name of the junction from the name map
        junction_name = junction_names_map.get(junction_id)

        # Check if the name exists and if it is in the default "Unnamed" format
        if not junction_name or junction_name.startswith("Unnamed Junction"):
            unnamed_junctions_found.append(junction_id)

    if unnamed_junctions_found:
        print("\n[Start Up] WARNING: The following controlled junctions were found to be using default names ---")
        for jid in unnamed_junctions_found:
            print(f"  - Junction ID: '{jid}' has the name '{junction_names_map.get(jid)}'")
        print("[Start Up] This may mean that the corresponding edges in the network file (.net.xml) are missing the 'streetName' attribute.")
        print("----------------------------------------------------------\n")
    else:
        # If no unnamed junctions are found, print a success message and a detailed list
        print("[Start Up] VERIFICATION PASSED: All controlled junctions have been successfully named ---")
        # Iterate through the mapped junctions and print their IDs and official names
        # for junction_id, tls_id in junction_to_tls_map.items():
        #     junction_name = junction_names_map.get(junction_id)
        #     print(f"  - Junction ID: '{junction_id}' -> Name: '{junction_name}' (Controls TLS: '{tls_id}')")
        # print("-----------------------------------------------------\n")

if __name__ == "__main__":
    # Use uvicorn to run the FastAPI application
    # reload=True means the server will automatically restart after code changes are saved
    uvicorn.run("withRedis:app", host="0.0.0.0", port=8000, reload=False)