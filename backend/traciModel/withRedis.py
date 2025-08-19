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
REDIS_EXPIRATION_SECONDS = 600

TASK_SCHEDULER = {}
stop_simulation_event = asyncio.Event()
VERIFICATION_STATE = {}
verification_events = {}

# Redis Key Constants
KEY_SIM_TIME = "sumo:simulation_time"
KEY_ALL_EDGES = "sumo:edge"
KEY_ALL_TLS = "sumo:tls"
KEY_EMERGENCY_VEHICLES = "sumo:emergency_vehicles"
KEY_ALL_JUNCTIONS = "sumo:junction"

# Global variables
junction_names_map = {}
tls_conflict_maps = {}
junction_to_tls_map = {}
junction_processor: Optional[JunctionDataProcessor] = None
event_manager = EventManager()

simulation_task_control = None
simulation_task_data = None


async def _internal_update_single_tls_in_redis(tls_id: str):
    """
    (Internal Helper) Fetches TLS data from SUMO and updates Redis.
    This function has been modified to REMOVE the internal lock, fixing the deadlock.
    It now ASSUMES a TRACI_LOCK is already held by the caller.
    """
    print(f"[TriggerUpdate] Actively updating data for TLS '{tls_id}' in Redis...")
    try:
        # The lock is now managed by the caller (control_loop)
        current_time = traci.simulation.getTime()

        correct_junction_id = tls_id
        for junc_id, t_id in junction_to_tls_map.items():
            if t_id == tls_id:
                correct_junction_id = junc_id
                break

        junction_name = junction_names_map.get(correct_junction_id, f"Unknown Junction ({correct_junction_id})")
        next_switch_abs_time = traci.trafficlight.getNextSwitch(tls_id)
        spent_time = traci.trafficlight.getSpentDuration(tls_id)

        tls_data = {
            "tlsID": tls_id, "junction_id": correct_junction_id, "junction_name": junction_name,
            "timestamp": current_time,
            "phase": traci.trafficlight.getPhase(tls_id) or 0,
            "state": traci.trafficlight.getRedYellowGreenState(tls_id) or "",
            "duration": traci.trafficlight.getPhaseDuration(tls_id) or 0.0,
            "connection": traci.trafficlight.getControlledLinks(tls_id) or [],
            "spendTime": spent_time,
            "nextSwitchTime": next_switch_abs_time - current_time
        }

        # Redis operations can happen without the TraCI lock
        await redis_client.hset(KEY_ALL_TLS, tls_id, json.dumps(tls_data))
        await redis_client.expire(KEY_ALL_TLS, REDIS_EXPIRATION_SECONDS)
        print(f"[TriggerUpdate] Successfully updated TLS '{tls_id}' in Redis.")

    except traci.TraCIException as e:
        print(f"[TriggerUpdate] Failed to update TLS '{tls_id}' due to TraCI error: {e}")
    except redis.RedisError as e:
        print(f"[TriggerUpdate] Failed to update TLS '{tls_id}' due to Redis error: {e}")


# LOOP 1: High-frequency loop for simulation stepping and control tasks
async def control_loop():
    print("Background control_loop has started.")
    while not stop_simulation_event.is_set():
        events_to_set = []
        async with TRACI_LOCK:
            try:
                # 1. Advance the simulation by one step
                traci.simulationStep()
                current_time = traci.simulation.getTime()

                # 2. Process immediate control tasks (TASK_SCHEDULER)
                if TASK_SCHEDULER:
                    print(f"[ControlLoop] Found tasks: {list(TASK_SCHEDULER.keys())}")
                    for tls_id, task in list(TASK_SCHEDULER.items()):
                        task_state = task.get("state")
                        print(f"[ControlLoop] > Processing task '{tls_id}'，state: '{task_state}'")

                        verification_event = task.get("verification_event")
                        verification_result = task.get("verification_result", {})

                        if task_state == "AWAITING_VERIFICATION":
                            expected_state = task["data"]["state"]
                            print(f"[ControlLoop] > > Task '{tls_id}' awaiting verification...")
                            verified_state = traci.trafficlight.getRedYellowGreenState(tls_id)
                            print(f"[ControlLoop] > > > Expected state: '{expected_state}', actual state retrieved: '{verified_state}'")

                            if verified_state == task["data"]["state"]:
                                print(f"[ControlLoop] Task '{tls_id}' ready be verified.")
                                verification_result["status"] = "VERIFIED_AND_RUNNING"
                                verification_result["detail"] = f"State for {tls_id} successfully set and verified."
                                traci.trafficlight.setPhaseDuration(tls_id, task["data"]["duration"])
                                await _internal_update_single_tls_in_redis(tls_id)
                                task["state"] = "RUNNING_MANUAL_PHASE"
                                task["execution_time"] = current_time + task["data"]["duration"] - 1
                            else:
                                error_message = f"Expected state '{task['data']['state']}' but got '{verified_state}'"
                                verification_result["status"] = "FAILED_VERIFICATION"
                                verification_result["detail"] = error_message
                                print(f"Task [{tls_id}]: Verification failed! {error_message}")
                                del TASK_SCHEDULER[tls_id]

                            if verification_event:
                                print(f"[ControlLoop] > > > Adding '{tls_id}' to the to-be-set list")
                                events_to_set.append(verification_event)

                        elif task_state == "RUNNING_MANUAL_PHASE":
                            if current_time >= task.get("execution_time", float('inf')):
                                traci.trafficlight.setProgram(tls_id, "0")
                                print(f"Task[{tls_id}]: Time's up. Default program has been restored silently. Task lifecycle ended.")
                                del TASK_SCHEDULER[tls_id]

                # 3. Check special events
                await event_manager.check_for_expired_events()

            except traci.TraCIException as step_error:
                print(f"[ControlLoop] Connection to SUMO lost: {step_error}")
                connection_status["sumo_connected"] = False
                connection_status["message"] = "SUMO connection lost during simulation."

        if events_to_set:
            for event in events_to_set:
                event.set()

        await asyncio.sleep(0.1)


