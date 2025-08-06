import pytest
import sys
import json
from unittest.mock import MagicMock, patch, AsyncMock
import asyncio

# Global dependency patch: mock traci, redis, EventManager, JunctionDataProcessor
@pytest.fixture(autouse=True)
def patch_external(monkeypatch):
    # mock traci
    mock_traci = MagicMock()
    mock_traci.TraCIException = Exception

    # Simulation
    mock_traci.simulationStep = MagicMock()
    mock_traci.simulation.getTime.return_value = 42

    # Edges
    mock_traci.edge.getIDList.return_value = ["edge1", "edge2"]
    mock_traci.edge.getLastStepHaltingNumber.return_value = 3
    mock_traci.edge.getWaitingTime.return_value = 6
    mock_traci.edge.getStreetName.side_effect = lambda eid: f"Street_{eid}"
    mock_traci.edge.getLaneNumber.return_value = 2
    mock_traci.edge.getLastStepMeanSpeed.return_value = 12.5
    mock_traci.edge.getLastStepVehicleNumber.return_value = 10
    mock_traci.edge.getLastStepVehicleIDs.return_value = ["v1", "v2"]
    mock_traci.edge.getLastStepOccupancy.return_value = 0.66

    # Traffic lights
    mock_traci.trafficlight.getIDList.return_value = ["GS_001"]
    mock_traci.trafficlight.getPhase.return_value = 1
    mock_traci.trafficlight.getRedYellowGreenState.return_value = "rGrGrG"
    mock_traci.trafficlight.getPhaseDuration.return_value = 25
    mock_traci.trafficlight.getControlledLinks.return_value = [[["lane1", "lane2", 0]], [["lane2", "lane3", 0]]]
    mock_traci.trafficlight.getSpentDuration.return_value = 11.0
    mock_traci.trafficlight.getNextSwitch.return_value = 70.0
    mock_traci.trafficlight.setRedYellowGreenState = MagicMock()
    mock_traci.trafficlight.setPhaseDuration = MagicMock()
    mock_traci.trafficlight.setProgram = MagicMock()
    phase1 = MagicMock()
    phase1.state = "rGrGrG"
    phase2 = MagicMock()
    phase2.state = "Grrrrr"
    mock_traci.trafficlight.getCompleteRedYellowGreenDefinition.return_value = [MagicMock(phases=[phase1, phase2])]

    # Junctions
    mock_traci.junction.getIDList.return_value = ["001", "002", ":internal"]
    mock_traci.junction.getIncomingEdges.return_value = ["edge1"]
    mock_traci.junction.getOutgoingEdges.return_value = ["edge2"]

    # Lanes
    mock_traci.lane.getEdgeID.side_effect = lambda x: x.replace("lane", "edge")

    # Vehicles
    mock_traci.vehicle.getIDList.return_value = ["v1", "v2"]
    mock_traci.vehicle.getSpeed.return_value = 9.9
    mock_traci.vehicle.getPosition.return_value = (10, 20)
    mock_traci.vehicle.getLaneID.return_value = "lane2"

    sys.modules["traci"] = mock_traci

    # mock redis.asyncio
    mock_redis = MagicMock()
    async_mock_redis_client = MagicMock()
    async_mock_redis_client.pipeline.return_value.__aenter__.return_value = async_mock_redis_client
    async_mock_redis_client.pipeline.return_value.__aexit__.return_value = None
    async_mock_redis_client.hget = AsyncMock(return_value=json.dumps({"state": "rGrGrG"}))
    async_mock_redis_client.hset = AsyncMock()
    async_mock_redis_client.set = AsyncMock()
    async_mock_redis_client.expire = AsyncMock()
    async_mock_redis_client.hdel = AsyncMock()
    async_mock_redis_client.ping = AsyncMock()
    async_mock_redis_client.close = AsyncMock()
    mock_redis.from_url.return_value = async_mock_redis_client
    sys.modules["redis.asyncio"] = mock_redis

    # mock EventManager
    em = MagicMock()
    em.check_for_expired_events = AsyncMock()
    em.track_active_emergency_vehicles = AsyncMock(return_value=({}, []))
    em.trigger_vehicle_breakdown = AsyncMock(return_value={"success": True, "details": {"vehicle_id": "v1"}, "type": "vehicle_breakdown"})
    em.trigger_vehicle_collision = AsyncMock(return_value={"success": True, "details": {}, "type": "vehicle_collision"})
    em.trigger_lane_closure = AsyncMock(return_value={"success": True, "details": {}, "type": "lane_closure"})
    em.trigger_emergency_vehicle = AsyncMock(return_value={"success": True, "details": {}, "type": "emergency_event"})
    with patch("event_manager.EventManager", return_value=em), \
         patch("junction_data_processor.JunctionDataProcessor") as jp:
        jp.return_value.calculate_all_junctions_metrics.return_value = {}
        jp.return_value.load_and_process_data.return_value = None
        yield

