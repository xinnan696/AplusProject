import os
import sys
import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
import asyncio
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
sumoBinary = "E:/sumo/sumo-1.23.1/bin/sumo-gui"
TRACI_PORT = 8813
sumoCmd = [sumoBinary, "-c", "E:/sumo/sumo-1.23.1/tools/2025-06-12-01-35-23/osm.sumocfg",  "--start"]


# --- 创建FastAPI应用和全局变量 ---
app = FastAPI(
    title="TraCI service",
    description="A model to provide SUMO simulation data and control",
    version="1.0.0"
)

in_memory_cache= {}
connection_status = {"sumo_connected": False, "message": "Initializing..."}
TRACI_LOCK = asyncio.Lock()

#启动并连接SUMO
@app.on_event("startup")
async def start_simulation_and_connect():
    # 启动并连接SUMO
    try:
        print("Starting SUMO and connecting to TraCI...")
        traci.start(sumoCmd, port=TRACI_PORT, numRetries=10)
        connection_status["sumo_connected"] = True
        asyncio.create_task(simulation_loop())
    except (traci.TraCIException, FileNotFoundError) as e:
        connection_status["message"] = f"Failed to start or connect to TraCI: {e}"
        print(connection_status["message"])


"""关闭所有连接"""
@app.on_event("shutdown")
async def shutdown_connections():
    if connection_status.get("sumo_connected"):
        await asyncio.sleep(0.2)  # 等待循环退出
        async with TRACI_LOCK:
            traci.close()
            connection_status["is_connected"] = False
            print("TraCI connection closed, SUMO process terminated.")


"""告知后端本模块是否已成功启动并连接到SUMO"""
@app.get("/status", summary="Check if the service is ready")
async def get_status():
    return {"connection": connection_status, "simulation_time": in_memory_cache.get("simulation_time")}

# 推进仿真
async def simulation_loop():
    while connection_status["sumo_connected"]:
        # 加锁以确保任何时候只有一个任务在与traci交互
        async with TRACI_LOCK:
            try:
                traci.simulationStep()
                current_time = traci.simulation.getTime()

                # 更新缓存
                in_memory_cache['time'] = current_time

                # 缓存道路状态
                edge_statuses = {}
                for edgeID in traci.edge.getIDList():
                    edge_statuses[edgeID] = {
                        "edgeID": edgeID,
                        "edgeName": traci.edge.getStreetNumber(edgeID),
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
                        "phase": traci.trafficlight.getPhase(tlsID),
                        "state": traci.trafficlight.getRedYellowGreenState(tlsID),
                        "duration": traci.trafficlight.getPhaseDuration(tlsID),
                        "connection": traci.trafficlight.getControlledLinks(tlsID),
                        "spendTime": traci.trafficlight.getSpentDuration(tlsID),
                        "nextSwitchTime": traci.trafficlight.getNextSwitch(tlsID)
                    }
                in_memory_cache['trafficLights'] = tls_statuses

            except traci.TraCIException as e:
                print(f"后台仿真循环发生TraCI错误，任务终止: {e}")
                connection_status["sumo_connected"] = False
                connection_status["message"] = "仿真因错误而中断。"
                break  # 发生错误，退出while循环

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


@app.post("/trafficlight/{tlsID}/set_state_duration", summary="Modify the state of a specific traffic light")
async def modify_tls_state_duration(tlsID, new_state_string,duration):
    try:
        traci.trafficlight.setRedYellowGreenState(tlsID, new_state_string)
        traci.simulationStep()
        traci.trafficlight.setPhaseDuration(tlsID, duration-1)
        traci.simulationStep()
        verified_state = traci.trafficlight.getRedYellowGreenState(tlsID)
        if verified_state == new_state_string:
            return {
                "status": "success",
                "message": f"Traffic light '{tlsID}' state successfully updated.",
                "error_message": None
            }
        else:
            return {
                "status": "error",
                "message": "Verification failed. The state did not update as expected.",
                "error_message": f"Expected state '{new_state_string}' but got '{verified_state}'."
            }
    except traci.TraCIException as e:
        # 将TraCI特定的异常转换为更通用的错误信息或自定义异常
        return {
            "status": "error",
            "message": f"Failed to execute command for '{tlsID}'.",
            "error_message": str(e)
        }

# 恢复信号灯默认配置
@app.post("/trafficlight/{tlsID}/restore_state", summary="Restore the default state of a specific traffic light")
async def restore_tls_state_to_default(tlsID):
    traci.trafficlight.setProgram(tlsID, "0")

@app.post("/trafficlight/{tlsID}/set_duration", summary="Restore the default state of a specific traffic light")
async def modify_tls_duration(tlsID, duration):
    traci.trafficlight.setPhaseDuration(tlsID, duration)
    traci.simulationStep()

@app.get("/junction/{junction_id}/exists", summary="检查交叉口是否存在")
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



if __name__ == "__main__":
    # 使用uvicorn来运行FastAPI应用
    # reload=True 意味着代码保存后服务器会自动重启
    uvicorn.run("onlyFast:app", host="0.0.0.0", port=8000, reload=True)