# LOOP 2: Low-frequency loop for heavy data gathering and Redis updates
async def data_gathering_loop(processor):
    print("Background data_gathering_loop has started.")
    while not stop_simulation_event.is_set():
        await asyncio.sleep(0.1)

        edges_to_cache = {}
        tls_to_cache = {}
        junctions_to_cache = {}
        emergency_vehicles_to_cache = {}
        redis_ev_keys_to_delete = []
        sim_time_to_cache = 0

        async with TRACI_LOCK:
            try:
                current_time = traci.simulation.getTime()
                sim_time_to_cache = current_time
                print(f"[DataLoop] Starting periodic data gathering at sim time {current_time}...")

                for edgeID in traci.edge.getIDList():
                    waiting_vehicle_count = traci.edge.getLastStepHaltingNumber(edgeID)
                    waiting_time = traci.edge.getWaitingTime(edgeID)
                    avg_waiting_time = (waiting_time / waiting_vehicle_count) if waiting_vehicle_count > 0 else 0.0
                    edge_data = {"edgeID": edgeID, "edgeName": traci.edge.getStreetName(edgeID) or "",
                                 "timestamp": current_time, "laneNumber": traci.edge.getLaneNumber(edgeID) or 0,
                                 "speed": traci.edge.getLastStepMeanSpeed(edgeID) or 0.0,
                                 'vehicleCount': traci.edge.getLastStepVehicleNumber(edgeID) or 0,
                                 'vehicleIDs': list(traci.edge.getLastStepVehicleIDs(edgeID) or []),
                                 'waitingTime': avg_waiting_time or 0.0,
                                 'occupancy': traci.edge.getLastStepOccupancy(edgeID) or 0.0,
                                 "waitingVehicleCount": waiting_vehicle_count or 0}
                    edges_to_cache[edgeID] = json.dumps(edge_data)

                for tlsID in traci.trafficlight.getIDList():
                    correct_junction_id = tlsID
                    for junc_id, t_id in junction_to_tls_map.items():
                        if t_id == tlsID: correct_junction_id = junc_id; break
                    junction_name = junction_names_map.get(correct_junction_id,
                                                           f"Unknown Junction ({correct_junction_id})")
                    nextSwitch = traci.trafficlight.getNextSwitch(tlsID)
                    nextSwitchTime = nextSwitch - current_time
                    tls_data = {"tlsID": tlsID, "junction_id": correct_junction_id, "junction_name": junction_name,
                                "timestamp": current_time, "phase": traci.trafficlight.getPhase(tlsID) or 0,
                                "state": traci.trafficlight.getRedYellowGreenState(tlsID) or "",
                                "duration": traci.trafficlight.getPhaseDuration(tlsID) or 0.0,
                                "connection": traci.trafficlight.getControlledLinks(tlsID) or [],
                                "spendTime": traci.trafficlight.getSpentDuration(tlsID) or 0.0,
                                "nextSwitchTime": nextSwitchTime or 0.0}
                    tls_to_cache[tlsID] = json.dumps(tls_data)

                junctions_to_cache = processor.calculate_all_junctions_metrics(current_time)

                emergency_vehicles_to_cache, redis_ev_keys_to_delete = \
                    await event_manager.track_active_emergency_vehicles(current_time, tls_to_cache, junction_to_tls_map)

            except traci.TraCIException as data_error:
                print(f"[DataLoop] TraCI error during data gathering: {data_error}")
                continue

        if sim_time_to_cache > 0:
            try:
                async with redis_client.pipeline(transaction=False) as pipe:
                    pipe.set(KEY_SIM_TIME, sim_time_to_cache, ex=REDIS_EXPIRATION_SECONDS)
                    if edges_to_cache:
                        pipe.hset(KEY_ALL_EDGES, mapping=edges_to_cache);
                        pipe.expire(KEY_ALL_EDGES,REDIS_EXPIRATION_SECONDS)
                    if tls_to_cache:
                        pipe.hset(KEY_ALL_TLS, mapping=tls_to_cache);
                        pipe.expire(KEY_ALL_TLS,REDIS_EXPIRATION_SECONDS)
                    if junctions_to_cache:
                        pipe.hset(KEY_ALL_JUNCTIONS, mapping=junctions_to_cache);
                        pipe.expire(KEY_ALL_JUNCTIONS, REDIS_EXPIRATION_SECONDS)
                    if emergency_vehicles_to_cache:
                        pipe.hset(KEY_EMERGENCY_VEHICLES,mapping=emergency_vehicles_to_cache);
                        pipe.expire(KEY_EMERGENCY_VEHICLES, REDIS_EXPIRATION_SECONDS)
                    if redis_ev_keys_to_delete:
                        pipe.hdel(KEY_EMERGENCY_VEHICLES, *redis_ev_keys_to_delete)
                    await pipe.execute()
                print(f"[DataLoop] Redis cache updated successfully.")
            except redis.RedisError as e:
                print(f"[DataLoop] Redis Error: {e}")


