import pytest
import asyncio
import json
from unittest.mock import patch, MagicMock
from event_manager import EventManager

# If there is no traci.TraCIException, define a fake one for testing.
import types
import sys
if not hasattr(sys.modules.get("traci"), "TraCIException"):
    class FakeTraCIException(Exception):
        pass
    sys.modules["traci"].TraCIException = FakeTraCIException

@pytest.fixture
def event_manager():
    """Fixture for EventManager instance."""
    return EventManager()

@pytest.mark.asyncio
async def test_trigger_vehicle_breakdown_success(event_manager):
    """
    Test trigger_vehicle_breakdown with a valid vehicle.
    """
    with patch("traci.vehicle.getIDList", return_value=["v1"]), \
         patch("traci.vehicle.setSpeed") as mock_set_speed, \
         patch("traci.vehicle.getLaneID", return_value="lane1"), \
         patch("traci.simulation.getTime", return_value=10.0):

        result = await event_manager.trigger_vehicle_breakdown("e1", 20.0)
        assert result["success"]
        assert "vehicle_id" in result["details"]
        assert event_manager.active_events["e1"]["type"] == "vehicle_breakdown"

@pytest.mark.asyncio
async def test_trigger_vehicle_breakdown_no_vehicles(event_manager):
    """
    Test trigger_vehicle_breakdown with no vehicles.
    """
    with patch("traci.vehicle.getIDList", return_value=[]):
        result = await event_manager.trigger_vehicle_breakdown("e1", 20.0)
        assert not result["success"]

@pytest.mark.asyncio
async def test_trigger_vehicle_collision_success(event_manager):
    """
    Test trigger_vehicle_collision with valid candidate vehicles.
    """
    with patch("traci.vehicle.getIDList", return_value=["v1", "v2"]), \
         patch("traci.vehicle.getLeader", side_effect=[("v2", 30.0), None]), \
         patch("traci.vehicle.getLaneID", return_value="lane1"), \
         patch("traci.vehicle.setSpeed"), \
         patch("traci.simulation.getTime", return_value=5.0):

        result = await event_manager.trigger_vehicle_collision("e2", 30.0)
        assert result["success"]
        assert "vehicle_ids" in result["details"]
        assert event_manager.active_events["e2"]["type"] == "vehicle_collision"

@pytest.mark.asyncio
async def test_trigger_vehicle_collision_no_candidates(event_manager):
    """
    Test trigger_vehicle_collision when no candidate vehicles exist.
    """
    with patch("traci.vehicle.getIDList", return_value=[]):
        result = await event_manager.trigger_vehicle_collision("e2", 30.0)
        assert not result["success"]

@pytest.mark.asyncio
async def test_trigger_lane_closure(event_manager):
    """
    Test trigger_lane_closure with valid lane IDs.
    """
    with patch("traci.lane.setDisallowed") as mock_disallow, \
         patch("traci.simulation.getTime", return_value=50.0):

        lane_ids = ["lane1", "lane2"]
        result = await event_manager.trigger_lane_closure("e3", 15.0, lane_ids)
        assert result["success"]
        assert event_manager.active_events["e3"]["type"] == "lane_closure"

@pytest.mark.asyncio
async def test_trigger_emergency_vehicle_success(event_manager):
    """
    Test trigger_emergency_vehicle when all info is provided and vehicle is new.
    """
    with patch("traci.vehicle.getIDList", return_value=[]), \
         patch("traci.route.add"), \
         patch("traci.vehicle.add"):
        command = {
            "event_id": "e4",
            "vehicle_id": "ev1",
            "route_edges": ["e1", "e2"],
            "vehicle_type": "emergency",
            "organization": "fire",
            "junctions_on_path": ["j1", "j2"],
            "signalized_junctions": ["j2"]
        }
        result = await event_manager.trigger_emergency_vehicle(command)
        assert result["success"]
        assert "ev1" in event_manager.active_emergency_vehicles

@pytest.mark.asyncio
async def test_trigger_emergency_vehicle_fail_duplicate(event_manager):
    """
    Test trigger_emergency_vehicle with duplicate vehicle_id.
    """
    event_manager.active_emergency_vehicles["ev1"] = {}
    command = {
        "event_id": "e4",
        "vehicle_id": "ev1",
        "route_edges": ["e1", "e2"]
    }
    result = await event_manager.trigger_emergency_vehicle(command)
    assert not result["success"]

