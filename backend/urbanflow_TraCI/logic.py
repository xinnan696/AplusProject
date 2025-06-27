from fastapi import FastAPI, HTTPException
import traci


# --- 1. 配置 ---
# 确保SUMO_HOME环境变量已设置
# if 'SUMO_HOME' in os.environ:
#     tools = os.path.join(os.environ['SUMO_HOME'], 'tools')
#     # 将上述 tools 路径添加到 Python 的模块搜索路径（sys.path）中
#     sys.path.append(tools)
# else:
#     sys.exit("Please configure the environment variable “SUMO_HOME” first.")
#
# print(tools)

# SUMO配置
sumoBinary = "E:/sumo/sumo-1.23.1/bin/sumo"
TRACI_PORT = 8813
sumoCmd = [sumoBinary, "-c", "E:/sumo/sumo-1.23.1/tools/2025-06-12-01-35-23/osm.sumocfg",  "--start"]

in_memory_cache= {}
status = {"sumo_connected": False, "message": "Initializing..."}
junction_names_map = {}
tls_conflict_maps = {}  # 用于存储每个交通灯的冲突逻辑

def start_simulation_and_connect():
    # 启动并连接SUMO
    print("Starting SUMO...")
    traci.start(sumoCmd, TRACI_PORT)
    print("Connected to SUMO")
    status["sumo_connected"] = True

    generate_and_send_junction_names()
    # 在连接成功后，立即为所有交通灯构建冲突地图
    build_all_conflict_maps()
    return status


"""关闭所有连接"""
def shutdown_connections():
    if status.get("sumo_connected"):
        traci.close()
        print("TraCI connection closed, SUMO process terminated.")


def simulation_step():
    # 推进仿真
    if not status["sumo_connected"]:
        raise HTTPException(status_code=503, detail="SUMO is not connected.")

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
                "edgeName": traci.edge.getStreetName(edgeID),
                "laneNumber": traci.edge.getLaneNumber(edgeID),
                "speed": traci.edge.getLastStepMeanSpeed(edgeID),
                'vehicleCount': traci.edge.getLastStepVehicleNumber(edgeID),
                'vehicleIDs': traci.edge.getLastStepVehicleIDs(edgeID),
                'waitTime': traci.edge.getWaitingTime(edgeID),
                'waitingVehicleIDs': traci.edge.getPendingVehicles(edgeID),
                "waitingVehicleCount": traci.edge.getLastStepHaltingNumber(edgeID)
            }

        # 缓存信号灯状态
        tls_statuses = {}
        for tlsID in traci.trafficlight.getIDList():
            # 如果tlsID在junction_names_map中不存在，则返回一个默认的提示信息
            junction_name = junction_names_map.get(tlsID, f"Unnamed or Internal TLS ({tlsID})")
            tls_statuses[tlsID] = {
                "tlsID": tlsID,
                "junctionID": tlsID,
                "junctionName": junction_name,
                "phase": traci.trafficlight.getPhase(tlsID),
                "state": traci.trafficlight.getRedYellowGreenState(tlsID),
                "duration": traci.trafficlight.getPhaseDuration(tlsID),
                "connection": traci.trafficlight.getControlledLinks(tlsID),
                "spendTime": traci.trafficlight.getSpentDuration(tlsID),
                "nextSwitchTime": traci.trafficlight.getNextSwitch(tlsID)
            }
        in_memory_cache['trafficlights'] = tls_statuses

        return {"status": "stepped_and_cached", "simulation_time": current_time}
    except traci.TraCIException as e:
        raise HTTPException(status_code=500, detail=f"SUMO simulation step failed: {e}")


def get_lane_status(laneID):
    laneData =in_memory_cache.get('lanes', {}).get(laneID)
    return laneData

def get_tls_status(tlsID):
    lightData = in_memory_cache.get('trafficlights', {}).get(tlsID)
    return lightData

# 修改指定信号灯的状态
def modify_tls_state_duration(tlsID: str, state: str, duration: int, index: int):
    cached_tls_data = in_memory_cache.get('trafficlights', {}).get(tlsID)
    if not cached_tls_data or 'state' not in cached_tls_data:
        return {
            "status": "error",
            "message": f"无法在缓存中找到交通灯'{tlsID}'的状态。请先运行 simulation_step()。"}

    current_state = cached_tls_data['state']
    new_state_list = list(current_state)

    if index >= len(new_state_list):
        return {
            "status": "error",
            "message": f"索引 {index} 超出范围。交通灯 '{tlsID}' 共有 {len(new_state_list)} 个信号。"
        }

    # 2. 查找冲突地图
    conflict_map = tls_conflict_maps.get(tlsID)
    if not conflict_map:
        return {"status": "error", "message": f"未找到交通灯 '{tlsID}' 的冲突地图。"}

    try:
        if state.lower() == 'g':
            new_state_list[index] = 'G'
            conflicting_indices = conflict_map.get(index, set())
            for conflict_idx in conflicting_indices:
                if conflict_idx < len(new_state_list):
                    new_state_list[conflict_idx] = 'r'
        elif state.lower() == 'r':
            new_state_list[index] = 'r'
        else:
            return {"status": "error", "message": "无效状态。必须是 'G' 或 'r'。"}

        new_state_string = "".join(new_state_list)

        # 4. 应用新状态到仿真
        traci.trafficlight.setRedYellowGreenState(tlsID, new_state_string)
        traci.simulationStep()
        traci.trafficlight.setPhaseDuration(tlsID, duration)
        traci.simulationStep()

        # 5. 验证更改 (注意: 验证时仍需直接查询TraCI以获取最新结果)
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
        return {
            "status": "error",
            "message": f"修改交通灯 '{tlsID}' 时发生TraCI错误。",
            "error_details": str(e)
        }


