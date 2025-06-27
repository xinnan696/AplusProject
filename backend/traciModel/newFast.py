
import uvicorn
from fastapi import FastAPI, HTTPException, status
from fastapi.responses import JSONResponse
import asyncio
from pydantic import BaseModel
import httpx
import traci

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
TRACI_PORT = 8813
sumoCmd = [sumoBinary, "-c", "E:/sumo/sumo-1.23.1/tools/2025-06-12-01-35-23/osm.sumocfg",  "--start"]

# --- Pydantic Models for Request Bodies ---
# This tells FastAPI how to validate and parse the incoming JSON
class DurationPayload(BaseModel):
    duration: int

class StateDurationPayload(BaseModel):
    state: str
    duration: int
    webhookUrl: str


# --- 创建FastAPI应用和全局变量 ---
app = FastAPI(
    title="TraCI service",
    description="A model to provide SUMO simulation data and control",
    version="1.0.0"
)

in_memory_cache= {}
connection_status = {"sumo_connected": False, "message": "Initializing..."}
TRACI_LOCK = asyncio.Lock()
# 结构: { "tls_id": {"state": "AWAITING_VERIFICATION", "data": {...}, "execution_time": ...}, ... }
TASK_SCHEDULER = {}


"""异步发送Webhook通知到后端服务"""
async def send_webhook_notification(webhook_url: str, payload: dict):
    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(webhook_url, json=payload, timeout=10.0)
            response.raise_for_status()  # Raises an exception for 4xx/5xx status codes
            print(f"Successfully sent Webhook to {webhook_url}, Task ID: {payload.get('taskId')}")
    except httpx.RequestError as e:
        print(f"!!! [Webhook Error] Failed to send to {e.request.url} (Request Error): {e}")


# 推进仿真
async def simulation_loop():
    print("后台调度器循环已启动。")
    while connection_status["sumo_connected"]:
        # 加锁以确保任何时候只有一个任务在与traci交互
        try:
            await asyncio.sleep(0.1)
            async with TRACI_LOCK:
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
                        webhook_url = task["data"].get("webhookUrl")

                        # 任务状态机处理
                        if task_state == "AWAITING_VERIFICATION":
                            verified_state = traci.trafficlight.getRedYellowGreenState(tls_id)

                            if verified_state == task["data"]["state"]:
                                # Report success and set event
                                print(f"Task '{tls_id}' ready be verified.")
                                verification_result["status"] = "VERIFIED_AND_RUNNING"
                                verification_result["detail"] = f"State for {tls_id} successfully set and verified."
                                if verification_event: verification_event.set()

                                traci.trafficlight.setPhaseDuration(tls_id, task["data"]["duration"] - 1)
                                asyncio.create_task(send_webhook_notification(webhook_url, {"taskId": tls_id,"status": "VERIFIED_AND_RUNNING"}))
                                task["state"] = "RUNNING_MANUAL_PHASE"
                                task["execution_time"] = current_time + task["data"]["duration"] - 1
                            else:
                                error_message = f"Expected state '{task['data']['state']}' but got '{verified_state}'"
                                if webhook_url:
                                    payload = {"taskId": tls_id, "status": "FAILED_VERIFICATION",
                                               "error": error_message}
                                    asyncio.create_task(send_webhook_notification(webhook_url, payload))
                                print(f"Task [{tls_id}]: Verification failed! {error_message}")
                                del TASK_SCHEDULER[tls_id]

                        elif task_state == "RUNNING_MANUAL_PHASE":
                            if current_time >= task.get("execution_time", float('inf')):
                                traci.trafficlight.setProgram(tls_id, "0")
                                print(f"Task[{tls_id}]: Time's up. Default program has been restored silently. Task lifecycle ended.")
                                del TASK_SCHEDULER[tls_id]

                # 3. 更新缓存
                try:
                    in_memory_cache['simulation_time'] = traci.simulation.getTime()

                    # 缓存道路状态
                    edge_statuses = {}
                    for edgeID in traci.edge.getIDList():
                        edge_statuses[edgeID] = {
                            "edgeID": edgeID,
                            "edgeName": traci.edge.getStreetName(edgeID),
                            "laneNumber": traci.edge.getLaneNumber(edgeID),
                            "speed": traci.edge.getLastStepMeanSpeed(edgeID),
                            'vehicleCount': traci.edge.getLastStepVehicleNumber(edgeID),
                            'vehicleIDs': traci.edge.getLastStepVehicleIDs(edgeID),
                            'waitTime': traci.edge.getWaitingTime(edgeID),
                            'waitingVehicleIDs': traci.edge.getPendingVehicles(edgeID),
                            "waitingVehicleCount": traci.edge.getLastStepHaltingNumber(edgeID)
                        }
                    in_memory_cache['edges'] = edge_statuses

                    # 缓存信号灯状态
                    tls_statuses = {}
                    for tlsID in traci.trafficlight.getIDList():
                        tls_statuses[tlsID] = {
                            "tlsID": tlsID,
                            "junctionID": tlsID,
                            "phase": traci.trafficlight.getPhase(tlsID),
                            "state": traci.trafficlight.getRedYellowGreenState(tlsID),
                            "duration": traci.trafficlight.getPhaseDuration(tlsID),
                            "connection": traci.trafficlight.getControlledLinks(tlsID),
                            "spendTime": traci.trafficlight.getSpentDuration(tlsID),
                            "nextSwitchTime": traci.trafficlight.getNextSwitch(tlsID)
                        }
                    in_memory_cache['trafficLights'] = tls_statuses
                except Exception as cache_error:
                    # 如果缓存的整体逻辑出错，打印警告但【不】停止仿真
                    print(f"--- [缓存警告] 后台数据缓存步骤发生错误: {cache_error} ---")

        except traci.TraCIException as e:
            print(f"[FATAL ERROR] TraCI error in background loop, task terminating: {e}")
            connection_status["sumo_connected"] = False
            connection_status["message"] = "The simulation was interrupted by an error."
            break  # 发生错误，退出while循环


