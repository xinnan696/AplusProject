import traci
import asyncio
import random
import json
from typing import Dict, Any, List, Union


class EventManager:
    """
    An independent class for managing the lifecycle of all special events.
    It is stateful, responsible for triggering, tracking, and automatically clearing events.
    """

    def __init__(self):
        # Store all ongoing events and their states
        self.active_events: Dict[str, Dict[str, Any]] = {}
        # Store currently tracked emergency vehicles separately
        self.active_emergency_vehicles: Dict[str, Dict[str, Any]] = {}
        # Async lock for safely modifying active_events in multi-threaded/async environments
        self.lock = asyncio.Lock()

    async def _find_collision_candidates(self) -> Union[tuple, None]:
        """Find a suitable pair of vehicles in the network (leader and follower on the same lane) for a collision."""
        # Ensure all TraCI operations are completed within the same async task
        vehicle_ids = list(traci.vehicle.getIDList())
        print(f"--- Debug info: vehicle_ids type is {type(vehicle_ids)} ---")
        if len(vehicle_ids) < 2:
            return None

        random.shuffle(vehicle_ids)
        for follower_id in vehicle_ids:
            try:
                leader_info = traci.vehicle.getLeader(follower_id)
                # Look for a leader within 50 meters on the same lane
                if leader_info and leader_info[1] < 50:
                    leader_id = leader_info[0]
                    if traci.vehicle.getLaneID(follower_id) == traci.vehicle.getLaneID(leader_id):
                        return leader_id, follower_id
            except traci.TraCIException:
                # Some vehicles may not have a leader momentarily, ignore safely
                continue
        return None

    async def trigger_vehicle_breakdown(self, event_id: str, duration: float) -> (bool, Dict):
        """Trigger a single vehicle breakdown event."""
        vehicle_ids = traci.vehicle.getIDList()
        if not vehicle_ids:
            return {"success": False, "details": {"message": "Event 'vehicle_breakdown' triggered failed. No vehicles found."}}

        target_vehicle = random.choice(vehicle_ids)
        lane_id = traci.vehicle.getLaneID(target_vehicle)

        # stop the vehicle
        traci.vehicle.setSpeed(target_vehicle, 0)

        # Record event state safely with async lock
        async with self.lock:
            self.active_events[event_id] = {
                "type": "vehicle_breakdown", "duration": duration,
                "start_time": traci.simulation.getTime(),
                "revert_info": {"vehicle_id": target_vehicle}
            }

        details = {"message": "Event 'vehicle_breakdown' triggered successfully.", "vehicle_id": target_vehicle,"lane_id": lane_id}
        return {"success": True, "details": details}

    async def trigger_vehicle_collision(self, event_id: str, duration: float) -> (bool, Dict):
        """Trigger a two-vehicle collision event."""
        candidates = await self._find_collision_candidates()
        if not candidates:
            return {"success": False, "details": {"message": "Event 'vehicle_collision' triggered failed. No suitable candidates found."}}

        leader_id, follower_id = candidates
        lane_id = traci.vehicle.getLaneID(leader_id)

        # stop both vehicles
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
        """Trigger a lane/road closure event."""
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

    async def trigger_emergency_vehicle(self, command: Dict) -> Dict:
        """
        Trigger an emergency vehicle event.
        """
        event_id = command.get("event_id")
        vehicle_id = command.get("vehicle_id")
        route_edges = command.get("route_edges")

        junctions_on_path = command.get("junctions_on_path", [])
        signalized_junctions = command.get("signalized_junctions", [])

        if not all([vehicle_id, route_edges]):
            return {"success": False, "details": {"message": "Emergency vehicle event missing vehicle_id or route_edges"}}

        # Check whether the vehicle already exists
        if vehicle_id in self.active_emergency_vehicles or vehicle_id in traci.vehicle.getIDList():
            return {"success": False, "details": {"message": f"Vehicle '{vehicle_id}' already exists or is being tracked."}}

        try:
            route_id = f"route_{vehicle_id}"
            traci.route.add(route_id, route_edges)
            traci.vehicle.add(
                vehID=vehicle_id,
                routeID=route_id,
                typeID=command.get("vehicle_type", "emergency")
            )
            # traci.vehicle.setColor(vehicle_id, (255, 0, 0, 255))

            async with self.lock:
                self.active_emergency_vehicles[vehicle_id] = {
                    "event_id": event_id,
                    "organization": command.get("organization"),
                    "route_edges": route_edges,
                    "junctions_on_path": junctions_on_path, # Store all junctions on the path
                    "signalized_junctions": signalized_junctions # Store signalized junctions
                }

            return {"success": True, "details": {"message": f"Emergency vehicle {vehicle_id} event triggered successfully."}}

        except traci.TraCIException as e:
            print(f"[EventManager Error] Failed to trigger vehicle '{vehicle_id}': {e}")
            return {"success": False, "details": {"message": f"TraCI Error: {e}"}}

    async def track_active_emergency_vehicles(self, current_time: float, tls_to_cache: Dict, junction_to_tls_map: Dict) -> tuple:
        """
        Track all active emergency vehicles at each simulation step.
        """
        vehicles_to_cache = {}
        keys_to_delete = []

        # Directly iterate over self.active_emergency_vehicles
        # Use list() to make a copy for safe modification during iteration
        for ev_id, ev_info in list(self.active_emergency_vehicles.items()):
            try:
                # Step 1: Obtain all real-time data as usual
                current_road_id = traci.vehicle.getRoadID(ev_id)
                current_lane_id = traci.vehicle.getLaneID(ev_id)

                if not current_road_id or current_road_id.startswith(':'):
                    continue

                route_edges = ev_info["route_edges"]
                all_junctions = ev_info["junctions_on_path"]
                signalized_junctions_set = set(ev_info["signalized_junctions"])  # 使用set以提高查找效率
                print(f"[EmergencyVehicle Debug] Tracking vehicle '{ev_id}', current road: '{current_road_id}'")
                current_edge_index = route_edges.index(current_road_id)

                # Step 2: Calculate next edge info and the upcoming signalized junction
                next_edge_id, next_lane_id = None, None
                if current_edge_index < len(route_edges) - 1:
                    next_edge_id = route_edges[current_edge_index + 1]
                    lane_index = current_lane_id.split('_')[-1]
                    next_lane_id = f"{next_edge_id}_{lane_index}"

                upcoming_junction_id = None
                if current_edge_index < len(all_junctions):
                    # 1. Get the next junction ID from the list of all junctions
                    next_junction_on_path = all_junctions[current_edge_index]
                    # 2. Check if this junction is in the signalized junctions list
                    if next_junction_on_path in signalized_junctions_set:
                        # If so, treat it as our target
                        upcoming_junction_id = next_junction_on_path
                    # 3. Otherwise, keep upcoming_junction_id as None (null)

                print(f"[EmergencyVehicle Debug] Tracking vehicle '{ev_id}', upcoming junction: '{upcoming_junction_id}'")

                # Step 3: Based on the value of upcomingJunctionID, decide whether to clear lane info
                if upcoming_junction_id is None:
                    # If the vehicle is not approaching a signalized junction, set lane info to null
                    current_lane_id = None
                    next_lane_id = None

                # Step 4: Prepare data packet
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
                # TraCIException: vehicle left simulation
                # ValueError: vehicle deviated from route
                print(f"[EventManager] Vehicle '{ev_id}' has completed its route or deviated, tracking ended.")
                async with self.lock:
                    if ev_id in self.active_emergency_vehicles:
                        del self.active_emergency_vehicles[ev_id]
                keys_to_delete.append(ev_id)

        return vehicles_to_cache, keys_to_delete


    async def _clear_event(self, event_id: str, event_data: Dict):
        """Internal function to clear events based on their type."""
        revert_info = event_data["revert_info"]
        event_type = event_data["type"]

        if event_type == "vehicle_breakdown":
            traci.vehicle.remove(revert_info["vehicle_id"])
        elif event_type == "vehicle_collision":
            for v_id in revert_info["vehicles"]:
                if v_id in traci.vehicle.getIDList():  # Ensure vehicle still exists
                    traci.vehicle.remove(v_id)
        elif event_type == "lane_closure":
            for lane_id in revert_info["lane_ids"]:
                if lane_id in traci.lane.getIDList():
                    traci.lane.setDisallowed(lane_id, [])

        print(f"[Special/Emergency Event] Event auto-cleared: ID={event_id}, Type={event_type}")

    async def check_for_expired_events(self):
        """Check and clear expired events at each simulation step."""
        current_time = traci.simulation.getTime()
        expired_events = {}

        async with self.lock:
            # Find all expired event IDs
            for event_id, event_data in list(self.active_events.items()):
                if current_time >= event_data["start_time"] + event_data["duration"]:
                    expired_events[event_id] = event_data
                    del self.active_events[event_id]  # Remove from active list

        # Clear all expired events
        for event_id, event_data in expired_events.items():
            await self._clear_event(event_id, event_data)