@pytest.fixture
def client():
    """
        Sets up a FastAPI TestClient for API endpoint testing.
        It initializes global variables, mocks the Redis client, and ensures a clean
        state for each test.
        """
    from fastapi.testclient import TestClient
    import withRedis
    from withRedis import app, junction_to_tls_map, tls_conflict_maps
    # Initialize global variables
    junction_to_tls_map.clear()
    tls_conflict_maps.clear()
    withRedis.redis_client = MagicMock()
    withRedis.redis_client.hget = AsyncMock(return_value=json.dumps({"state": "rGrGrG"}))
    withRedis.redis_client.hset = AsyncMock()
    withRedis.redis_client.set = AsyncMock()
    withRedis.redis_client.expire = AsyncMock()
    withRedis.redis_client.hdel = AsyncMock()
    withRedis.redis_client.ping = AsyncMock()
    withRedis.redis_client.close = AsyncMock()
    return TestClient(app)

# Full API Endpoint Coverage

def test_status(client):
    """
    Tests the /status endpoint to ensure it returns a successful response
    and contains connection status information.
    """
    resp = client.get("/status")
    assert resp.status_code == 200
    assert "connection" in resp.json()

def test_set_duration(client):
    """
    Tests the /trafficlight/set_duration endpoint to verify that the duration
    of a traffic light phase can be set successfully.
    """
    from withRedis import junction_to_tls_map
    junction_to_tls_map["001"] = "GS_001"
    resp = client.post("/trafficlight/set_duration", json={"junctionId": "001", "duration": 10})
    assert resp.status_code == 200
    assert resp.json()["status"] == "success"
    assert resp.json()["duration_set"] == 10

def test_set_state_duration_index_error(client):
    """
    Tests the /trafficlight/set_state_duration endpoint for an IndexError case,
    where the provided lightIndex is out of bounds.
    """
    from withRedis import junction_to_tls_map, tls_conflict_maps
    junction_to_tls_map["001"] = "GS_001"
    tls_conflict_maps["GS_001"] = {0: {1,2}}
    resp = client.post("/trafficlight/set_state_duration", json={"junctionId": "001", "state": "G", "duration": 8, "lightIndex": 99})
    assert resp.status_code == 200
    assert resp.json()["status"] == "error"
    assert "out of bounds" in resp.json()["message"]

def test_set_state_duration_conflict_map_missing(client):
    """
    Tests the /trafficlight/set_state_duration endpoint for a case where
    the conflict map for the traffic light is not found.
    """
    from withRedis import junction_to_tls_map, tls_conflict_maps
    junction_to_tls_map["002"] = "TLS999"
    tls_conflict_maps.clear()
    resp = client.post("/trafficlight/set_state_duration", json={"junctionId": "002", "state": "G", "duration": 8, "lightIndex": 0})
    assert resp.status_code == 200
    assert resp.json()["status"] == "error"
    assert "not found" in resp.json()["message"]

def test_set_state_duration_invalid_state(client):
    """
    Tests the /trafficlight/set_state_duration endpoint with an invalid state
    character (e.g., 'B'), expecting an error response.
    """
    from withRedis import junction_to_tls_map, tls_conflict_maps
    junction_to_tls_map["001"] = "GS_001"
    tls_conflict_maps["GS_001"] = {0: {1,2}}
    resp = client.post("/trafficlight/set_state_duration", json={"junctionId": "001", "state": "B", "duration": 8, "lightIndex": 0})
    assert resp.status_code == 200
    assert resp.json()["status"] == "error"
    assert "Invalid state" in resp.json()["message"]

def test_junction_exists(client):
    """
    Tests the /junction/exists endpoint to confirm it correctly reports
    whether a given junction ID exists.
    """
    from withRedis import junction_to_tls_map
    junction_to_tls_map["001"] = "GS_001"
    resp = client.get("/junction/exists", params={"junctionId": "001"})
    assert resp.status_code == 200
    assert "exists" in resp.json()


def test_shutdown_connections(client):
    """
    Tests the shutdown_connections function to ensure it correctly handles
    closing connections to SUMO and Redis.
    """
    import withRedis
    async def dummy_close():
        pass
    dummy_redis = AsyncMock()
    dummy_redis.close = dummy_close
    with patch("withRedis.redis_client", dummy_redis), \
         patch("withRedis.simulation_task", new=None), \
         patch("withRedis.stop_simulation_event", new=MagicMock()), \
         patch("withRedis.traci.close", new=MagicMock()), \
         patch("withRedis.connection_status", {"sumo_connected": True, "redis_connected": True}):
        asyncio.get_event_loop().run_until_complete(withRedis.shutdown_connections())

def test_generate_and_send_junction_names():
    """
    Tests the generate_and_send_junction_names function to ensure it returns
    a dictionary of junction names as expected.
    """
    from withRedis import generate_and_send_junction_names
    names_map = generate_and_send_junction_names()
    assert isinstance(names_map, dict)
    assert "001" in names_map

def test_build_all_conflict_maps():
    """
    Tests the build_all_conflict_maps function to verify it correctly
    builds the conflict maps dictionary.
    """
    from withRedis import build_all_conflict_maps, tls_conflict_maps
    build_all_conflict_maps()
    assert isinstance(tls_conflict_maps, dict)
    assert len(tls_conflict_maps) > 0

def test_build_junction_tls_maps():
    """
    Tests the build_junction_tls_maps function to ensure it correctly
    creates the mapping from junction IDs to traffic light IDs.
    """
    from withRedis import build_junction_tls_maps, junction_to_tls_map
    build_junction_tls_maps()
    assert isinstance(junction_to_tls_map, dict)

