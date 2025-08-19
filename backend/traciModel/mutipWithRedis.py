import os
import json
import redis.asyncio as redis
import uvicorn
from fastapi import FastAPI, HTTPException, status
import asyncio
from pydantic import BaseModel
import traci
import multiprocessing
from worker import caching_worker # 从创建的文件导入函数

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
#sumoBinary = "E:/sumo/sumo-1.23.1/bin/sumo"
sumoBinary = "sumo"
TRACI_PORT = 8813
#sumoCmd = [sumoBinary, "-c", "E:/sumo/sumo-1.23.1/tools/2025-06-12-01-35-23/osm.sumocfg",   "--num-clients", "2"]
sumoCmd = ["sumo", "-c", "sumo_scenario/osm.sumocfg", "--num-clients", "2"]

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
    junction_id: str
    duration: int

class StateDurationPayload(BaseModel):
    junction_id: str
    state: str
    duration: int
    light_index: int

class JunctionPayload(BaseModel):
    junction_id: str


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
REDIS_EXPIRATION_SECONDS = 60

task_queue = None
worker_process = None

# --- Redis 键名常量 ---
# 使用常量来管理Redis键名，方便维护
KEY_SIM_TIME = "sumo:simulation_time"
KEY_EDGE_PREFIX = "sumo:edge:"
KEY_TLS_PREFIX = "sumo:tls:"

# 推进仿真
async def simulation_loop():
    print("后台调度器循环已启动。")

    while not stop_simulation_event.is_set():
        # 加锁以确保任何时候只有一个任务在与traci交互
        async with TRACI_LOCK:
            try:
                # 1. 推进仿真一步
                traci.simulationStep()
                current_time = traci.simulation.getTime()
                print(f"current time is {current_time}")

                # 2. 检查并执行调度器中的任务
                if TASK_SCHEDULER:
                    for tls_id, task in list(TASK_SCHEDULER.items()):
                        task_state = task.get("state")
                        verification_event = task.get("verification_event")
                        verification_result = task.get("verification_result", {})

                        # 任务状态机处理
                        if task_state == "AWAITING_VERIFICATION":
                            verified_state = traci.trafficlight.getRedYellowGreenState(tls_id)

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
                                verification_event.set()

                        elif task_state == "RUNNING_MANUAL_PHASE":
                            if current_time >= task.get("execution_time", float('inf')):
                                traci.trafficlight.setProgram(tls_id, "0")
                                print(
                                    f"Task[{tls_id}]: Time's up. Default program has been restored silently. Task lifecycle ended.")
                                del TASK_SCHEDULER[tls_id]

                # 3. 通知工作进程开始缓存当前时间步的数据
                if task_queue:  # 确保队列存在
                    task_queue.put(current_time)

            except traci.TraCIException as step_error:
                # 如果在 simulationStep 中断开连接，我们优雅地处理它
                print(f"[SimLoop] 在仿真步骤中与 SUMO 的连接丢失: {step_error}")
                print("[SimLoop] 将在下一次循环中尝试重连...")
                connection_status["sumo_connected"] = False
                connection_status["message"] = "SUMO connection lost during simulation."
                # 不需要 break，循环会自然继续，并在下一次迭代时进入重连逻辑
        # !! 加上这关键的一行，让事件循环有机会处理其他任务 !!
        await asyncio.sleep(0.2)  # 暂停0.1秒，这个时间可以根据需要调整

#启动并连接SUMO
@app.on_event("startup")
async def start_simulation_and_connect():
    global redis_client
    # 1. 连接 Redis
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
        traci.start(sumoCmd, port=TRACI_PORT, label="main_sim_client")
        traci.setOrder(0)
        connection_status["sumo_connected"] = True
        connection_status["message"] = "服务已就绪"
        print(f"成功启动并连接到 SUMO (端口: {TRACI_PORT})。")
    except Exception as e:
        connection_status["message"] = f"启动 SUMO 失败: {e}"
        print(connection_status["message"])
        # 如果SUMO启动失败，关闭已连接的Redis
        if redis_client:
            await redis_client.close()
        return

    # 3. 在主进程连接成功后，才启动工作进程
    print("主进程已就绪，正在启动后台缓存工作进程...")
    task_queue = multiprocessing.Queue()
    worker_process = multiprocessing.Process(
        target=caching_worker,
        args=(task_queue, redis_config, TRACI_PORT),
        daemon=True # 设置为守护进程，主进程退出时会自动终止
    )
    worker_process.start()

    # 5. 启动后台仿真任务
    simulation_task = asyncio.create_task(simulation_loop())
    print("[FastAPI] 后台仿真任务已创建。")


