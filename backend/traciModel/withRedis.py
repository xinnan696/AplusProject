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

# SUMO
sumoBinary = "E:/sumo/sumo-1.23.1/bin/sumo"
#sumoBinary = "sumo"
TRACI_PORT = 8813
sumoCmd = [sumoBinary, "-c", "E:/sumo/sumo-1.23.1/tools/2025-06-22-11-26-25/osm.sumocfg",  "--start"]
#sumoCmd = ["sumo", "-c", "sumo_scenario/osm.sumocfg"]

# Redis Configuration
# Read JSON configuration file
with open('config.json', 'r') as f:
    config = json.load(f)
# Get Redis configuration from the parsed dictionary
redis_config = config.get('redis', {})
REDIS_HOST = redis_config.get('host')
REDIS_PORT = redis_config.get('port')
REDIS_DB = redis_config.get('db')
SUMO_HOST = "host.docker.internal"

TARGET_EDGE_ID_TO_MONITOR = "1314507321#1"

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

class EventPayload(BaseModel):
    event_id: str
    duration: float

class LaneClosurePayload(BaseModel):
    event_id: str
    duration: float
    lane_ids: List[str]

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

# Redis Key Constants
# Use constants to manage Redis key names for easier maintenance
KEY_SIM_TIME = "sumo:simulation_time"
KEY_ALL_EDGES = "sumo:edge"
KEY_ALL_TLS = "sumo:tls"

# Global variables to store static data generated at startup
junction_names_map = {}
tls_conflict_maps = {}
junction_to_tls_map = {}

# <<< 创建 EventManager 的全局实例 >>>
event_manager = EventManager()


