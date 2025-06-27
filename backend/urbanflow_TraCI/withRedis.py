import os
import json
import redis.asyncio as redis
import uvicorn
from fastapi import FastAPI, HTTPException, status
import asyncio
from pydantic import BaseModel
import traci
import time

# --- 配置 ---
# 确保SUMO_HOME环境变量已设置
# if 'SUMO_HOME' in os.environ:
#     tools = os.path.join(os.environ['SUMO_HOME'], 'tools')
#     # 将上述 tools 路径添加到 Python 的模块搜索路径（sys.path）中
#     sys.path.append(tools)
# else:
#     sys.exit("Please configure the environment variable “SUMO_HOME” first.")
#
# print(tools)

# SUMO
sumoBinary = "E:/sumo/sumo-1.23.1/bin/sumo"
#sumoBinary = "sumo"
TRACI_PORT = 8813
sumoCmd = [sumoBinary, "-c", "E:/sumo/sumo-1.23.1/tools/2025-06-22-11-26-25/osm.sumocfg",  "--start"]
#sumoCmd = ["sumo", "-c", "sumo_scenario/osm.sumocfg"]

# --- Redis 配置 ---
# 读取 JSON 配置文件 ---
with open('config.json', 'r') as f:
    config = json.load(f)
# 从解析后的字典中获取 Redis 配置
redis_config = config.get('redis', {})
REDIS_HOST = redis_config.get('host')
REDIS_PORT = redis_config.get('port')
REDIS_DB = redis_config.get('db')
SUMO_HOST = "host.docker.internal"

# --- Pydantic Models for Request Bodies ---
# This tells FastAPI how to validate and parse the incoming JSON
class DurationPayload(BaseModel):
    junctionId: str
    duration: int

class StateDurationPayload(BaseModel):
    junctionId: str
    state: str
    duration: int
    lightIndex: int



# --- 创建FastAPI应用和全局变量 ---
app = FastAPI(
    title="TraCI service",
    description="A model to provide SUMO simulation data and control",
    version="1.0.0"
)

connection_status = {"sumo_connected": False, "redis_connected": False, "message": "Initializing..."}
TRACI_LOCK = asyncio.Lock()
# 结构: { "tls_id": {"state": "AWAITING_VERIFICATION", "data": {...}, "execution_time": ...}, ... }
TASK_SCHEDULER = {}
redis_client: redis.Redis = None
simulation_task = None
stop_simulation_event = asyncio.Event()
# Redis中缓存数据的过期时间（秒）
REDIS_EXPIRATION_SECONDS = 600

# --- Redis 键名常量 ---
# 使用常量来管理Redis键名，方便维护
KEY_SIM_TIME = "sumo:simulation_time"
KEY_ALL_EDGES = "sumo:edge"
KEY_ALL_TLS = "sumo:tls"

# 用于存储启动时生成的静态数据的全局变量
junction_names_map = {}
tls_conflict_maps = {}
junction_to_tls_map = {}