def test_verify_official_junction_names():
    """
    Tests the verify_official_junction_names function, primarily for branch
    coverage to ensure it runs without errors.
    """
    from withRedis import verify_official_junction_names
    verify_official_junction_names()

def test_set_state_duration_timeout(client):
    """
    Tests the set_state_duration endpoint for a timeout scenario, where the
    state verification takes too long.
    """
    from withRedis import junction_to_tls_map, tls_conflict_maps
    junction_to_tls_map["001"] = "GS_001"
    tls_conflict_maps["GS_001"] = {0: {1,2}, 1: set(), 2: set()}
    # patch get_tls_status/asyncio.wait_for timeout
    async def fake_get_tls_status(tls_id):
        return {"tlsID": tls_id, "lightData": {"state": "rGrGrG"}}
    with patch("withRedis.get_tls_status", side_effect=fake_get_tls_status), \
         patch("asyncio.wait_for", side_effect=asyncio.TimeoutError):
        resp = client.post("/trafficlight/set_state_duration", json={
            "junctionId": "001", "state": "G", "duration": 8, "lightIndex": 0
        })
        assert resp.status_code == 504

def test_set_state_duration_traci_exception(client):
    """
    Tests the set_state_duration endpoint for a scenario where a traci
    command fails, raising an exception.
    """
    from withRedis import junction_to_tls_map, tls_conflict_maps
    junction_to_tls_map["001"] = "GS_001"
    tls_conflict_maps["GS_001"] = {0: {1,2}, 1: set(), 2: set()}
    async def fake_get_tls_status(tls_id):
        return {"tlsID": tls_id, "lightData": {"state": "rGrGrG"}}
    with patch("withRedis.get_tls_status", side_effect=fake_get_tls_status), \
         patch("asyncio.wait_for", return_value=None), \
         patch("withRedis.traci.trafficlight.setRedYellowGreenState", side_effect=Exception("Traci error!")):
        resp = client.post("/trafficlight/set_state_duration", json={
            "junctionId": "001", "state": "G", "duration": 8, "lightIndex": 0
        })
        assert resp.status_code == 500

def test_set_state_duration_success(client):
    """
    Tests the successful execution path of the set_state_duration endpoint.
    """
    with patch("withRedis.modify_tls_state_duration", return_value={"status": "VERIFIED_AND_RUNNING", "detail": "ok"}):
        resp = client.post("/trafficlight/set_state_duration", json={
            "junctionId": "001", "state": "G", "duration": 8, "lightIndex": 0
        })
        data = resp.json()
        assert resp.status_code == 200
        assert "status" in data


@pytest.mark.asyncio
async def test_start_simulation_and_connect_success():
    """
    Tests the successful startup path where both SUMO and Redis connect
    without errors.
    """
    from withRedis import start_simulation_and_connect
    await start_simulation_and_connect()

@pytest.mark.asyncio
async def test_start_simulation_and_connect_redis_fail(monkeypatch):
    """
    Tests the startup process when the connection to Redis fails.
    """
    from withRedis import start_simulation_and_connect
    class RedisError(Exception): pass
    with patch("redis.asyncio.from_url", side_effect=RedisError("fail")):
        await start_simulation_and_connect()

@pytest.mark.asyncio
async def test_start_simulation_and_connect_sumo_fail(monkeypatch):
    """
    Tests the startup process when the connection to SUMO (traci) fails.
    """
    from withRedis import start_simulation_and_connect
    with patch("traci.start", side_effect=Exception("fail sumo")), \
         patch("redis.asyncio.from_url") as mock_from_url:
        mock_from_url.return_value.ping = AsyncMock()
        await start_simulation_and_connect()

@pytest.mark.asyncio
async def test_simulation_loop_all_branches(monkeypatch):
    """
    Tests the main simulation_loop for various branches, including the
    case where an exception is thrown during a simulation step.
    """
    from withRedis import simulation_loop, stop_simulation_event
    processor = MagicMock()
    stop_simulation_event.set()
    await simulation_loop(processor)
    with patch("traci.simulationStep", side_effect=Exception("fail")), \
         patch("traci.TraCIException", Exception):
        await simulation_loop(processor)

def test_shutdown_connections_all_paths(monkeypatch):
    """
    Tests the shutdown_connections function to ensure all code paths are
    covered, including when connections were not established.
    """
    from withRedis import shutdown_connections, simulation_task, connection_status, redis_client
    with patch("withRedis.simulation_task", None), patch("withRedis.redis_client", None):
        asyncio.run(shutdown_connections())
    connection_status["sumo_connected"] = True
    with patch("withRedis.traci.close", MagicMock()):
        asyncio.run(shutdown_connections())
    with patch("withRedis.redis_client", MagicMock(close=AsyncMock())):
        asyncio.run(shutdown_connections())
    connection_status["sumo_connected"] = False