# Advance Simulation
async def simulation_loop():
    print("Background scheduler loop has started.")

    while not stop_simulation_event.is_set():
        print("[SimLoop] Loop started, waiting for lock...")

        # [Deadlock Fix]: Create a temporary list to hold events
        # that need to be triggered after the lock is released.
        events_to_set = []

        # Initialize containers for data to be cached in Redis
        edges_to_cache = {}
        tls_to_cache = {}

        # Initialize containers for data to be cached in Redis
        async with TRACI_LOCK:
            print("[SimLoop] Lock acquired.")
            try:
                # 1. Advance the simulation by one step
                print("[SimLoop] Calling traci.simulationStep()...")
                traci.simulationStep()
                current_time = traci.simulation.getTime()
                print(f"current time is {current_time}")
                sim_time_to_cache = current_time

                # 2. heck and execute tasks in the scheduler
                if TASK_SCHEDULER:
                    print(f"[SimLoop] Found tasks: {list(TASK_SCHEDULER.keys())}")
                    for tls_id, task in list(TASK_SCHEDULER.items()):
                        task_state = task.get("state")
                        print(f"[SimLoop] > Processing task '{tls_id}'，state: '{task_state}'")

                        verification_event = task.get("verification_event")
                        verification_result = task.get("verification_result", {})

                        # Task state machine processing
                        if task_state == "AWAITING_VERIFICATION":
                            expected_state = task["data"]["state"]
                            print(f"[SimLoop] > > Task '{tls_id}' awaiting verification...")
                            verified_state = traci.trafficlight.getRedYellowGreenState(tls_id)
                            print(f"[SimLoop] > > > Expected state: '{expected_state}', actual state retrieved: '{verified_state}'")

                            if verified_state == task["data"]["state"]:
                                # Report success and set event
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
                                print(f"[SimLoop] > > > Adding  '{tls_id}' to the to-be-set list")
                                events_to_set.append(verification_event)

                        elif task_state == "RUNNING_MANUAL_PHASE":
                            if current_time >= task.get("execution_time", float('inf')):
                                traci.trafficlight.setProgram(tls_id, "0")
                                print(f"Task[{tls_id}]: Time's up. Default program has been restored silently. Task lifecycle ended.")
                                del TASK_SCHEDULER[tls_id]

                # <<< 新增：每一步都检查特殊事件是否到期 >>>
                # 3. Check special events
                print("[SimLoop] Starting checking  special events...")
                await event_manager.check_for_expired_events()

                # 4. Update data to Redis
                print("[SimLoop] Starting data update...")

                for edgeID in traci.edge.getIDList():
                    edge_data = {
                        "edgeID": edgeID,
                        "edgeName": traci.edge.getStreetName(edgeID) or "",
                        "timestamp": current_time,
                        "laneNumber": traci.edge.getLaneNumber(edgeID) or 0,
                        "speed": traci.edge.getLastStepMeanSpeed(edgeID) or 0.0,
                        'vehicleCount': traci.edge.getLastStepVehicleNumber(edgeID) or 0,
                        'vehicleIDs': list(traci.edge.getLastStepVehicleIDs(edgeID) or []),
                        'waitTime': traci.edge.getWaitingTime(edgeID) or 0.0,
                        'waitingVehicleIDs': list(traci.edge.getPendingVehicles(edgeID) or []),
                        "waitingVehicleCount": traci.edge.getLastStepHaltingNumber(edgeID) or 0
                    }
                    edges_to_cache[edgeID] = json.dumps(edge_data)

                for tlsID in traci.trafficlight.getIDList():
                    #correct_junction_id = tlsID  # Default to tlsID itself
                    for junc_id, t_id in junction_to_tls_map.items():
                        if t_id == tlsID:
                            correct_junction_id = junc_id
                            break
                    junction_name = junction_names_map.get(correct_junction_id, f"Unknown Junction ({correct_junction_id})")
                    tls_data = {
                        "tlsID": tlsID,
                        "junction_id": correct_junction_id,
                        "junction_name": junction_name,
                        "timestamp": current_time,
                        "phase": traci.trafficlight.getPhase(tlsID) or 0,
                        "state": traci.trafficlight.getRedYellowGreenState(tlsID) or "",
                        "duration": traci.trafficlight.getPhaseDuration(tlsID) or 0.0,
                        "connection": traci.trafficlight.getControlledLinks(tlsID) or [],
                        "spendTime": traci.trafficlight.getSpentDuration(tlsID) or 0.0,
                        "nextSwitchTime": traci.trafficlight.getNextSwitch(tlsID) or -1.0
                    }
                    tls_to_cache[tlsID] = json.dumps(tls_data)


            except traci.TraCIException as step_error:
                # If the connection is lost during simulationStep, handle it gracefully
                print(f"[SimLoop] Connection to SUMO lost during simulation step: {step_error}")
                print("[SimLoop] Will attempt to reconnect in the next loop...")
                connection_status["sumo_connected"] = False
                connection_status["message"] = "SUMO connection lost during simulation."
                # No need to break; the loop will continue naturally and enter reconnection logic on the next iteration

        # 5. Send notifications immediately
        if events_to_set:
            print(f"Sending  {len(events_to_set)} notifications immediately...")
            for event in events_to_set:
                event.set()
        try:
            print("Background data packaging complete, starting Redis cache update...")
            async with redis_client.pipeline(transaction=False) as pipe:
                # Cache simulation time
                pipe.set(KEY_SIM_TIME, sim_time_to_cache, ex=REDIS_EXPIRATION_SECONDS)

                # If there is edge data, bulk update it to the hash
                if edges_to_cache:
                    pipe.hset(KEY_ALL_EDGES, mapping=edges_to_cache)
                    pipe.expire(KEY_ALL_EDGES, REDIS_EXPIRATION_SECONDS)

                # If there is traffic light data, bulk update it to the hash
                if tls_to_cache:
                    pipe.hset(KEY_ALL_TLS, mapping=tls_to_cache)
                    pipe.expire(KEY_ALL_TLS, REDIS_EXPIRATION_SECONDS)

                print("Background process starting to send all commands to the redis server for execution...")
                start_time = time.monotonic()
                await pipe.execute()
                end_time = time.monotonic()
                execution_time = end_time - start_time
                print(f"pipe.execute() finished, duration: {execution_time:.4f} seconds")
            print("Redis cache update complete.")

        except redis.RedisError as e:
            print(f"Redis Error: {e}")

        #  Add this crucial line to give the event loop a chance to handle other tasks
        await asyncio.sleep(0.1)