# 推进仿真
async def simulation_loop():
    print("后台调度器循环已启动。")

    while not stop_simulation_event.is_set():
        print("[SimLoop] 循环开始，等待锁...")

        # 【死锁修复】: 创建一个临时列表，用于存放需要在释放锁之后才被触发的事件。
        events_to_set = []

        # 初始化将要缓存到Redis的数据容器
        edges_to_cache = {}
        tls_to_cache = {}

        # 加锁以确保任何时候只有一个任务在与traci交互
        async with TRACI_LOCK:
            print("[SimLoop] 锁已获取。")
            try:
                # 1. 推进仿真一步
                print("[SimLoop] 调用 traci.simulationStep()...")
                traci.simulationStep()
                current_time = traci.simulation.getTime()
                print(f"current time is {current_time}")
                sim_time_to_cache = current_time

                # 2. 检查并执行调度器中的任务
                if TASK_SCHEDULER:
                    print(f"[SimLoop] 发现任务: {list(TASK_SCHEDULER.keys())}")
                    for tls_id, task in list(TASK_SCHEDULER.items()):
                        task_state = task.get("state")
                        print(f"[SimLoop] > 正在处理任务 '{tls_id}'，状态: '{task_state}'")

                        verification_event = task.get("verification_event")
                        verification_result = task.get("verification_result", {})

                        # 任务状态机处理
                        if task_state == "AWAITING_VERIFICATION":
                            expected_state = task["data"]["state"]
                            print(f"[SimLoop] > > 任务 '{tls_id}' 等待验证中...")
                            verified_state = traci.trafficlight.getRedYellowGreenState(tls_id)
                            print(f"[SimLoop] > > > 期望状态: '{expected_state}', 实际获取状态: '{verified_state}'")

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
                                print(f"[SimLoop] > > >  '{tls_id}' 加入待办列表")
                                events_to_set.append(verification_event)

                        elif task_state == "RUNNING_MANUAL_PHASE":
                            if current_time >= task.get("execution_time", float('inf')):
                                traci.trafficlight.setProgram(tls_id, "0")
                                print(f"Task[{tls_id}]: Time's up. Default program has been restored silently. Task lifecycle ended.")
                                del TASK_SCHEDULER[tls_id]

                # 3. 更新数据到 Redis
                print("[SimLoop] 开始更新数据...")
                cache_map = {KEY_SIM_TIME: current_time}

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


                    # if cache_map:
                    #     async with redis_client.pipeline(transaction=False) as pipe:
                    #         await pipe.mset(cache_map)
                    #         for key in cache_map:
                    #             await pipe.expire(key, REDIS_EXPIRATION_SECONDS)
                    #         await pipe.execute()

                    # # 使用Pipeline批量执行命令，提高效率
                    # async with redis_client.pipeline(transaction=False) as pipe:
                    #     # 缓存仿真时间
                    #     await pipe.set(KEY_SIM_TIME, current_time, ex=REDIS_EXPIRATION_SECONDS)
                    #
                    #     # 缓存道路状态
                    #     edge_data = {}
                    #     for edgeID in traci.edge.getIDList():
                    #         edge_data = {
                    #             "edgeID": edgeID,
                    #             "edgeName": traci.edge.getStreetName(edgeID),
                    #             "timestamp": current_time,
                    #             "laneNumber": traci.edge.getLaneNumber(edgeID),
                    #             "speed": traci.edge.getLastStepMeanSpeed(edgeID),
                    #             'vehicleCount': traci.edge.getLastStepVehicleNumber(edgeID),
                    #             'vehicleIDs': traci.edge.getLastStepVehicleIDs(edgeID),
                    #             'waitTime': traci.edge.getWaitingTime(edgeID),
                    #             'waitingVehicleIDs': traci.edge.getPendingVehicles(edgeID),
                    #             "waitingVehicleCount": traci.edge.getLastStepHaltingNumber(edgeID)
                    #         }
                    #         # 将字典序列化为JSON字符串存入Redis
                    #         await pipe.set(f"{KEY_EDGE_PREFIX}{edgeID}", json.dumps(edge_data), ex=REDIS_EXPIRATION_SECONDS)
                    #
                    #     # 缓存信号灯状态
                    #     for tlsID in traci.trafficlight.getIDList():
                    #         tls_data = {
                    #             "tlsID": tlsID,
                    #             "junction_id": tlsID,
                    #             "timestamp": current_time,
                    #             "phase": traci.trafficlight.getPhase(tlsID),
                    #             "state": traci.trafficlight.getRedYellowGreenState(tlsID),
                    #             "duration": traci.trafficlight.getPhaseDuration(tlsID),
                    #             "connection": traci.trafficlight.getControlledLinks(tlsID),
                    #             "spendTime": traci.trafficlight.getSpentDuration(tlsID),
                    #             "nextSwitchTime": traci.trafficlight.getNextSwitch(tlsID)
                    #         }
                    #         await pipe.set(f"{KEY_TLS_PREFIX}{tlsID}", json.dumps(tls_data), ex=REDIS_EXPIRATION_SECONDS)
                    #
                    #     # 执行所有缓存命令
                    #     await pipe.execute()


            except traci.TraCIException as step_error:
                # 如果在 simulationStep 中断开连接，我们优雅地处理它
                print(f"[SimLoop] 在仿真步骤中与 SUMO 的连接丢失: {step_error}")
                print("[SimLoop] 将在下一次循环中尝试重连...")
                connection_status["sumo_connected"] = False
                connection_status["message"] = "SUMO connection lost during simulation."
                # 不需要 break，循环会自然继续，并在下一次迭代时进入重连逻辑

        # 4. 立即发送通知 (现在不会被阻塞了)
        if events_to_set:
            print(f"即时发送 {len(events_to_set)} 个通知...")
            for event in events_to_set:
                event.set()

        # 5. 在后台执行耗时的数据库写入 (现在不会阻塞API响应了)
        # if data_to_cache:
        #     try:
        #         print("后台打包数据完毕，开始更新Redis缓存...")
        #         async with redis_client.pipeline(transaction=False) as pipe:
        #             print("准备通过MSET向pip添加命令设置多个键值对...")
        #             await pipe.mset(data_to_cache)
        #             print("准备通过EXPIRE向pip添加命令设置一个以秒为单位的过期时间...")
        #             for key in data_to_cache:
        #                 await pipe.expire(key, REDIS_EXPIRATION_SECONDS)
        #             print("后台开始将所有命令发送到redis服务器并执行...")
        #             # 2. 在执行前记录单调时间
        #             start_time = time.monotonic()
        #             await pipe.execute()
        #             # 3. 在执行后记录单调时间
        #             end_time = time.monotonic()
        #
        #             # 4. 计算差值并打印结果
        #             execution_time = end_time - start_time
        #             print(f"pipe.execute() 执行完毕，耗时: {execution_time:.4f} 秒")
        #         print("Redis缓存更新完毕。")
        #     except redis.RedisError as e:
        #         print(f"Redis Error: {e}")
        try:
            print("后台打包数据完毕，开始更新Redis缓存...")
            async with redis_client.pipeline(transaction=False) as pipe:
                # 缓存仿真时间
                pipe.set(KEY_SIM_TIME, sim_time_to_cache, ex=REDIS_EXPIRATION_SECONDS)

                # 如果有道路数据，则批量更新到哈希
                if edges_to_cache:
                    pipe.hset(KEY_ALL_EDGES, mapping=edges_to_cache)
                    pipe.expire(KEY_ALL_EDGES, REDIS_EXPIRATION_SECONDS)

                # 如果有信号灯数据，则批量更新到哈希
                if tls_to_cache:
                    pipe.hset(KEY_ALL_TLS, mapping=tls_to_cache)
                    pipe.expire(KEY_ALL_TLS, REDIS_EXPIRATION_SECONDS)

                print("后台开始将所有命令发送到redis服务器并执行...")
                start_time = time.monotonic()
                await pipe.execute()
                end_time = time.monotonic()
                execution_time = end_time - start_time
                print(f"pipe.execute() 执行完毕，耗时: {execution_time:.4f} 秒")
            print("Redis缓存更新完毕。")
        except redis.RedisError as e:
            print(f"Redis Error: {e}")

        # !! 加上这关键的一行，让事件循环有机会处理其他任务 !!
        await asyncio.sleep(0.1)  # 暂停0.1秒，这个时间可以根据需要调整