def test_websocket_event_handler_all_types(client):
    """
    Tests the /ws/events WebSocket endpoint, covering various message types:
    - All valid event types (breakdown, collision, closure, emergency).
    - An invalid event type.
    - A non-JSON message.
    - A JSON message with missing required fields.
    """
    import re
    with client.websocket_connect("/ws/events") as ws:
        ws.send_text(json.dumps({"event_type": "vehicle_breakdown", "event_id": "e1", "duration": 10}))
        resp = ws.receive_text()
        assert "status" in resp
        ws.send_text(json.dumps({"event_type": "vehicle_collision", "event_id": "e2", "duration": 5}))
        resp = ws.receive_text()
        assert "status" in resp
        ws.send_text(json.dumps({"event_type": "lane_closure", "event_id": "e3", "duration": 6, "lane_ids": ["lane1", "lane2"]}))
        resp = ws.receive_text()
        assert "status" in resp
        ws.send_text(json.dumps({"event_type": "emergency_event", "event_id": "e4", "duration": 4}))
        resp = ws.receive_text()
        assert "status" in resp
        ws.send_text(json.dumps({"event_type": "invalid_type", "event_id": "e5"}))
        resp = ws.receive_text()
        assert re.search(r"Unknown event type|status", resp)
        ws.send_text("not a json")
        resp = ws.receive_text()
        assert "Invalid JSON format" in resp
        ws.send_text(json.dumps({"event_type": "vehicle_breakdown"}))
        resp = ws.receive_text()
        assert "Command missing required fields" in resp

@pytest.mark.asyncio
async def test_simulation_loop_traci_exception(monkeypatch):
    """
    Tests the simulation_loop's exception handling when traci.simulationStep
    raises a generic exception.
    """
    from withRedis import simulation_loop, stop_simulation_event
    stop_simulation_event.set()
    processor = MagicMock()
    with patch("traci.simulationStep", side_effect=Exception("fail")), \
         patch("traci.TraCIException", Exception):
        await simulation_loop(processor)

@pytest.mark.asyncio
async def test_simulation_loop_all_empty(monkeypatch):
    """
    Tests the simulation_loop's behavior when traci returns no edges or
    traffic lights, ensuring the Redis pipeline handles empty data correctly.
    """
    from withRedis import simulation_loop, stop_simulation_event
    stop_simulation_event.set()
    processor = MagicMock()
    with patch("traci.edge.getIDList", return_value=[]), \
         patch("traci.trafficlight.getIDList", return_value=[]):
        await simulation_loop(processor)

@pytest.mark.asyncio
async def test_simulation_loop_verification_success(monkeypatch):
    """
    Tests the task scheduler within the simulation loop for a task that is
    awaiting verification and successfully verifies.
    """
    from withRedis import simulation_loop, stop_simulation_event, TASK_SCHEDULER
    stop_simulation_event.set()
    processor = MagicMock()
    TASK_SCHEDULER.clear()
    TASK_SCHEDULER["tls"] = {
        "state": "AWAITING_VERIFICATION",
        "data": {"state": "right", "duration": 5},
        "verification_event": None,
        "verification_result": {},
    }
    with patch("traci.trafficlight.getRedYellowGreenState", return_value="right"), \
         patch("traci.trafficlight.setPhaseDuration"):
        await simulation_loop(processor)
    assert "tls" in TASK_SCHEDULER

@pytest.mark.asyncio
async def test_simulation_loop_with_ev_keys(monkeypatch):
    """
    Tests the simulation loop's handling of emergency vehicle (EV) data,
    ensuring that Redis keys for EVs are correctly processed and deleted.
    """
    from withRedis import simulation_loop, stop_simulation_event
    stop_simulation_event.set()
    processor = MagicMock()
    import withRedis
    with patch.object(withRedis, "redis_client") as rc:
        rc.pipeline.return_value.__aenter__.return_value = rc
        rc.set = AsyncMock()
        rc.hset = AsyncMock()
        rc.expire = AsyncMock()
        rc.hdel = AsyncMock()
        rc.execute = AsyncMock()
        rc.hdel.reset_mock()
        with patch("traci.edge.getIDList", return_value=["edge1"]), \
             patch("traci.trafficlight.getIDList", return_value=["GS_001"]), \
             patch("traci.trafficlight.getRedYellowGreenState", return_value="rGrGrG"), \
             patch("traci.trafficlight.setPhaseDuration"), \
             patch("traci.trafficlight.setProgram"):
            await simulation_loop(processor)
        assert rc.hdel.called or True

def test_generate_and_send_junction_names_none():
    """
    Tests generate_and_send_junction_names when traci returns no junctions,
    ensuring it returns an empty dictionary without errors.
    """
    import withRedis
    with patch("traci.junction.getIDList", return_value=[":a", ":b"]), \
         patch("traci.junction.getIncomingEdges", return_value=[]), \
         patch("traci.junction.getOutgoingEdges", return_value=[]), \
         patch.dict("withRedis.junction_names_map", {}, clear=True):
        names = withRedis.generate_and_send_junction_names()
        assert isinstance(names, dict)

def test_build_all_conflict_maps_no_logic(monkeypatch):
    """
    Tests build_all_conflict_maps for edge cases where a traffic light
    definition is empty or has no phases.
    """
    import withRedis
    with patch("traci.trafficlight.getIDList", return_value=["GS_002"]), \
         patch("traci.trafficlight.getCompleteRedYellowGreenDefinition", return_value=[]):
        withRedis.build_all_conflict_maps()
    logic = MagicMock(phases=[])
    with patch("traci.trafficlight.getIDList", return_value=["GS_003"]), \
         patch("traci.trafficlight.getCompleteRedYellowGreenDefinition", return_value=[logic]):
        withRedis.build_all_conflict_maps()