# 恢复信号灯默认配置
def restore_tls_state_to_default(tlsID):
    traci.trafficlight.setProgram(tlsID, "0")
    traci.simulationStep()

# 修改指定信号灯当前相位的持续时间
def modify_tls_duration(tlsID, duration):
    traci.trafficlight.setPhaseDuration(tlsID, duration)
    traci.simulationStep()


"""实时查询指定车辆的状态"""
def get_vehicle_status_realtime(vehicleID):
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


if __name__ == "__main__":
    start_status = start_simulation_and_connect()
    print(f"启动状态: {start_status}")

    TEST_LANE_ID = "-1005997147_0"
    TEST_TLS_ID = "joinedS_12136692966_5056283101_5101079521_5101079523_#3more"
    existing_junction_id = "30846825"

    for step in range(3):
        traci.simulationStep()

    simulation_step()

    # 验证build_all_conflict_maps()生成的map是否正确
    # 1. 获取该交通灯控制的所有连接，建立 索引 -> 连接 的映射
    # controlled_links = traci.trafficlight.getControlledLinks(TEST_TLS_ID)
    # print(f"交通灯 '{TEST_TLS_ID}' 控制的连接列表 (索引 -> 连接):")
    # for i, link in enumerate(controlled_links):
    #     # link[0] 是一个元组 (入口车道, 出口车道, 内部车道)
    #     from_edge = traci.lane.getEdgeID(link[0][0])
    #     to_edge = traci.lane.getEdgeID(link[0][1])
    #     print(f"  Index {i}: 从 {from_edge} (车道: {link[0][0]}) 到 {to_edge} (车道: {link[0][1]})")
    #
    # # 2. 获取并打印该交通灯的冲突地图
    # conflict_map_for_tls = tls_conflict_maps[TEST_TLS_ID]
    # print(f"\n交通灯 '{TEST_TLS_ID}' 的冲突地图 (索引 -> 冲突的索引集合):")
    # # 为了更美观地打印
    # import json
    #
    # # 将 set 转换为 list 以便 json 序列化
    # printable_map = {k: sorted(list(v)) for k, v in conflict_map_for_tls.items()}
    # print(json.dumps(printable_map, indent=2))

    # 测试交通灯修改功能
    if TEST_TLS_ID in tls_conflict_maps:
        print(f"\n--- 测试交通灯修改功能于 '{TEST_TLS_ID}' ---")

        # 1. 从缓存获取初始状态
        initial_status = get_tls_status(TEST_TLS_ID)
        print(f"从缓存获取的初始状态: {initial_status.get('state')}")

        # 2. 调用修改函数
        target_index = 4
        target_state = 'G'
        target_duration = 30

        print(f"\n尝试将索引 [{target_index}] 的信号设置为 '{target_state}'，持续 {target_duration}秒...")

        modification_result = modify_tls_state_duration(
            tlsID=TEST_TLS_ID,
            state=target_state,
            duration=target_duration,
            index=target_index
        )
        print(f"修改结果: {modification_result}")

        simulation_step()
        if modification_result.get("status") == "success":
            final_status = get_tls_status(TEST_TLS_ID)
            print(f"\n修改后的缓存状态: {final_status.get('state')}")
    else:
        print(f"\n错误: 测试交通灯ID '{TEST_TLS_ID}' 不存在或未找到其冲突地图。")



    # while traci.trafficlight.getPhase(TEST_TLS_ID) != 1:
    #     traci.simulationStep()
    #
    # simulation_step()
    # print(f"当前时间: {traci.simulation.getTime()}")
    # print(get_tls_status(TEST_TLS_ID))
    #
    # modify_tls_state_duration(TEST_TLS_ID, "rrrrrrrrrrGGrrrrrrrrrrrrrrrrrrr",4)
    # simulation_step()
    # print(f"当前时间: {traci.simulation.getTime()}")
    # print(get_tls_status(TEST_TLS_ID))
    # for step in range(3):
    #     traci.simulationStep()
    #
    # restore_tls_state_to_default(TEST_TLS_ID)
    # simulation_step()
    # print(f"当前时间: {traci.simulation.getTime()}")
    # print(get_tls_status(TEST_TLS_ID))
    #
    # for step in range(3):
    #     traci.simulationStep()
    #
    # simulation_step()
    # print(f"当前时间: {traci.simulation.getTime()}")
    # print(get_tls_status(TEST_TLS_ID))