#启动并连接SUMO
@app.on_event("startup")
async def start_simulation_and_connect():
    global redis_client
    # 连接 Redis
    try:
        redis_url = f"redis://{REDIS_HOST}:{REDIS_PORT}/{REDIS_DB}"
        print(f"正在连接到 Redis ({redis_url})...")
        # 使用 from_url 是创建客户端的推荐方式之一
        redis_client = redis.from_url(redis_url, decode_responses=True)
        await redis_client.ping()
        connection_status["redis_connected"] = True
        print("成功连接到 Redis。")
    except redis.RedisError as e:
        connection_status["message"] = f"连接 Redis 失败: {e}"
        print(connection_status["message"])
        # 如果Redis连接失败，可以选择不继续启动SUMO
        return

    # 2. 使用 traci.start 启动并连接 SUMO
    try:
        print("正在使用 traci.start 启动 SUMO 进程...")
        # traci.start会启动sumoCmd定义的进程，并自动连接到指定的端口
        traci.start(sumoCmd, port=TRACI_PORT)
        connection_status["sumo_connected"] = True
        connection_status["message"] = "服务已就绪"
        print(f"成功启动并连接到 SUMO (端口: {TRACI_PORT})。")

        # SUMO启动后，立即执行内部初始化任务
        print("SUMO已连接，开始执行一次性启动任务...")
        generate_and_send_junction_names()
        build_all_conflict_maps()
        build_junction_tls_maps()
        verify_official_junction_names()
        print("一次性启动任务执行完毕。")
    except Exception as e:
        connection_status["message"] = f"启动 SUMO 失败: {e}"
        print(connection_status["message"])
        # 如果SUMO启动失败，关闭已连接的Redis
        if redis_client:
            await redis_client.close()
        return

    # 3. 启动后台仿真任务
    stop_simulation_event.clear()
    simulation_task = asyncio.create_task(simulation_loop())
    print("[FastAPI] 后台仿真任务已创建。")