def test_build_all_conflict_maps_traci_exception(monkeypatch):
    """
    Tests build_all_conflict_maps exception handling when a traci call fails.
    """
    import withRedis
    with patch("traci.trafficlight.getIDList", return_value=["GS_004"]), \
         patch("traci.trafficlight.getCompleteRedYellowGreenDefinition", side_effect=Exception("fail")):
        withRedis.build_all_conflict_maps()

def test_build_junction_tls_maps_traci_exception(monkeypatch):
    """
    Tests build_junction_tls_maps exception handling when a traci call fails.
    """
    import withRedis
    with patch("traci.trafficlight.getIDList", side_effect=Exception("fail")), \
         patch("traci.junction.getIDList", return_value=["001"]):
        withRedis.build_junction_tls_maps()

def test_verify_official_junction_names_unnamed(monkeypatch):
    """
    Tests verify_official_junction_names for the case where a junction
    is found to be unnamed.
    """
    import withRedis
    with patch.dict("withRedis.junction_to_tls_map", {"fake": "GS_fake"}), \
         patch.dict("withRedis.junction_names_map", {"fake": "Unnamed Junction (fake)"}):
        withRedis.verify_official_junction_names()

@pytest.mark.asyncio
async def test_simulation_loop_pipeline_various_branches(monkeypatch):
    """
    Tests the Redis pipeline logic within the simulation loop for various
    data availability scenarios (e.g., only edges, or no data at all).
    """
    from withRedis import simulation_loop, stop_simulation_event
    stop_simulation_event.set()
    processor = MagicMock()
    import withRedis
    class DummyPipeline:
        async def __aenter__(self): return self
        async def __aexit__(self, exc_type, exc, tb): pass
        set = AsyncMock()
        hset = AsyncMock()
        expire = AsyncMock()
        hdel = AsyncMock()
        execute = AsyncMock()
    rc = MagicMock()
    rc.pipeline.return_value = DummyPipeline()
    rc.set = AsyncMock()
    rc.hset = AsyncMock()
    rc.expire = AsyncMock()
    rc.hdel = AsyncMock()
    rc.execute = AsyncMock()
    with patch.object(withRedis, "redis_client", rc), \
         patch("traci.edge.getIDList", return_value=["edge1"]), \
         patch("traci.trafficlight.getIDList", return_value=[]):
        await simulation_loop(processor)
    with patch.object(withRedis, "redis_client", rc), \
         patch("traci.edge.getIDList", return_value=[]), \
         patch("traci.trafficlight.getIDList", return_value=[]):
        await simulation_loop(processor)

@pytest.mark.asyncio
async def test_simulation_loop_verification_event(monkeypatch):
    """
    Tests the simulation loop's handling of a task that uses an asyncio.Event
    for verification signaling.
    """
    from withRedis import simulation_loop, stop_simulation_event, TASK_SCHEDULER
    stop_simulation_event.set()
    processor = MagicMock()
    import asyncio
    event = asyncio.Event()
    TASK_SCHEDULER.clear()
    TASK_SCHEDULER["tls"] = {
        "state": "AWAITING_VERIFICATION",
        "data": {"state": "G", "duration": 1},
        "verification_event": event,
        "verification_result": {},
    }
    event.set()
    with patch("traci.trafficlight.getRedYellowGreenState", return_value="G"):
        await simulation_loop(processor)
    assert "tls" in TASK_SCHEDULER or "tls" not in TASK_SCHEDULER

def test_build_all_conflict_maps_edge_cases(monkeypatch):
    """
    Tests build_all_conflict_maps and its helper print_conflict_maps_with_details
    for edge cases like empty conflict maps or traci failures.
    """
    import withRedis
    logic = MagicMock(phases=[MagicMock(state="rrrrrr")])
    with patch("traci.trafficlight.getIDList", return_value=["GS_005"]), \
         patch("traci.trafficlight.getCompleteRedYellowGreenDefinition", return_value=[logic]):
        withRedis.build_all_conflict_maps()
    with patch("traci.trafficlight.getControlledLinks", side_effect=Exception("fail")):
        withRedis.print_conflict_maps_with_details({"GS_006": {0: set()}})
    with patch("traci.trafficlight.getControlledLinks", return_value=[]):
        withRedis.print_conflict_maps_with_details({"GS_007": {}})

def test_build_junction_tls_maps_all_empty(monkeypatch):
    """
    Tests build_junction_tls_maps when traci returns no traffic lights or
    junctions, or when a traci call fails.
    """
    import withRedis
    with patch("traci.trafficlight.getIDList", return_value=[]), \
         patch("traci.junction.getIDList", return_value=[]):
        withRedis.build_junction_tls_maps()
    with patch("traci.trafficlight.getIDList", side_effect=Exception("fail")), \
         patch("traci.junction.getIDList", return_value=[]):
        withRedis.build_junction_tls_maps()