# STARTUP AND SHUTDOWN EVENTS

@app.on_event("startup")
async def start_simulation_and_connect():
    global redis_client, junction_processor
    global simulation_task_control, simulation_task_data

    try:
        redis_url = f"redis://{REDIS_HOST}:{REDIS_PORT}/{REDIS_DB}"
        redis_client = redis.from_url(redis_url, decode_responses=True)
        await redis_client.ping()
        connection_status["redis_connected"] = True
        print("[Start Up] Successfully connected to Redis.")
    except redis.RedisError as e:
        connection_status["message"] = f"Failed to connect to Redis: {e}"
        print(connection_status["message"])
        return

    try:
        traci.start(sumoCmd, port=TRACI_PORT)
        connection_status["sumo_connected"] = True
        connection_status["message"] = "Service is ready"
        print(f"[Start Up] Successfully launched and connected to SUMO (Port: {TRACI_PORT}).")

        print("[Start Up] Starting one-time initialization tasks...")
        generate_and_send_junction_names()
        build_all_conflict_maps()
        build_junction_tls_maps()
        verify_official_junction_names()
        junction_processor = JunctionDataProcessor(SQL_FILE_PATH, junction_to_tls_map)
        junction_processor.load_and_process_data()
        print("[Start Up] One-time initialization tasks completed.")
    except Exception as e:
        connection_status["message"] = f"Failed to start SUMO: {e}"
        print(connection_status["message"])
        if redis_client: await redis_client.close()
        return

    stop_simulation_event.clear()
    simulation_task_control = asyncio.create_task(control_loop())
    simulation_task_data = asyncio.create_task(data_gathering_loop(junction_processor))
    print("[FastAPI] Background control and data gathering tasks have been created.")


@app.on_event("shutdown")
async def shutdown_connections():
    if simulation_task_control or simulation_task_data:
        print("[FastAPI] Application shutting down, sending stop signal to background tasks...")
        stop_simulation_event.set()
        if simulation_task_control:
            await simulation_task_control
        if simulation_task_data:
            await simulation_task_data
        print("[FastAPI] Background tasks have stopped successfully.")

    if connection_status.get("sumo_connected"):
        traci.close()
        connection_status["sumo_connected"] = False
        print("[FastAPI] TraCI connection closed, SUMO process terminated.")

    if redis_client:
        await redis_client.close()
        connection_status["redis_connected"] = False
        print("[FastAPI] Redis connection closed")



# API ENDPOINTS (Unchanged)

@app.get("/status", summary="Check if the service is ready")
async def get_status():
    return {"connection": connection_status}