"""告知后端本模块是否已成功启动并连接到SUMO"""
@app.get("/status", summary="Check if the service is ready")
async def get_status():
    return {"connection": connection_status}


"""获取指定道路的完整状态"""
async def get_edge_status(edgeID):
    # 从 'sumo:edges' 哈希中获取指定 edgeID 的数据
    edge_data_json = await redis_client.hget(KEY_ALL_EDGES, edgeID)
    if edge_data_json:
        return {"edgeID": edgeID, "edgeData": json.loads(edge_data_json)}
    else:
        # 如果在哈希中找不到该道路，返回空数据
        return {"edgeID": edgeID, "edgeData": None}


"""读取指定信号灯的最新状态"""
async def get_tls_status(tlsID):
    # 从 'sumo:tls' 哈希中获取指定 tlsID 的数据
    light_data_json = await redis_client.hget(KEY_ALL_TLS, tlsID)
    if light_data_json:
        return {"tlsID": tlsID, "lightData": json.loads(light_data_json)}
    else:
        # 如果在哈希中找不到该信号灯，返回空数据
        return {"tlsID": tlsID, "lightData": None}

# 模式一：只修改duration
@app.post("/trafficlight/set_duration", summary="Restore the default state of a specific traffic light")
async def modify_tls_duration(payload: DurationPayload):
    tls_id = junction_to_tls_map.get(payload.junctionId)
    async with TRACI_LOCK:
        print(f"[API {tls_id}] 收到请求: duration={payload.duration}")
        traci.trafficlight.setPhaseDuration(tls_id, payload.duration)
        return {"status": "success", "junctionId": payload.junctionId, "duration_set": payload.duration}


# 模式二：同时修改state和duration
@app.post("/trafficlight/set_state_duration", summary="Modify the state of a specific traffic light")
async def modify_tls_state_duration(payload: StateDurationPayload):
    junctionId = payload.junctionId
    state = payload.state
    duration = payload.duration
    index = payload.lightIndex
    tls_id = junction_to_tls_map.get(junctionId)
    print(f"[API {tls_id}] 收到请求: state='{state}', duration={duration}, index={index}")

    current_state = await get_tls_status(tls_id)
    current_state_str = current_state["lightData"].get("state")
    print(f"[API {tls_id}] 获取到当前状态: {current_state_str}")

    state_list = list(current_state_str)

    if index >= len(state_list):
        return {
            "status": "error",
            "message": f"索引 {index} 超出范围。交通灯 '{tls_id}' 共有 {len(state_list)} 个信号。"
        }

        # 2. 查找冲突地图
    conflict_map = tls_conflict_maps.get(tls_id)
    if not conflict_map:
        return {"status": "error", "message": f"未找到交通灯 '{tls_id}' 的冲突地图。"}


    verification_event = asyncio.Event()
    verification_result = {}  # Mutable dictionary to hold the result

    async with TRACI_LOCK:
        print(f"[API {tls_id}] 锁已获取。")
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
                return {"status": "error", "message": "无效状态。必须是 'G' 或 'r'。"}

            new_state_string = "".join(state_list)
            print(f"[API {tls_id}] 计算出的新状态: {new_state_string}")

            traci.trafficlight.setRedYellowGreenState(tls_id, new_state_string)
            print(f"[API {tls_id}] setRedYellowGreenState 命令已发送。")

            # 在调度器中创建任务
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
        print(f"[API {tls_id}] 锁已释放。")

    print(f"[API {tls_id}] 开始等待事件信号 (超时时间: 15秒)...")
    # Wait for the event to be set by the loop
    try:
        await asyncio.wait_for(verification_event.wait(), timeout=400.0)
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
    # 返回一个简单的JSON对象，方便Java解析
    return {"exists": exists}


