import traci
import asyncio
import random
import json
from typing import Dict, Any, List, Union


class EventManager:
    """
    一个独立的类，用于管理所有特殊事件的生命周期。
    它是有状态的，负责触发、跟踪和自动清除事件。
    """

    def __init__(self):
        # 存储所有正在进行的事件及其状态
        self.active_events: Dict[str, Dict[str, Any]] = {}
        # ### 新增部分开始 ###
        # 单独存储正在追踪的紧急车辆
        self.active_emergency_vehicles: Dict[str, Dict[str, Any]] = {}
        # ### 新增部分结束 ###
        # 异步锁，用于在多线程/异步环境中安全地修改 active_events
        self.lock = asyncio.Lock()

    async def _find_collision_candidates(self) -> Union[tuple, None]:
        """在路网中寻找一对合适的碰撞车辆（同一车道的前后车）。"""
        # 确保TraCI操作在同一个异步任务中完成
        vehicle_ids = list(traci.vehicle.getIDList())
        print(f"--- 诊断信息: vehicle_ids 的类型是 {type(vehicle_ids)} ---")
        if len(vehicle_ids) < 2:
            return None

        random.shuffle(vehicle_ids)
        for follower_id in vehicle_ids:
            try:
                leader_info = traci.vehicle.getLeader(follower_id)
                # 寻找距离较近（<50米）且在同一车道的目标
                if leader_info and leader_info[1] < 50:
                    leader_id = leader_info[0]
                    if traci.vehicle.getLaneID(follower_id) == traci.vehicle.getLaneID(leader_id):
                        return leader_id, follower_id
            except traci.TraCIException:
                # 某些车辆可能瞬间没有leader，这会抛出异常，安全地忽略它
                continue
        return None

    async def trigger_vehicle_breakdown(self, event_id: str, duration: float) -> (bool, Dict):
        """触发单车故障事件。"""
        vehicle_ids = traci.vehicle.getIDList()
        if not vehicle_ids:
            return {"success": False, "details": {"message": "Event 'vehicle_breakdown' triggered failed. No vehicles found."}}

        target_vehicle = random.choice(vehicle_ids)
        lane_id = traci.vehicle.getLaneID(target_vehicle)

        # 执行动作：停车
        traci.vehicle.setSpeed(target_vehicle, 0)

        # 异步安全地记录事件状态
        async with self.lock:
            self.active_events[event_id] = {
                "type": "vehicle_breakdown", "duration": duration,
                "start_time": traci.simulation.getTime(),
                "revert_info": {"vehicle_id": target_vehicle}
            }

        details = {"message": "Event 'vehicle_breakdown' triggered successfully.", "vehicle_id": target_vehicle,"lane_id": lane_id}
        return {"success": True, "details": details}

    async def trigger_vehicle_collision(self, event_id: str, duration: float) -> (bool, Dict):
        """触发两车相撞事件。"""
        candidates = await self._find_collision_candidates()
        if not candidates:
            return {"success": False, "details": {"message": "Event 'vehicle_collision' triggered failed. No suitable candidates found."}}

        leader_id, follower_id = candidates
        lane_id = traci.vehicle.getLaneID(leader_id)

        # 执行动作：两车停车
        traci.vehicle.setSpeed(leader_id, 0)
        traci.vehicle.setSpeed(follower_id, 0)
        # traci.lane.setDisallowed(lane_id, ["all"])

        async with self.lock:
            self.active_events[event_id] = {
                "type": "vehicle_collision", "duration": duration,
                "start_time": traci.simulation.getTime(),
                "revert_info": {"vehicles": [leader_id, follower_id], "lane_id": lane_id}
            }

        details = {"message": "Event 'vehicle_collision' triggered successfully.",
                   "vehicle_ids": [leader_id, follower_id], "lane_id": lane_id}
        return {"success": True, "details": details}

    async def trigger_lane_closure(self, event_id: str, duration: float, lane_ids: List[str]) -> (bool, Dict):
        """触发车道/道路封闭事件。"""
        for lane_id in lane_ids:
            traci.lane.setDisallowed(lane_id, ["all"])

        async with self.lock:
            self.active_events[event_id] = {
                "type": "lane_closure", "duration": duration,
                "start_time": traci.simulation.getTime(),
                "revert_info": {"lane_ids": lane_ids}
            }

        details = {"message": "Event 'lane_closure' triggered successfully.", "lane_ids": lane_ids}
        return {"success": True, "details": details}

    # ### 新增方法开始 ###
    async def trigger_emergency_vehicle(self, command: Dict) -> Dict:
        """
        触发紧急车辆事件。
        """
        event_id = command.get("event_id")
        vehicle_id = command.get("vehicle_id")
        route_edges = command.get("route_edges")

        junctions_on_path = command.get("junctions_on_path", [])
        signalized_junctions = command.get("signalized_junctions", [])

        if not all([vehicle_id, route_edges]):
            return {"success": False, "details": {"message": "紧急车辆事件缺少 vehicle_id 或 route_edges"}}

        # 检查车辆是否已存在 (注意：此函数在TRACI_LOCK内被调用)
        if vehicle_id in self.active_emergency_vehicles or vehicle_id in traci.vehicle.getIDList():
            return {"success": False, "details": {"message": f"车辆 '{vehicle_id}' 已存在或正在被追踪。"}}

        try:
            route_id = f"route_{vehicle_id}"
            traci.route.add(route_id, route_edges)
            traci.vehicle.add(
                vehID=vehicle_id,
                routeID=route_id,
                typeID=command.get("vehicle_type", "emergency")
            )
            # traci.vehicle.setColor(vehicle_id, (255, 0, 0, 255))

            # 使用锁安全地更新状态
            async with self.lock:
                self.active_emergency_vehicles[vehicle_id] = {
                    "event_id": event_id,
                    "organization": command.get("organization"),
                    "route_edges": route_edges,
                    "junctions_on_path": junctions_on_path, # 存储所有交叉口列表
                    "signalized_junctions": signalized_junctions # 存储信号灯交叉口列表
                }

            return {"success": True, "details": {"message": f"紧急车辆 {vehicle_id} 触发成功。"}}

        except traci.TraCIException as e:
            print(f"[事件管理器错误] 触发车辆 '{vehicle_id}' 失败: {e}")
            return {"success": False, "details": {"message": f"TraCI Error: {e}"}}

    async def track_active_emergency_vehicles(self, current_time: float, tls_to_cache: Dict, junction_to_tls_map: Dict) -> tuple:
        """
        在每个仿真步中追踪所有活跃的紧急车辆。
        """
        vehicles_to_cache = {}
        keys_to_delete = []

        # 直接遍历 self.active_emergency_vehicles
        # 使用 list() 来创建一个副本，以允许在循环中安全地修改原始字典
        for ev_id, ev_info in list(self.active_emergency_vehicles.items()):
            try:
                # --- 步骤 1: 照常获取所有实时数据 ---
                current_road_id = traci.vehicle.getRoadID(ev_id)
                current_lane_id = traci.vehicle.getLaneID(ev_id)  # 先获取原始数据

                if not current_road_id or current_road_id.startswith(':'):
                    continue

                route_edges = ev_info["route_edges"]
                all_junctions = ev_info["junctions_on_path"]
                signalized_junctions_set = set(ev_info["signalized_junctions"])  # 使用set以提高查找效率
                print(f"[紧急车辆事件调试日志] 正在追踪车辆 '{ev_id}'，当前所在道路: '{current_road_id}'")
                current_edge_index = route_edges.index(current_road_id)

                # --- 步骤 2: 照常计算下一路段信息和即将到达的信号灯路口 ---
                next_edge_id, next_lane_id = None, None
                if current_edge_index < len(route_edges) - 1:
                    next_edge_id = route_edges[current_edge_index + 1]
                    lane_index = current_lane_id.split('_')[-1]
                    next_lane_id = f"{next_edge_id}_{lane_index}"  # 先计算原始数据

                upcoming_junction_id = None
                if current_edge_index < len(all_junctions):
                    # 1. 直接从“所有交叉口”列表中获取即将到达的交叉口ID
                    next_junction_on_path = all_junctions[current_edge_index]

                    # 2. 判断这个交叉口是否在“信号灯交叉口”列表中
                    if next_junction_on_path in signalized_junctions_set:
                        # 如果是，则将其作为我们的目标
                        upcoming_junction_id = next_junction_on_path
                    # 3. 如果不是，upcoming_junction_id 保持为 None (null)

                print(f"[紧急车辆事件调试日志] 正在追踪车辆 '{ev_id}'，即将到达的交叉口: '{upcoming_junction_id}'")

                # --- 步骤 3: 【新增逻辑】根据 upcomingJunctionID 的值，决定是否清空车道信息 ---
                if upcoming_junction_id is None:
                    # 如果车辆不是正在接近信号灯路口，则将车道信息设为 null
                    current_lane_id = None
                    next_lane_id = None

                # --- 步骤 4: 准备数据包，其中车道信息已根据新逻辑被处理 ---
                upcoming_tls_id, upcoming_tls_state, upcoming_tls_countdown = None, None, None
                if upcoming_junction_id:
                    tls_id = junction_to_tls_map.get(upcoming_junction_id)
                    if tls_id and tls_to_cache.get(tls_id):
                        tls_info = json.loads(tls_to_cache[tls_id])
                        upcoming_tls_id = tls_id
                        upcoming_tls_state = tls_info.get("state")
                        upcoming_tls_countdown = tls_info.get("nextSwitchTime")

                position = traci.vehicle.getPosition(ev_id)

                ev_data_packet = {
                    "eventID": ev_info["event_id"],
                    "vehicleID": ev_id,
                    "currentEdgeID": current_road_id,
                    "currentLaneID": current_lane_id,
                    "upcomingJunctionID": upcoming_junction_id,
                    "nextEdgeID": next_edge_id,
                    "nextLaneID": next_lane_id,
                    "upcomingTlsID": upcoming_tls_id,
                    "upcomingTlsState": upcoming_tls_state,
                    "upcomingTlsCountdown": upcoming_tls_countdown,
                    "position": {"x": position[0], "y": position[1]},
                    "timestamp": current_time,
                }
                vehicles_to_cache[ev_id] = json.dumps(ev_data_packet)

            except (traci.TraCIException, ValueError):
                # TraCIException: 车辆离开仿真
                # ValueError: 车辆偏离路线
                print(f"[事件管理器] 车辆 '{ev_id}' 已完成其路线或偏离，追踪结束。")
                async with self.lock:
                    if ev_id in self.active_emergency_vehicles:
                        del self.active_emergency_vehicles[ev_id]
                keys_to_delete.append(ev_id)

        return vehicles_to_cache, keys_to_delete


    async def _clear_event(self, event_id: str, event_data: Dict):
        """内部使用的、根据事件类型执行不同清理操作的函数。"""
        revert_info = event_data["revert_info"]
        event_type = event_data["type"]

        if event_type == "vehicle_breakdown":
            traci.vehicle.remove(revert_info["vehicle_id"])
        elif event_type == "vehicle_collision":
            for v_id in revert_info["vehicles"]:
                if v_id in traci.vehicle.getIDList():  # 确保车辆还存在
                    traci.vehicle.remove(v_id)
        elif event_type == "lane_closure":
            for lane_id in revert_info["lane_ids"]:
                if lane_id in traci.lane.getIDList():
                    traci.lane.setDisallowed(lane_id, [])

        print(f"[Special/Emergency Event] 事件已自动清除: ID={event_id}, 类型={event_type}")

    async def check_for_expired_events(self):
        """在每个仿真步中检查并清除到期事件。"""
        current_time = traci.simulation.getTime()
        expired_events = {}

        async with self.lock:
            # 找出所有到期的事件ID
            for event_id, event_data in list(self.active_events.items()):
                if current_time >= event_data["start_time"] + event_data["duration"]:
                    expired_events[event_id] = event_data
                    del self.active_events[event_id]  # 从活动列表中移除

        # 对所有到期的事件执行清理
        for event_id, event_data in expired_events.items():
            await self._clear_event(event_id, event_data)