"""关闭所有连接"""
@app.on_event("shutdown")
async def shutdown_connections():
    # 1. 停止后台任务
    if simulation_task:
        print("[FastAPI] 应用关闭，正在发送停止信号到后台任务...")
        stop_simulation_event.set()
        await simulation_task
        print("[FastAPI] 后台任务已成功停止。")

    # 2. 关闭工作进程
    if worker_process and worker_process.is_alive():
        print("[FastAPI] 正在发送关闭信号到工作进程...")
        task_queue.put(None) # 发送哨兵值
        worker_process.join(timeout=10) # 等待工作进程结束，最多10秒
        if worker_process.is_alive():
            print("[FastAPI] 工作进程超时，强制终止。")
            worker_process.terminate()
        else:
            print("[FastAPI] 工作进程已成功关闭。")

    # 3. 使用 traci.close 关闭SUMO连接和进程
    # 这是traci.start模式下必须的步骤，它会终止我们启动的SUMO子进程
    if connection_status.get("sumo_connected"):
        traci.close()
        connection_status["sumo_connected"] = False
        print("[FastAPI] TraCI 连接已关闭, SUMO 进程已终止。")

    # 4. 关闭Redis连接
    if redis_client:
        await redis_client.close()
        connection_status["redis_connected"] = False
        print("[FastAPI] Redis connection closed")


"""告知后端本模块是否已成功启动并连接到SUMO"""
@app.get("/status", summary="Check if the service is ready")
async def get_status():
    return {"connection": connection_status}


"""获取指定道路的完整状态"""
@app.get("/edge/{edgeID}/status", summary="Get status of a specific edge")
async def get_edge_status(edgeID):
    edge_data_json = await redis_client.get(f"{KEY_EDGE_PREFIX}{edgeID}")
    return {"edgeID": edgeID, "edgeData": json.loads(edge_data_json)}


"""读取指定信号灯的最新状态"""
@app.get("/trafficlight/{tlsID}/status", summary="Get status of a specific traffic light")
async def get_tls_status(tlsID):
    light_data_json = await redis_client.get(f"{KEY_TLS_PREFIX}{tlsID}")
    return {"tlsID": tlsID, "lightData": json.loads(light_data_json)}

# 模式一：只修改duration
@app.post("/trafficlight/set_duration", summary="Restore the default state of a specific traffic light")
async def modify_tls_duration(payload: DurationPayload):
    async with TRACI_LOCK:
        traci.trafficlight.setPhaseDuration(payload.junction_id, payload.duration)
        return {"status": "success", "junction_id": payload.junction_id, "duration_set": payload.duration}


# 模式二：同时修改state和duration
@app.post("/trafficlight/set_state_duration", summary="Modify the state of a specific traffic light")
async def modify_tls_state_duration(payload: StateDurationPayload):
    junction_id = payload.junction_id
    new_state_string = payload.state
    duration = payload.duration

    verification_event = asyncio.Event()
    verification_result = {}  # Mutable dictionary to hold the result

    async with TRACI_LOCK:
        try:
            traci.trafficlight.setRedYellowGreenState(junction_id, new_state_string)
            # 在调度器中创建任务
            TASK_SCHEDULER[junction_id] = {
                "state": "AWAITING_VERIFICATION",
                "data": {"state": new_state_string, "duration": duration},
                "verification_event": verification_event,
                "verification_result": verification_result,
            }
            print(f"API: Task ready created for {junction_id}.")
        except traci.TraCIException as e:
            raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                                detail=f"TraCI command execution failed: {e}")

    # Wait for the event to be set by the loop
    try:
        await asyncio.wait_for(verification_event.wait(), timeout=5.0)
    except asyncio.TimeoutError:
        print(f"[API] Waiting for task “{junction_id}” validation timeout!")
        # Clean up the task to prevent it from running later
        if junction_id in TASK_SCHEDULER:
            del TASK_SCHEDULER[junction_id]
        raise HTTPException(status_code=504, detail="Verification timed out. The task might not have been processed in time.")

    return verification_result


@app.get("/junction/exists", summary="Check for the existence of junction")
async def check_junction_exists(payload: JunctionPayload):
    if not connection_status["sumo_connected"]:
        raise HTTPException(status_code=503, detail="SUMO is not connected.")

    async with TRACI_LOCK:
        exists = payload.junction_id in traci.junction.getIDList()
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


if __name__ == "__main__":
    # 使用uvicorn来运行FastAPI应用
    # reload=True 意味着代码保存后服务器会自动重启
    uvicorn.run("mutipWithRedis:app", host="0.0.0.0", port=8000)