#Start and connect to SUMO and Redis
@app.on_event("startup")
async def start_simulation_and_connect():
    global redis_client
    # Connect to Redis
    try:
        redis_url = f"redis://{REDIS_HOST}:{REDIS_PORT}/{REDIS_DB}"
        print(f"Connecting to Redis ({redis_url})...")
        redis_client = redis.from_url(redis_url, decode_responses=True)
        await redis_client.ping()
        connection_status["redis_connected"] = True
        print("Successfully connected to Redis。")
    except redis.RedisError as e:
        connection_status["message"] = f"Failed to connect to Redis: {e}"
        print(connection_status["message"])
        # If Redis connection fails, we can choose not to proceed with starting SUMO
        return

    # 2. Use traci.start to launch and connect to SUMO
    try:
        print("Launching SUMO process using traci.start...")
        # traci.start will launch the process defined in sumoCmd and automatically connect to the specified port
        traci.start(sumoCmd, port=TRACI_PORT)
        connection_status["sumo_connected"] = True
        connection_status["message"] = "Service is ready"
        print(f"Successfully launched and connected to SUMO (Port: {TRACI_PORT})。")

        # After SUMO starts, immediately execute internal initialization tasks
        print("SUMO connected, starting one-time initialization tasks...")
        generate_and_send_junction_names()
        build_all_conflict_maps()
        build_junction_tls_maps()
        verify_official_junction_names()
        print("One-time initialization tasks completed.")
    except Exception as e:
        connection_status["message"] = f"Failed to start SUMO: {e}"
        print(connection_status["message"])
        # If SUMO fails to start, close the established Redis connection
        if redis_client:
            await redis_client.close()
        return

    # 3. Start the background simulation task
    stop_simulation_event.clear()
    simulation_task = asyncio.create_task(simulation_loop())
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
# 没有被用上，因为后端默认会传state
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

"""Get real-time status of a specific vehicle"""
@app.get("/vehicle/{vehicleID}/status", summary="Get real-time status of a specific vehicle")
async def get_vehicle_status_realtime(vehicleID):
    try:
        # 检查车辆是否存在
        if vehicleID not in traci.vehicle.getIDList():
            raise HTTPException(status_code=404, detail=f"Vehicle '{vehicleID}' not found in simulation.")

        return {
            "speed": traci.vehicle.getSpeed(vehicleID),
            "position": traci.vehicle.getPosition(vehicleID),
            "lane": traci.vehicle.getLaneID(vehicleID)
        }
    except traci.TraCIException as e:
        raise HTTPException(status_code=500, detail=f"Error while fetching status for vehicle '{vehicleID}': {e}")