@pytest.mark.asyncio
async def test_track_active_emergency_vehicles_expire_and_cache(event_manager):
    """
    Test track_active_emergency_vehicles for normal tracking.
    """
    event_manager.active_emergency_vehicles["ev1"] = {
        "event_id": "e4",
        "route_edges": ["e1", "e2"],
        "junctions_on_path": ["j1"],
        "signalized_junctions": ["j1"]
    }

    tls_to_cache = {
        "tls1": json.dumps({"state": "GrGr", "nextSwitchTime": 20})
    }
    junction_to_tls_map = {"j1": "tls1"}

    with patch("traci.vehicle.getRoadID", return_value="e1"), \
         patch("traci.vehicle.getLaneID", return_value="e1_0"), \
         patch("traci.vehicle.getPosition", return_value=(100.0, 200.0)):

        data, keys = await event_manager.track_active_emergency_vehicles(10.0, tls_to_cache, junction_to_tls_map)
        assert "ev1" in data
        assert keys == []

@pytest.mark.asyncio
async def test_check_for_expired_events_and_cleanup(event_manager):
    """
    Test check_for_expired_events and auto cleanup.
    """
    event_manager.active_events["e5"] = {
        "type": "vehicle_breakdown",
        "start_time": 0.0,
        "duration": 5.0,
        "revert_info": {"vehicle_id": "v5"}
    }
    with patch("traci.simulation.getTime", return_value=10.0), \
         patch("traci.vehicle.remove") as mock_remove:

        await event_manager.check_for_expired_events()
        mock_remove.assert_called_once_with("v5")
        assert "e5" not in event_manager.active_events

@pytest.mark.asyncio
async def test_find_collision_candidates_too_few(event_manager):
    """
    Test _find_collision_candidates when only one vehicle exists.
    """
    with patch("traci.vehicle.getIDList", return_value=["v1"]):
        result = await event_manager._find_collision_candidates()
        assert result is None

@pytest.mark.asyncio
async def test_emergency_vehicle_route_add_exception(event_manager):
    """
    Test trigger_emergency_vehicle when traci.route.add raises TraCIException.
    """
    from traci import TraCIException
    with patch("traci.vehicle.getIDList", return_value=[]), \
         patch("traci.route.add", side_effect=TraCIException("fail")), \
         patch("traci.vehicle.add"):
        command = {
            "event_id": "e10",
            "vehicle_id": "ev_fail",
            "route_edges": ["e1"]
        }
        result = await event_manager.trigger_emergency_vehicle(command)
        assert not result["success"]

@pytest.mark.asyncio
async def test_track_ev_on_invalid_road_none(event_manager):
    """
    Test track_active_emergency_vehicles where vehicle's road is None.
    """
    event_manager.active_emergency_vehicles["ev1"] = {
        "event_id": "e9", "route_edges": ["e1"],
        "junctions_on_path": ["j1"], "signalized_junctions": ["j1"]
    }
    with patch("traci.vehicle.getRoadID", return_value=None), \
         patch("traci.vehicle.getLaneID", return_value="e1_0"):
        data, keys = await event_manager.track_active_emergency_vehicles(0.0, {}, {})
        assert data == {}

@pytest.mark.asyncio
async def test_track_ev_on_invalid_road_colon(event_manager):
    """
    Test track_active_emergency_vehicles where vehicle's road starts with ':'.
    """
    event_manager.active_emergency_vehicles["ev1"] = {
        "event_id": "e9", "route_edges": ["e1"],
        "junctions_on_path": ["j1"], "signalized_junctions": ["j1"]
    }
    with patch("traci.vehicle.getRoadID", return_value=":junction1"), \
         patch("traci.vehicle.getLaneID", return_value="e1_0"):
        data, keys = await event_manager.track_active_emergency_vehicles(0.0, {}, {})
        assert data == {}

@pytest.mark.asyncio
async def test_track_ev_vehicle_traci_exception(event_manager):
    """
    Test track_active_emergency_vehicles when traci.vehicle.getRoadID raises TraCIException.
    """
    from traci import TraCIException
    event_manager.active_emergency_vehicles["ev1"] = {
        "event_id": "e9", "route_edges": ["e1"],
        "junctions_on_path": ["j1"], "signalized_junctions": ["j1"]
    }
    with patch("traci.vehicle.getRoadID", side_effect=TraCIException("traci lost")):
        data, keys = await event_manager.track_active_emergency_vehicles(0.0, {}, {})
        assert "ev1" in keys