# 验证否可以第一秒设置修改的相位，第二秒设置修改后的相位要持续的时间
    # step = 0
    # for step in range(48):
    #     traci.simulationStep()
    #
    # simulation_step()
    # print(get_tls_status(TEST_TLS_ID))
    #
    # for step in range(3):
    #     traci.simulationStep()
    #
    # simulation_step()
    # print(get_tls_status(TEST_TLS_ID))
    #
    # modify_tls_state(TEST_TLS_ID, "rrG")
    # modify_tls_duration(TEST_TLS_ID, 10)
    # simulation_step()
    # print(get_tls_status(TEST_TLS_ID))
    #
    # step1 = 0
    # for step1 in range(5):
    #     traci.simulationStep()
    #
    # simulation_step()
    # print(get_tls_status(TEST_TLS_ID))
    #
    # restore_tls_state_to_default(TEST_TLS_ID)
    # simulation_step()
    # print(get_tls_status(TEST_TLS_ID))
    #
    # step2 = 0
    # for step2 in range(5):
    #     traci.simulationStep()
    #
    # simulation_step()
    # print(get_tls_status(TEST_TLS_ID))




    # conflict_data = {}
    # for laneID in traci.lane.getIDList():
    #     foes_tuple = traci.lane.getInternalFoes(laneID)
    #     num_foes = len(foes_tuple)
    #     if num_foes > 0:
    #         conflict_data[laneID] = {
    #             'count': num_foes,
    #             'foes': foes_tuple
    #         }
    #
    # max_conflict_lane_id = max(conflict_data, key=lambda k: conflict_data[k]['count'])
    # max_data = conflict_data[max_conflict_lane_id]
    #
    # print(f"拥有最多冲突的车道 ID: {max_conflict_lane_id}")
    # print(f"  - 其冲突车道数量为: {max_data['count']}")
    # print(f"  - 其冲突车道列表为: {max_data['foes']}")



# print("\n--- 测试: 推进仿真并缓存数据 ---")
    # step_result = simulation_step()
    # #print(f"仿真步推进结果: {step_result}")
    # print(f"缓存中的仿真时间: {in_memory_cache.get('time')}")
    #
    # # 3. 测试从缓存中获取车道状态
    # print(f"\n--- 测试: 从缓存获取车道 '{TEST_LANE_ID}' 的状态 ---")
    # # 确保测试的车道ID存在于缓存中
    # if TEST_LANE_ID in in_memory_cache.get('lanes', {}):
    #     lane_data = get_lane_status(TEST_LANE_ID)
    #     print(f"获取到的车道数据: {lane_data}")
    # else:
    #     print(f"错误: 车道 '{TEST_LANE_ID}' 不存在。请检查您的ID。")
    #     print(f"可用的车道ID有: {list(in_memory_cache.get('lanes', {}).keys())[:5]}")  # 打印前5个作为参考
    #
    # # 4. 测试从缓存中获取信号灯状态
    # print(f"\n--- 测试: 从缓存获取信号灯 '{TEST_TLS_ID}' 的状态 ---")
    # # 确保测试的信号灯ID存在于缓存中
    # if TEST_TLS_ID in in_memory_cache.get('trafficlights', {}):
    #     tls_data = get_tls_status(TEST_TLS_ID)
    #     print(f"获取到的信号灯数据: {tls_data}")
    # else:
    #     print(f"错误: 信号灯 '{TEST_TLS_ID}' 不存在。请检查您的ID。")
    #     print(f"可用的信号灯ID有: {list(in_memory_cache.get('trafficlights', {}).keys())}")
    #
    # # 5. 测试修改信号灯状态
    # print(f"\n--- 测试: 修改信号灯 '{TEST_TLS_ID}' 的状态 ---")
    # if TEST_TLS_ID in traci.trafficlight.getIDList():
    #     # 这个状态字符串的长度和格式必须与您信号灯的程序定义相匹配
    #     # 您可能需要根据实际情况调整 "GrGr"
    #     new_state = "Grr"
    #     print(f"尝试将状态设置为: '{new_state}'")
    #     modification_result = modify_tls_state(TEST_TLS_ID, new_state)
    #     print(f"修改结果: {modification_result}")
    # else:
    #     print(f"错误: 信号灯 '{TEST_TLS_ID}' 在仿真中不存在，无法修改。")