"""Close all connections"""
@app.on_event("shutdown")
async def shutdown_connections():
    # 1. 停止后台任务
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
    处理来自后端的WebSocket连接，并持续监听事件触发指令。
    """
    await websocket.accept()
    print("[Special Event] 后端WebSocket客户端已连接。")
    try:
        while True:
            # 1. 等待并接收来自后端的消息
            data = await websocket.receive_text()

            try:
                # 2. 解析JSON指令
                command = json.loads(data)
                event_type = command.get("event_type")
                event_id = command.get("event_id")
                duration = command.get("duration")

                if not all([event_type, event_id, duration]):
                    raise ValueError("[Special Event] 指令缺少必需字段: event_type, event_id, or duration")

                print(f"[Special Event] 收到WebSocket指令: event_id={event_id}, type={event_type}")

                # 3. 根据 event_type 调用不同的事件管理器方法
                result = {}
                async with TRACI_LOCK:
                    if event_type == "vehicle_breakdown":
                        result = await event_manager.trigger_vehicle_breakdown(event_id, duration)
                    elif event_type == "vehicle_collision":
                        result = await event_manager.trigger_vehicle_collision(event_id, duration)
                    elif event_type == "lane_closure":
                        lane_ids = command.get("lane_ids", [])
                        if not lane_ids:
                            raise ValueError("lane_closure 事件缺少 lane_ids 参数")
                        result = await event_manager.trigger_lane_closure(event_id, duration, lane_ids)
                    else:
                        raise ValueError(f"[Special Event] 未知的事件类型: {event_type}")

                # 4. 将执行结果通过WebSocket发回给后端
                if result.get("success"):
                    response_payload = {"status": "success", "event_id": event_id, **result.get("details", {})}
                    await websocket.send_text(json.dumps(response_payload))
                else:
                    response_payload = {"status": "success", "event_id": event_id, **result.get("details", {})}
                    await websocket.send_text(json.dumps(response_payload))

            except json.JSONDecodeError:
                await websocket.send_text(json.dumps({"status": "fail", "message": "无效的JSON格式。"}))
            except ValueError as e:
                await websocket.send_text(json.dumps({"status": "fail", "message": str(e)}))

    except WebSocketDisconnect:
        print("[Special Event] 后端WebSocket客户端已断开连接。")
    except Exception as e:
        print(f"WebSocket处理时发生未知错误: {e}")
        print("--- 详细错误堆栈跟踪 ---")
        traceback.print_exc()
        print("--------------------------")


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
            street_name = traci.edge.getStreetName(edge_id)
            if street_name:  # Ensure the name is not empty
                street_names.add(street_name)

        # 4. Consolidate the names
        if street_names:
            # Sort to ensure consistent name order, then join with " - "
            junction_name = "-".join(sorted(list(street_names)))
            junction_names_map[jid] = junction_name
        else:
            # If a Junction has no connecting edges with names, give it a default name
            junction_names_map[jid] = f"Unnamed Junction ({jid})"

    if not junction_names_map:
        print("Failed to generate any junction names, task ending.")
        return

    print(f"Successfully generated names for {len(junction_names_map)} junctions...")
    return junction_names_map

# Analyzes the traffic light logic in the simulation to build a conflict map for each light.
# This function should be called once after the simulation starts.
def build_all_conflict_maps():
    print("Building conflict relationship maps for traffic lights...")
    tls_ids = traci.trafficlight.getIDList()
    for tlsID in tls_ids:
        try:
            logics = traci.trafficlight.getCompleteRedYellowGreenDefinition(tlsID)
            if not logics:
                print(f"Warning: No logic definition found for traffic light '{tlsID}'.")
                continue
            program = logics[0]
            if not program.phases:
                print(f"Warning: No phases found in the program for traffic light '{tlsID}'.")
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
            print(f"Error building conflict map for {tlsID}: {e}")
    print(f"Successfully built conflict maps for {len(tls_conflict_maps)} traffic lights.")

def build_junction_tls_maps():
    """
    Builds a map to translate from junction IDs to their corresponding traffic light IDs.
    This is necessary because the tlsID might have a prefix (e.g., "GS_") while the
    junctionID does not. This function should be called once after the simulation starts.
    构建从Junction ID到其对应的交通灯ID之间的映射。
    这是必需的，因为tlsID可能有前缀（如“GS_”），而JunctionID没有。
    此函数应在仿真启动后调用一次。
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
                print(
                    f"Warning: The Junction ID '{tls_id}'inferred for '{potential_junction_id}' based on rules does not exist in the network and has been skipped.")

    except traci.TraCIException as e:
        print(f"TraCI error occurred while building Junction/TLS map: {e}")

    if not junction_to_tls_map:
        print("Warning: Failed to build any Junction ID -> TLS ID mappings. The API may not be able to find traffic lights by Junction ID")
    else:
        print(f"Successfully built Junction->TLS map for {len(junction_to_tls_map)} junctions.")
        # For debugging, print the entire map
        # For debugging, print the entire map
        #print("Completed map: ", junction_to_tls_map)

# Checks if all junctions that have a corresponding traffic light in junction_to_tls_map

def verify_official_junction_names():
    print("Verifying that all controlled junctions have official names...")
    unnamed_junctions_found = []

    # Iterate through all junction IDs that have a mapped traffic light
    for junction_id in junction_to_tls_map.keys():
        # Get the name of the junction from the name map
        junction_name = junction_names_map.get(junction_id)

        # Check if the name exists and if it is in the default "Unnamed" format
        if not junction_name or junction_name.startswith("Unnamed Junction"):
            unnamed_junctions_found.append(junction_id)

    if unnamed_junctions_found:
        print("\n--- WARNING: The following controlled junctions were found to be using default names ---")
        for jid in unnamed_junctions_found:
            print(f"  - Junction ID: '{jid}' has the name '{junction_names_map.get(jid)}'")
        print("This may mean that the corresponding edges in the network file (.net.xml) are missing the 'streetName' attribute.")
        print("----------------------------------------------------------\n")
    else:
        # If no unnamed junctions are found, print a success message and a detailed list
        print("\n--- VERIFICATION PASSED: All controlled junctions have been successfully named ---")
        # Iterate through the mapped junctions and print their IDs and official names
        # for junction_id, tls_id in junction_to_tls_map.items():
        #     junction_name = junction_names_map.get(junction_id)
        #     print(f"  - Junction ID: '{junction_id}' -> Name: '{junction_name}' (Controls TLS: '{tls_id}')")
        # print("-----------------------------------------------------\n")

if __name__ == "__main__":
    # Use uvicorn to run the FastAPI application
    # reload=True means the server will automatically restart after code changes are saved
    uvicorn.run("withRedis:app", host="0.0.0.0", port=8000, reload=True)