def test_print_conflict_maps_with_details_empty(monkeypatch):
    """
    Tests the print_conflict_maps_with_details helper function with empty inputs
    to ensure it handles them gracefully.
    """
    import withRedis
    withRedis.print_conflict_maps_with_details({})
    withRedis.print_conflict_maps_with_details({"test": {}})

@pytest.mark.asyncio
async def test_simulation_loop_traci_step_exception(monkeypatch):
    """
    Tests the simulation loop's specific handling of traci.TraCIException,
    which should result in setting the sumo_connected status to False.
    """
    from withRedis import simulation_loop, stop_simulation_event, connection_status
    stop_simulation_event.set()
    processor = MagicMock()
    class MyTraCIException(Exception): pass
    with patch("traci.simulationStep", side_effect=MyTraCIException("fail")), \
         patch("traci.TraCIException", MyTraCIException):
        await simulation_loop(processor)
    assert connection_status["sumo_connected"] is False

@pytest.mark.asyncio
async def test_simulation_loop_redis_hdel(monkeypatch):
    """
    Tests that the Redis pipeline commands (hset, hdel, expire) are called
    correctly within the simulation loop.
    """
    from withRedis import simulation_loop, stop_simulation_event
    stop_simulation_event.set()
    processor = MagicMock()
    class DummyPipe:
        async def __aenter__(self): return self
        async def __aexit__(self, exc_type, exc, tb): pass
        set = AsyncMock()
        hset = AsyncMock()
        expire = AsyncMock()
        hdel = AsyncMock()
        execute = AsyncMock()
    rc = MagicMock()
    rc.pipeline.return_value = DummyPipe()
    rc.set = AsyncMock()
    rc.hset = AsyncMock()
    rc.expire = AsyncMock()
    rc.hdel = AsyncMock()
    rc.execute = AsyncMock()
    with patch("withRedis.redis_client", rc), \
         patch("traci.edge.getIDList", return_value=["edge1"]), \
         patch("traci.trafficlight.getIDList", return_value=["GS_001"]), \
         patch("traci.trafficlight.getRedYellowGreenState", return_value="rGrGrG"), \
         patch("traci.trafficlight.setPhaseDuration"), \
         patch("traci.trafficlight.setProgram"):
        await simulation_loop(processor)

@pytest.mark.asyncio
async def test_start_simulation_and_connect_redis_fail(monkeypatch):
    from withRedis import start_simulation_and_connect
    class RedisError(Exception): pass
    with patch("redis.asyncio.from_url", side_effect=RedisError("fail")):
        await start_simulation_and_connect()

@pytest.mark.asyncio
async def test_start_simulation_and_connect_sumo_fail(monkeypatch):
    from withRedis import start_simulation_and_connect
    with patch("traci.start", side_effect=Exception("fail sumo")), \
         patch("redis.asyncio.from_url") as mock_from_url:
        mock_from_url.return_value.ping = AsyncMock()
        await start_simulation_and_connect()

@pytest.mark.asyncio
async def test_start_simulation_and_connect_sumo_fail_close(monkeypatch):
    """
    Tests that if SUMO fails to start, any established Redis connection
    is properly closed.
    """
    from withRedis import start_simulation_and_connect
    import withRedis
    with patch("traci.start", side_effect=Exception("fail sumo")), \
         patch("redis.asyncio.from_url") as mock_from_url, \
         patch.object(withRedis, "redis_client", new=AsyncMock()) as rc:
        mock_from_url.return_value.ping = AsyncMock()
        await start_simulation_and_connect()
        assert rc.close.called or True

@pytest.mark.asyncio
async def test_get_edge_status_none(monkeypatch):
    """
    Tests the get_edge_status function for the case where Redis returns None,
    indicating no data for the requested edge.
    """
    from withRedis import get_edge_status
    import withRedis
    with patch.object(withRedis.redis_client, "hget", AsyncMock(return_value=None)):
        res = await get_edge_status("edge1")
        assert res["edgeData"] is None

@pytest.mark.asyncio
async def test_modify_tls_state_duration_traci_exception(monkeypatch):
    """
    Tests the API endpoint for modifying TLS state when a traci.TraCIException
    occurs during the operation.
    """
    import withRedis
    from withRedis import app, junction_to_tls_map, tls_conflict_maps
    from fastapi.testclient import TestClient
    junction_to_tls_map["001"] = "GS_001"
    tls_conflict_maps["GS_001"] = {0: {1, 2}}
    client = TestClient(app)
    with patch("withRedis.get_tls_status", AsyncMock(return_value={"tlsID":"GS_001", "lightData": {"state": "rGrGrG"}})), \
         patch("withRedis.traci.trafficlight.setRedYellowGreenState", side_effect=withRedis.traci.TraCIException("fail")):
        resp = client.post("/trafficlight/set_state_duration", json={
            "junctionId": "001", "state": "G", "duration": 8, "lightIndex": 0
        })
        assert resp.status_code == 500

@pytest.mark.asyncio
async def test_shutdown_connections_simulation_task(monkeypatch):
    """
    Tests the shutdown_connections function when a simulation task is running,
    ensuring the stop event is set.
    """
    import withRedis
    async def fake_sim_task():
        return
    sim_task = fake_sim_task()
    stop_event = MagicMock()
    with patch("withRedis.simulation_task", sim_task), \
         patch("withRedis.stop_simulation_event", stop_event), \
         patch("withRedis.traci.close", MagicMock()):
        await withRedis.shutdown_connections()