#@app.get("/trafficlight/get_tls_status", summary="Restore the default state of a specific traffic light")
async def get_tls_status(tlsID):
    light_data_json = await redis_client.hget(KEY_ALL_TLS, tlsID)
    if light_data_json:
        return {"tlsID": tlsID, "lightData": json.loads(light_data_json)}
    else:
        return {"tlsID": tlsID, "lightData": None}

@app.get("/junction/exists", summary="Check for the existence of junction")
async def check_junction_exists(junctionId:str):
    tls_id = junction_to_tls_map.get(junctionId)
    print(tls_id)
    async with TRACI_LOCK:
        exists = tls_id in traci.trafficlight.getIDList()
    return {"exists": exists}

# Mode 1: Modify duration only
@app.post("/trafficlight/set_duration")
async def modify_tls_duration(payload: DurationPayload):
    tls_id = junction_to_tls_map.get(payload.junctionId)
    async with TRACI_LOCK:
        nextSwitch = traci.trafficlight.getNextSwitch(tls_id)
        current_time = traci.simulation.getTime()
    nextSwitchTime = nextSwitch - current_time
    add_duration = payload.duration + nextSwitchTime
    async with TRACI_LOCK:
        print(f"[API {tls_id}] Received request: duration={payload.duration}")
        traci.trafficlight.setPhaseDuration(tls_id, add_duration)
        print(f"[API {tls_id}] new duration is {add_duration}")
        return {"status": "success", "junctionId": payload.junctionId, "duration_set": payload.duration}

# Mode 2: Modify both state and duration
@app.post("/trafficlight/set_state_duration", summary="Modify the state of a specific traffic light")
async def modify_tls_state_duration(payload: StateDurationPayload):
    junctionId = payload.junctionId
    state = payload.state
    duration = payload.duration
    index = payload.lightIndex
    tls_id = junction_to_tls_map.get(junctionId)

    if not tls_id:
        raise HTTPException(status_code=404,detail=f"Junction ID '{junctionId}' not found or not associated with a traffic light.")

    print(f"[API {tls_id}] Received request: state='{state}', duration={duration}, index={index}")

    current_state_res = await get_tls_status(tls_id)
    if not current_state_res or not current_state_res.get("lightData"):
        raise HTTPException(status_code=404,detail=f"Could not retrieve current state for TLS '{tls_id}'. Is simulation running?")
    current_state_str = current_state_res["lightData"].get("state")

    async with TRACI_LOCK:
        nextSwitch = traci.trafficlight.getNextSwitch(tls_id)
        current_time = traci.simulation.getTime()
    nextSwitchTime = nextSwitch - current_time

    state_list = list(current_state_str)
    if index >= len(state_list):
        raise HTTPException(status_code=400, detail=f"Index {index} is out of bounds for TLS '{tls_id}'.")

    final_duration = duration
    if state_list[index].lower() == state.lower():
        final_duration += nextSwitchTime

    conflict_map = tls_conflict_maps.get(tls_id)
    if not conflict_map:
        raise HTTPException(status_code=500, detail=f"Conflict map for TLS '{tls_id}' not found.")

    verification_event = asyncio.Event()
    verification_result = {}

    async with TRACI_LOCK:
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
                raise HTTPException(status_code=400, detail="Invalid state. Must be 'G' or 'r'.")

            new_state_string = "".join(state_list)
            traci.trafficlight.setRedYellowGreenState(tls_id, new_state_string)

            TASK_SCHEDULER[tls_id] = {
                "state": "AWAITING_VERIFICATION",
                "data": {"state": new_state_string, "duration": final_duration},
                "verification_event": verification_event,
                "verification_result": verification_result,
            }
        except traci.TraCIException as e:
            raise HTTPException(status_code=500, detail=f"TraCI command execution failed: {e}")

    try:
        await asyncio.wait_for(verification_event.wait(), timeout=40.0)
    except asyncio.TimeoutError:
        if tls_id in TASK_SCHEDULER: del TASK_SCHEDULER[tls_id]
        raise HTTPException(status_code=504, detail="Verification timed out.")

    return verification_result