@pytest.mark.asyncio
async def test_track_ev_vehicle_value_error(event_manager):
    """
    Test track_active_emergency_vehicles when traci.vehicle.getRoadID raises ValueError.
    """
    event_manager.active_emergency_vehicles["ev1"] = {
        "event_id": "e9", "route_edges": ["e1"],
        "junctions_on_path": ["j1"], "signalized_junctions": ["j1"]
    }
    with patch("traci.vehicle.getRoadID", side_effect=ValueError("bad vehicle")):
        data, keys = await event_manager.track_active_emergency_vehicles(0.0, {}, {})
        assert "ev1" in keys

@pytest.mark.asyncio
async def test_clear_event_vehicle_collision(event_manager):
    """
    Test _clear_event for vehicle_collision type.
    """
    event = {
        "type": "vehicle_collision",
        "revert_info": {"vehicles": ["v1", "v2"], "lane_id": "lane1"}
    }
    with patch("traci.vehicle.getIDList", return_value=["v1", "v2"]), \
         patch("traci.vehicle.remove") as mock_remove:
        await event_manager._clear_event("e7", event)
        assert mock_remove.call_count == 2

@pytest.mark.asyncio
async def test_clear_event_lane_closure(event_manager):
    """
    Test _clear_event for lane_closure type.
    """
    event = {
        "type": "lane_closure",
        "revert_info": {"lane_ids": ["lane1", "lane2"]}
    }
    with patch("traci.lane.getIDList", return_value=["lane1", "lane2"]), \
         patch("traci.lane.setDisallowed") as mock_dis:
        await event_manager._clear_event("e8", event)
        mock_dis.assert_called()

@pytest.mark.asyncio
async def test_trigger_emergency_vehicle_missing_fields(event_manager):
    """
    Test trigger_emergency_vehicle when required fields are missing.
    """
    # Missing vehicle_id
    command = {
        "event_id": "e11",
        "route_edges": ["e1"]
    }
    result = await event_manager.trigger_emergency_vehicle(command)
    assert not result["success"]
    assert "vehicle_id" in result["details"]["message"]

    # Missing route_edges
    command = {
        "event_id": "e12",
        "vehicle_id": "v123"
    }
    result = await event_manager.trigger_emergency_vehicle(command)
    assert not result["success"]
    assert "vehicle_id" in result["details"]["message"]

@pytest.mark.asyncio
async def test_track_ev_upcoming_junction_none(event_manager):
    """
    Test track_active_emergency_vehicles when upcoming_junction_id is None.
    """
    event_manager.active_emergency_vehicles["ev_junc"] = {
        "event_id": "e13",
        "route_edges": ["e1", "e2"],
        "junctions_on_path": [],   # No junctions on path
        "signalized_junctions": []
    }
    with patch("traci.vehicle.getRoadID", return_value="e1"), \
         patch("traci.vehicle.getLaneID", return_value="e1_0"), \
         patch("traci.vehicle.getPosition", return_value=(1,2)):
        data, keys = await event_manager.track_active_emergency_vehicles(0.0, {}, {})
        ev_data = json.loads(data["ev_junc"])
        assert ev_data["currentLaneID"] is None
        assert ev_data["nextLaneID"] is None

@pytest.mark.asyncio
async def test_find_collision_candidates_no_vehicle(event_manager):
    """
    Test _find_collision_candidates when vehicle list is empty.
    """
    with patch("traci.vehicle.getIDList", return_value=[]):
        result = await event_manager._find_collision_candidates()
        assert result is None

@pytest.mark.asyncio
async def test_find_collision_candidates_one_vehicle(event_manager):
    """
    Test _find_collision_candidates when only one vehicle exists.
    """
    with patch("traci.vehicle.getIDList", return_value=["v1"]):
        result = await event_manager._find_collision_candidates()
        assert result is None

@pytest.mark.asyncio
async def test_find_collision_candidates_traci_exception(event_manager):
    """
    Test _find_collision_candidates where getLeader raises TraCIException.
    """
    from traci import TraCIException
    # At least two vehicles to enter the for loop, getLeader raises TraCIException.
    with patch("traci.vehicle.getIDList", return_value=["v1", "v2"]), \
         patch("traci.vehicle.getLeader", side_effect=TraCIException("test exc")):
        result = await event_manager._find_collision_candidates()
        assert result is None  # All caught and continue, finally return None