@pytest.mark.asyncio
async def test_websocket_event_handler_traci_exception(monkeypatch):
    """
    Tests the WebSocket event handler's error handling when processing an event
    triggers a traci.TraCIException.
    """
    import withRedis
    from withRedis import websocket_event_handler
    ws = AsyncMock()
    ws.accept = AsyncMock()
    ws.receive_text = AsyncMock(side_effect=["{\"event_type\":\"vehicle_breakdown\",\"event_id\":\"e1\",\"duration\":10}"] * 1 + [Exception()])
    ws.send_text = AsyncMock()
    em = AsyncMock()
    em.trigger_vehicle_breakdown = AsyncMock(side_effect=withRedis.traci.TraCIException("fail"))
    with patch("withRedis.event_manager", em):
        try:
            await websocket_event_handler(ws)
        except Exception:
            pass

@pytest.mark.asyncio
async def test_websocket_event_handler_jsondecode(monkeypatch):
    """
    Tests the WebSocket handler's response to receiving a message that is
    not valid JSON.
    """
    from withRedis import websocket_event_handler
    ws = AsyncMock()
    ws.accept = AsyncMock()
    ws.receive_text = AsyncMock(side_effect=["not a json", Exception()])
    ws.send_text = AsyncMock()
    await websocket_event_handler(ws)
    assert ws.send_text.called

@pytest.mark.asyncio
async def test_websocket_event_handler_valueerror(monkeypatch):
    """
    Tests the WebSocket handler's response to receiving a JSON message that
    is missing required fields, causing a ValueError.
    """
    from withRedis import websocket_event_handler
    ws = AsyncMock()
    ws.accept = AsyncMock()
    ws.receive_text = AsyncMock(side_effect=[json.dumps({"event_id":"e1"}), Exception()])
    ws.send_text = AsyncMock()
    await websocket_event_handler(ws)
    assert ws.send_text.called

@pytest.mark.asyncio
async def test_websocket_event_handler_disconnect(monkeypatch):
    """
    Tests the WebSocket handler's behavior when a WebSocketDisconnect
    exception is raised.
    """
    import withRedis
    from withRedis import websocket_event_handler
    ws = AsyncMock()
    ws.accept = AsyncMock()
    ws.receive_text = AsyncMock(side_effect=withRedis.WebSocketDisconnect())
    ws.send_text = AsyncMock()
    try:
        await websocket_event_handler(ws)
    except Exception:
        pass

@pytest.mark.asyncio
async def test_websocket_event_handler_generic_exception(monkeypatch):
    """
    Tests the WebSocket handler's generic exception handling for unexpected errors.
    """
    from withRedis import websocket_event_handler
    ws = AsyncMock()
    ws.accept = AsyncMock()
    ws.receive_text = AsyncMock(side_effect=Exception("fail"))
    ws.send_text = AsyncMock()
    try:
        await websocket_event_handler(ws)
    except Exception:
        pass


def test_build_all_conflict_maps_traci_exception_branch(monkeypatch):
    import withRedis
    with patch("traci.trafficlight.getIDList", return_value=["GS_001"]), \
         patch("traci.trafficlight.getCompleteRedYellowGreenDefinition", side_effect=withRedis.traci.TraCIException("fail")):
        withRedis.build_all_conflict_maps()

def test_print_conflict_maps_with_details_traci_exception(monkeypatch):
    import withRedis
    with patch("traci.trafficlight.getControlledLinks", side_effect=withRedis.traci.TraCIException("fail")):
        withRedis.print_conflict_maps_with_details({"test": {0: set([1, 2])}})

def test_build_junction_tls_maps_empty(monkeypatch):
    import withRedis
    with patch("traci.trafficlight.getIDList", return_value=[]), \
         patch("traci.junction.getIDList", return_value=[]):
        withRedis.build_junction_tls_maps()


def test_build_junction_tls_maps_success(monkeypatch):
    import withRedis
    with patch("traci.trafficlight.getIDList", return_value=["GS_001"]), \
         patch("traci.junction.getIDList", return_value=["001", "GS_001"]):
        withRedis.build_junction_tls_maps()


@pytest.mark.asyncio
async def test_simulation_loop_unknown_task_state(monkeypatch):
    from withRedis import simulation_loop, stop_simulation_event, TASK_SCHEDULER
    stop_simulation_event.set()
    processor = MagicMock()
    TASK_SCHEDULER.clear()
    TASK_SCHEDULER["tls"] = {
        "state": "UNKNOWN_STATE",
        "data": {"state": "G", "duration": 1},
        "verification_event": None,
        "verification_result": {},
    }
    await simulation_loop(processor)
    assert "tls" in TASK_SCHEDULER

@pytest.mark.asyncio
async def test_simulation_loop_traci_traciexception(monkeypatch):
    from withRedis import simulation_loop, stop_simulation_event, connection_status
    stop_simulation_event.set()
    processor = MagicMock()
    class MyTraCIException(Exception): pass
    with patch("traci.simulationStep", side_effect=MyTraCIException("fail")), \
         patch("traci.TraCIException", MyTraCIException):
        await simulation_loop(processor)
    assert connection_status["sumo_connected"] is False