@app.websocket("/ws/events")
async def websocket_event_handler(websocket: WebSocket):
    await websocket.accept()
    print("[Special/Emergency Event] WebSocket client from backend connected.")
    try:
        while True:
            data = await websocket.receive_text()
            try:
                command = json.loads(data)
                event_type = command.get("event_type")
                event_id = command.get("event_id")
                duration = command.get("duration")

                if not all([event_type, event_id]):
                    raise ValueError("Command missing required fields")

                result = {}
                async with TRACI_LOCK:
                    if event_type == "vehicle_breakdown":
                        result = await event_manager.trigger_vehicle_breakdown(event_id, duration)
                    elif event_type == "vehicle_collision":
                        result = await event_manager.trigger_vehicle_collision(event_id, duration)
                    elif event_type == "lane_closure":
                        result = await event_manager.trigger_lane_closure(event_id, duration,
                                                                          command.get("lane_ids", []))
                    elif event_type == "emergency_event":
                        result = await event_manager.trigger_emergency_vehicle(command)
                    else:
                        raise ValueError(f"Unknown event type: {event_type}")

                response_status = "success" if result.get("success") else "fail"
                response_payload = {"status": response_status, "event_id": event_id, **result.get("details", {})}
                await websocket.send_text(json.dumps(response_payload))

            except (json.JSONDecodeError, ValueError) as e:
                await websocket.send_text(json.dumps({"status": "fail", "message": str(e)}))
            except traci.TraCIException as e:
                failed_event_id = json.loads(data).get("event_id", "unknown")
                await websocket.send_text(json.dumps(
                    {"status": "fail", "event_id": failed_event_id, "message": f"TraCI command failed: {e}"}))
            except Exception as e:
                traceback.print_exc()

    except WebSocketDisconnect:
        print("[Special/Emergency Event] WebSocket client from backend disconnected.")
    except Exception as e:
        print(f"[Special/Emergency Event] Unknown error at WebSocket connection level: {e}")
        traceback.print_exc()


# ONE-TIME INITIALIZATION FUNCTIONS
def generate_and_send_junction_names():
    junction_ids = traci.junction.getIDList()
    for jid in junction_ids:
        if jid.startswith(':'): continue
        incoming_edges = traci.junction.getIncomingEdges(jid)
        outgoing_edges = traci.junction.getOutgoingEdges(jid)
        street_names = set()
        for edge_id in (incoming_edges + outgoing_edges):
            if not edge_id.startswith(':'):
                street_name = traci.edge.getStreetName(edge_id)
                if street_name: street_names.add(street_name)
        if street_names:
            junction_name = "-".join(sorted(list(street_names))[:2])
            junction_names_map[jid] = junction_name
        else:
            junction_names_map[jid] = f"Unnamed Junction ({jid})"
    print(f"[Start Up] Successfully generated names for {len(junction_names_map)} junctions.")
    return junction_names_map


def build_all_conflict_maps():
    print("[Start Up] Building conflict relationship maps for traffic lights...")
    tls_ids = traci.trafficlight.getIDList()
    for tlsID in tls_ids:
        try:
            logics = traci.trafficlight.getCompleteRedYellowGreenDefinition(tlsID)
            if not logics: continue
            program = logics[0]
            if not program.phases: continue
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
    print(f"[Start Up] Successfully built conflict maps for {len(tls_conflict_maps)} traffic lights.")


def build_junction_tls_maps():
    print("Building Junction ID -> TLS ID map")
    prefix = "GS_"
    try:
        all_tls_ids = traci.trafficlight.getIDList()
        all_junction_ids = set(traci.junction.getIDList())
        for tls_id in all_tls_ids:
            potential_junction_id = tls_id[len(prefix):] if tls_id.startswith(prefix) else tls_id
            if potential_junction_id in all_junction_ids:
                junction_to_tls_map[potential_junction_id] = tls_id
            else:
                print(
                    f"[Start Up] Warning: Inferred Junction ID '{potential_junction_id}' for TLS '{tls_id}' does not exist.")
    except traci.TraCIException as e:
        print(f"[Start Up] TraCI error occurred while building Junction/TLS map: {e}")
    print(f"[Start Up] Successfully built Junction->TLS map for {len(junction_to_tls_map)} junctions.")


def verify_official_junction_names():
    print("[Start Up] Verifying that all controlled junctions have official names...")
    unnamed_junctions_found = []
    for junction_id in junction_to_tls_map.keys():
        junction_name = junction_names_map.get(junction_id)
        if not junction_name or junction_name.startswith("Unnamed Junction"):
            unnamed_junctions_found.append(junction_id)
    if unnamed_junctions_found:
        print("\n[Start Up] WARNING: The following controlled junctions use default names:")
        for jid in unnamed_junctions_found:
            print(f"  - Junction ID: '{jid}' has name '{junction_names_map.get(jid)}'")
    else:
        print("[Start Up] VERIFICATION PASSED: All controlled junctions have been successfully named.")


if __name__ == "__main__":
    uvicorn.run("withRedis:app", host="0.0.0.0", port=8000, reload=False)