async def generate_and_send_junction_names():
    # 给予仿真一点启动时间，确保所有组件都已加载
    await asyncio.sleep(1.0)
    print("开始生成并发送交叉口名称...")

    junction_names_map = {}

    # 加锁以确保与TraCI的交互是线程安全的
    async with TRACI_LOCK:
        try:
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
                    junction_name = " / ".join(sorted(list(street_names)))
                    junction_names_map[jid] = junction_name
                else:
                    # 如果一个Junction没有任何有名字的道路连接，给一个默认名
                    junction_names_map[jid] = f"Unnamed Junction ({jid})"

        except traci.TraCIException as e:
            print(f"!!! [Junction Naming Error] 在生成交叉口名称时与TraCI交互失败: {e}")
            return  # 发生错误，终止此任务

    # 5. 检查是否成功生成了名称
    if not junction_names_map:
        print("未能生成任何交叉口名称，任务结束。")
        return

    print(f"已成功生成 {len(junction_names_map)} 个交叉口的名称，准备发送到后端...")

#启动并连接SUMO
@app.on_event("startup")
async def start_simulation_and_connect():
    # 启动并连接SUMO
    try:
        print("Starting SUMO and connecting to TraCI...")
        traci.start(sumoCmd, TRACI_PORT)
        connection_status["sumo_connected"] = True
        asyncio.create_task(generate_and_send_junction_names())
        asyncio.create_task(simulation_loop())
    except (traci.TraCIException, FileNotFoundError) as e:
        connection_status["message"] = f"Failed to start or connect to TraCI: {e}"
        print(connection_status["message"])


"""告知后端本模块是否已成功启动并连接到SUMO"""
@app.get("/status", summary="Check if the service is ready")
async def get_status():
    return {"connection": connection_status, "simulation_time": in_memory_cache.get("simulation_time")}


"""获取指定道路的完整状态"""
@app.get("/edge/{edgeID}/status", summary="Get status of a specific edge")
async def get_edge_status(edgeID):
    edgeData =in_memory_cache.get('edges', {}).get(edgeID)
    return {"edgeID": edgeID, "edgeData":edgeData}


"""读取指定信号灯的最新状态"""
@app.get("/trafficlight/{tlsID}/status", summary="Get status of a specific traffic light")
async def get_tls_status(tlsID):
    lightData = in_memory_cache.get('trafficLights', {}).get(tlsID)
    return {"tlsID": tlsID, "lightData":lightData}

# 模式一：只修改duration
@app.post("/trafficlight/{tlsID}/set_duration", summary="Restore the default state of a specific traffic light")
async def modify_tls_duration(tlsID, payload: DurationPayload):
    async with TRACI_LOCK:
        traci.trafficlight.setPhaseDuration(tlsID, payload.duration)
        return {"status": "success", "tlsID": tlsID, "duration_set": payload.duration}

# 模式二：同时修改state和duration
@app.post("/trafficlight/{tlsID}/set_state_duration", summary="Modify the state of a specific traffic light")
async def modify_tls_state_duration(tlsID, payload: StateDurationPayload):
    new_state_string = payload.state
    duration = payload.duration
    webhook_url = payload.webhookUrl

    verification_event = asyncio.Event()
    verification_result = {}  # Mutable dictionary to hold the result

    async with TRACI_LOCK:
        try:
            traci.trafficlight.setRedYellowGreenState(tlsID, new_state_string)
            # 在调度器中创建任务
            TASK_SCHEDULER[tlsID] = {
                "state": "AWAITING_VERIFICATION",
                "data": {"state": new_state_string, "duration": duration, "webhookUrl": webhook_url},
                "verification_event": verification_event,
                "verification_result": verification_result,
            }
            print(f"API: Task with Webhook created for {tlsID}.")
        except traci.TraCIException as e:
            raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                                detail=f"TraCI command execution failed: {e}")

    # Wait for the event to be set by the loop
    try:
        await asyncio.wait_for(verification_event.wait(), timeout=5.0)
    except asyncio.TimeoutError:
        print(f"[API] Waiting for task “{tlsID}” validation timeout!")
        # Clean up the task to prevent it from running later
        if tlsID in TASK_SCHEDULER:
            del TASK_SCHEDULER[tlsID]
        raise HTTPException(status_code=504,
                            detail="Verification timed out. The task might not have been processed in time.")

    return verification_result


@app.get("/junction/{junction_id}/exists", summary="Check for the existence of junction")
async def check_junction_exists(junction_id: str):
    if not connection_status["sumo_connected"]:
        raise HTTPException(status_code=503, detail="SUMO is not connected.")

    async with TRACI_LOCK:
        exists = junction_id in traci.junction.getIDList()
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
    if connection_status.get("sumo_connected"):
        await asyncio.sleep(0.2)  # 等待循环退出
        async with TRACI_LOCK:
            traci.close()
            connection_status["sumo_connected"] = False
            print("TraCI connection closed, SUMO process terminated.")


if __name__ == "__main__":
    # 使用uvicorn来运行FastAPI应用
    # reload=True 意味着代码保存后服务器会自动重启
    uvicorn.run("newFast:app", host="0.0.0.0", port=8000, reload=True)