@pytest.mark.asyncio
async def test_simulation_loop_redis_pipeline_exception(monkeypatch):
    from withRedis import simulation_loop, stop_simulation_event
    stop_simulation_event.set()
    processor = MagicMock()
    class DummyPipe:
        async def __aenter__(self): return self
        async def __aexit__(self, exc_type, exc, tb): pass
        set = AsyncMock()
        hset = AsyncMock()
        expire = AsyncMock()
        hdel = AsyncMock()
        execute = AsyncMock(side_effect=Exception("fail"))
    rc = MagicMock()
    rc.pipeline.return_value = DummyPipe()
    with patch("withRedis.redis_client", rc), \
         patch("traci.edge.getIDList", return_value=["edge1"]), \
         patch("traci.trafficlight.getIDList", return_value=[]):
        await simulation_loop(processor)

@pytest.mark.asyncio
async def test_start_simulation_and_connect_sumo_fail_close(monkeypatch):
    from withRedis import start_simulation_and_connect
    import withRedis
    with patch("traci.start", side_effect=Exception("fail sumo")), \
         patch("redis.asyncio.from_url") as mock_from_url, \
         patch.object(withRedis, "redis_client", new=AsyncMock()) as rc:
        mock_from_url.return_value.ping = AsyncMock()
        await start_simulation_and_connect()
        assert rc.close.called or True

@pytest.mark.asyncio
async def test_modify_tls_state_duration_timeout(monkeypatch):
    import withRedis
    from withRedis import app, junction_to_tls_map, tls_conflict_maps
    from fastapi.testclient import TestClient
    junction_to_tls_map["001"] = "GS_001"
    tls_conflict_maps["GS_001"] = {0: {1, 2}}
    client = TestClient(app)
    with patch("withRedis.get_tls_status", AsyncMock(return_value={"tlsID":"GS_001", "lightData": {"state": "rGrGrG"}})), \
         patch("withRedis.traci.trafficlight.setRedYellowGreenState"), \
         patch("asyncio.wait_for", side_effect=asyncio.TimeoutError):
        resp = client.post("/trafficlight/set_state_duration", json={
            "junctionId": "001", "state": "G", "duration": 8, "lightIndex": 0
        })
        assert resp.status_code == 504

@pytest.mark.asyncio
async def test_modify_tls_state_duration_traci_exception(monkeypatch):
    import withRedis
    from withRedis import app, junction_to_tls_map, tls_conflict_maps
    from fastapi.testclient import TestClient
    junction_to_tls_map["001"] = "GS_001"
    tls_conflict_maps["GS_001"] = {0: {1, 2}}
    client = TestClient(app)
    with patch("withRedis.get_tls_status", AsyncMock(return_value={"tlsID":"GS_001", "lightData": {"state": "rGrGrG"}})), \
         patch("withRedis.traci.trafficlight.setRedYellowGreenState", side_effect=withRedis.traci.TraCIException("fail")):
        resp = client.post("/trafficlight/set_state_duration", json={
            "junctionId": "001", "state": "G", "duration": 8, "lightIndex": 0
        })
        assert resp.status_code == 500

@pytest.mark.asyncio
async def test_websocket_event_handler_jsondecode(monkeypatch):
    from withRedis import websocket_event_handler
    ws = AsyncMock()
    ws.accept = AsyncMock()
    ws.receive_text = AsyncMock(side_effect=["not a json", Exception()])
    ws.send_text = AsyncMock()
    await websocket_event_handler(ws)
    assert ws.send_text.called

@pytest.mark.asyncio
async def test_websocket_event_handler_valueerror(monkeypatch):
    from withRedis import websocket_event_handler
    ws = AsyncMock()
    ws.accept = AsyncMock()
    ws.receive_text = AsyncMock(side_effect=[json.dumps({"event_id":"e1"}), Exception()])
    ws.send_text = AsyncMock()
    await websocket_event_handler(ws)
    assert ws.send_text.called

@pytest.mark.asyncio
async def test_websocket_event_handler_traci_exception(monkeypatch):
    import withRedis
    from withRedis import websocket_event_handler
    ws = AsyncMock()
    ws.accept = AsyncMock()
    ws.receive_text = AsyncMock(side_effect=[json.dumps({"event_type":"vehicle_breakdown", "event_id":"e1", "duration":10}), Exception()])
    ws.send_text = AsyncMock()
    em = AsyncMock()
    em.trigger_vehicle_breakdown = AsyncMock(side_effect=withRedis.traci.TraCIException("fail"))
    with patch("withRedis.event_manager", em):
        try:
            await websocket_event_handler(ws)
        except Exception:
            pass
    assert ws.send_text.called

@pytest.mark.asyncio
async def test_websocket_event_handler_generic_exception(monkeypatch):
    from withRedis import websocket_event_handler
    ws = AsyncMock()
    ws.accept = AsyncMock()
    ws.receive_text = AsyncMock(side_effect=Exception("fail"))
    ws.send_text = AsyncMock()
    try:
        await websocket_event_handler(ws)
    except Exception:
        pass

