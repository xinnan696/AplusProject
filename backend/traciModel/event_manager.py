import traci
import asyncio
import random
from typing import Dict, Any, List, Union


class EventManager:
    """
    一个独立的类，用于管理所有特殊事件的生命周期。
    它是有状态的，负责触发、跟踪和自动清除事件。
    """

    def __init__(self):
        # 存储所有正在进行的事件及其状态
        self.active_events: Dict[str, Dict[str, Any]] = {}
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

        print(f"[Special Event] 事件已自动清除: ID={event_id}, 类型={event_type}")

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