"""实时查询指定车辆的状态"""
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

"""关闭所有连接"""
@app.on_event("shutdown")
async def shutdown_connections():
    # 1. 停止后台任务
    if simulation_task:
        print("[FastAPI] 应用关闭，正在发送停止信号到后台任务...")
        stop_simulation_event.set()
        await simulation_task
        print("[FastAPI] 后台任务已成功停止。")

    # 2. 使用 traci.close 关闭SUMO连接和进程
    # 这是traci.start模式下必须的步骤，它会终止我们启动的SUMO子进程
    if connection_status.get("sumo_connected"):
        traci.close()
        connection_status["sumo_connected"] = False
        print("[FastAPI] TraCI 连接已关闭, SUMO 进程已终止。")

    # 3. 关闭Redis连接
    if redis_client:
        await redis_client.close()
        connection_status["redis_connected"] = False
        print("[FastAPI] Redis connection closed")


def generate_and_send_junction_names():
    # 1. 获取所有Junction的ID
    junction_ids = traci.junction.getIDList()

    for jid in junction_ids:
        # SUMO内部的Junction通常以':'开头，我们通常不关心这些，可以跳过
        if jid.startswith(':'):
            continue

        # 2. 获取该Junction的入口和出口Edge的ID
        incoming_edges = traci.junction.getIncomingEdges(jid)
        outgoing_edges = traci.junction.getOutgoingEdges(jid)

        # 使用set来自动处理重复的道路名
        street_names = set()

        # 3. 获取所有相关Edge的街道名称
        for edge_id in (incoming_edges + outgoing_edges):
            street_name = traci.edge.getStreetName(edge_id)
            if street_name:  # 确保名称不为空
                street_names.add(street_name)

        # 4. 整合名称
        if street_names:
            # 排序以保证名称的顺序一致性，然后用 " / " 连接
            junction_name = "-".join(sorted(list(street_names)))
            junction_names_map[jid] = junction_name
        else:
            # 如果一个Junction没有任何有名字的道路连接，给一个默认名
            junction_names_map[jid] = f"Unnamed Junction ({jid})"

    if not junction_names_map:
        print("未能生成任何交叉口名称，任务结束。")
        return

    print(f"已成功生成 {len(junction_names_map)} 个交叉口的名称...")
    return junction_names_map

def build_all_conflict_maps():
    """
    分析仿真中的交通灯逻辑，为每个交通灯构建一个冲突连接的地图。
    这个函数应该在仿真启动后调用一次。
    """
    print("正在为交通灯构建冲突关系图...")
    tls_ids = traci.trafficlight.getIDList()
    for tlsID in tls_ids:
        try:
            logics = traci.trafficlight.getCompleteRedYellowGreenDefinition(tlsID)
            if not logics:
                print(f"警告: 未找到交通灯 '{tlsID}' 的逻辑定义。")
                continue
            program = logics[0]
            if not program.phases:
                print(f"警告: 交通灯 '{tlsID}' 的程序中未找到任何相位。")
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
            print(f"为 {tlsID} 构建冲突图时出错: {e}")
    print(f"已成功为 {len(tls_conflict_maps)} 个交通灯构建冲突关系图。")

def build_junction_tls_maps():
    """
    Builds a map to translate from junction IDs to their corresponding traffic light IDs.
    This is necessary because the tlsID might have a prefix (e.g., "GS_") while the
    junctionID does not. This function should be called once after the simulation starts.
    构建从Junction ID到其对应的交通灯ID之间的映射。
    这是必需的，因为tlsID可能有前缀（如“GS_”），而JunctionID没有。
    此函数应在仿真启动后调用一次。
    """
    print("正在基于前缀规则构建 Junction ID -> TLS ID 映射...")
    prefix = "GS_"

    try:
        all_tls_ids = traci.trafficlight.getIDList()
        # 获取一次所有真实的Junction ID，用于后续验证
        all_junction_ids = set(traci.junction.getIDList())

        for tls_id in all_tls_ids:
            potential_junction_id = ""
            if tls_id.startswith(prefix):
                potential_junction_id = tls_id[len(prefix):]
            else:
                # 如果没有前缀，则假定ID相同
                potential_junction_id = tls_id

            # 验证推断出的 junction_id 是否真实存在
            if potential_junction_id in all_junction_ids:
                junction_to_tls_map[potential_junction_id] = tls_id
            else:
                print(
                    f"警告: 根据规则为 '{tls_id}' 推断出的 Junction ID '{potential_junction_id}' 在路网中不存在，已跳过。")

    except traci.TraCIException as e:
        print(f"构建Junction/TLS映射时发生TraCI错误: {e}")

    if not junction_to_tls_map:
        print("警告：未能构建任何 Junction ID -> TLS ID 映射。API可能无法通过Junction ID找到交通灯。")
    else:
        print(f"已成功为 {len(junction_to_tls_map)} 个交叉口构建Junction->TLS映射。")
        # For debugging, print the entire map
        # 为了调试，打印整个映射
        #print("构建完成的映射: ", junction_to_tls_map)


def verify_official_junction_names():
    """
    检查所有在 junction_to_tls_map 中有对应交通灯的交叉口
    是否都拥有一个正式的、非默认的名称。
    此函数应在所有映射构建完毕后调用。
    """
    print("正在验证所有受控交叉口是否都有正式名称...")
    unnamed_junctions_found = []

    # 遍历所有已映射了交通灯的交叉口ID
    for junction_id in junction_to_tls_map.keys():
        # 从名称映射中获取该交叉口的名称
        junction_name = junction_names_map.get(junction_id)

        # 检查名称是否存在，以及是否是默认的 "Unnamed" 格式
        if not junction_name or junction_name.startswith("Unnamed Junction"):
            unnamed_junctions_found.append(junction_id)

    if unnamed_junctions_found:
        print("\n--- 警告: 发现以下受控交叉口使用了默认名称 ---")
        for jid in unnamed_junctions_found:
            print(f"  - Junction ID: '{jid}' 的名称是 '{junction_names_map.get(jid)}'")
        print("这可能意味着路网文件(.net.xml)中对应的道路(edge)缺少'streetName'属性。")
        print("----------------------------------------------------------\n")
    else:
        # 如果没有发现未命名的交叉口，则打印成功信息和详细列表
        print("\n--- 验证通过：所有受控交叉口均已成功命名 ---")
        # 遍历已映射的交叉口并打印其ID和正式名称
        # for junction_id, tls_id in junction_to_tls_map.items():
        #     junction_name = junction_names_map.get(junction_id)  # 添加一个备用值以防万一
        #     print(f"  - Junction ID: '{junction_id}' -> Name: '{junction_name}' (Controls TLS: '{tls_id}')")
        # print("-----------------------------------------------------\n")

if __name__ == "__main__":
    # 使用uvicorn来运行FastAPI应用
    # reload=True 意味着代码保存后服务器会自动重启
    uvicorn.run("withRedis:app", host="0.0.0.0", port=8000, reload=True)