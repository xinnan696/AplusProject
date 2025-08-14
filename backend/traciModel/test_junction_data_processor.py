import pytest
import pandas as pd
import json
from unittest.mock import patch, mock_open
from itertools import cycle

from junction_data_processor import JunctionDataProcessor

FAKE_SQL = """
INSERT INTO junction_flow_relations VALUES
('j1', 'a', 'b', '0', 'c', 'd', '1', 'Conflicting'),
('j1', 'a', 'b', '0', 'e', 'f', '2', 'Non-Conflicting'),
('j2', 'x', 'y', '0', 'u', 'v', '1', 'Conflicting');
"""

def make_processor():
    # Create a new JunctionDataProcessor instance for each test.
    return JunctionDataProcessor("dummy.sql", {"j1": "tls1", "j2": "tls2", "j3": "tls3"})

def patch_traci_exception():
    # Patch traci.TraCIException as a custom Exception with a desc parameter.
    import types, sys
    traci = sys.modules.setdefault("traci", types.ModuleType("traci"))
    class TraCIException(Exception):
        def __init__(self, desc): super().__init__(desc)
    traci.TraCIException = TraCIException
    return TraCIException

def test_parse_sql_file_success():
    """
    Test _parse_sql_file for successful SQL parsing.
    """
    processor = make_processor()
    with patch("builtins.open", mock_open(read_data=FAKE_SQL)):
        df = processor._parse_sql_file()
        assert isinstance(df, pd.DataFrame)
        assert len(df) == 3
        assert set(df.columns) == {
            'junction_id', 'from_edge_id_1', 'to_edge_id_1', 'linkindex_1',
            'from_edge_id_2', 'to_edge_id_2', 'linkindex_2', 'relationship_type'
        }

def test_parse_sql_file_fail():
    """
    Test _parse_sql_file for file open exception (should return None).
    """
    processor = make_processor()
    with patch("builtins.open", side_effect=OSError):
        df = processor._parse_sql_file()
        assert df is None

def test_load_and_process_data():
    """
    Test load_and_process_data for normal and empty SQL file.
    """
    processor = make_processor()
    # Normal SQL content
    with patch("builtins.open", mock_open(read_data=FAKE_SQL)):
        processor.load_and_process_data()
        assert isinstance(processor.junction_relations, dict)
        assert 'j1' in processor.junction_relations
    # Empty SQL file
    processor2 = make_processor()
    with patch("builtins.open", mock_open(read_data="")):
        processor2.load_and_process_data()
        assert processor2.junction_relations == {}

def test_process_junction_data_groups():
    """
    Test _process_junction_data for correct phase group generation.
    """
    processor = make_processor()
    df = pd.DataFrame([
        {'junction_id': 'j1', 'from_edge_id_1': 'a', 'to_edge_id_1': 'b', 'linkindex_1': '0',
         'from_edge_id_2': 'c', 'to_edge_id_2': 'd', 'linkindex_2': '1', 'relationship_type': 'Conflicting'},
        {'junction_id': 'j1', 'from_edge_id_1': 'a', 'to_edge_id_1': 'b', 'linkindex_1': '0',
         'from_edge_id_2': 'e', 'to_edge_id_2': 'f', 'linkindex_2': '2', 'relationship_type': 'Non-Conflicting'},
    ])
    out = processor._process_junction_data(df)
    assert 'j1' in out
    assert 'non_conflicting_edges' in out['j1']
    assert 'conflicting_edges' in out['j1']

@pytest.mark.parametrize("mock_tls_list, has_exception", [
    (["tls1", "tls2"], False),
    ([], True)
])
def test_calculate_all_junctions_metrics(mock_tls_list, has_exception):
    """
    Test calculate_all_junctions_metrics for normal case and TLS cache exception.
    """
    processor = make_processor()
    processor.junction_relations = {
        "j1": {
            "non_conflicting_edges": [('a', 'b')],
            "conflicting_edges": [('c', 'd')],
            "non_conflicting_link_indices": [0],
            "conflicting_link_indices": [1],
        }
    }
    TraCIException = patch_traci_exception()
    with patch("traci.trafficlight.getIDList", return_value=mock_tls_list), \
         patch("traci.edge.getLastStepVehicleNumber", side_effect=cycle([2, 3])), \
         patch("traci.edge.getLastStepHaltingNumber", side_effect=cycle([1, 2])), \
         patch("traci.trafficlight.getRedYellowGreenState", return_value="GYR"), \
         patch("traci.trafficlight.getNextSwitch", return_value=20), \
         patch("traci.edge.getLastStepOccupancy", side_effect=cycle([0.7, 0.5])):
        result = processor.calculate_all_junctions_metrics(10)
        if has_exception:
            assert result == {}
        else:
            val = json.loads(result["j1"])
            assert val["junctionid"] == "j1"
            assert val["edge1_vehicle_count"] >= 0
            assert val["edge1_light_state"] in {"G", "Y", "R", "N/A"}
            assert val["edge1_congestion"] in {"Congested", "Non-Congested"}

def test_calculate_all_junctions_metrics_handles_exceptions():
    """
    Test calculate_all_junctions_metrics for TraCIException in TLS ID list fetching.
    """
    processor = make_processor()
    processor.junction_relations = {
        "j1": {
            "non_conflicting_edges": [('a', 'b')],
            "conflicting_edges": [('c', 'd')],
            "non_conflicting_link_indices": [0],
            "conflicting_link_indices": [1],
        }
    }
    TraCIException = patch_traci_exception()
    with patch("traci.trafficlight.getIDList", side_effect=lambda: (_ for _ in ()).throw(TraCIException("mock error"))):
        result = processor.calculate_all_junctions_metrics(0)
        assert result == {}

def test_calculate_all_junctions_metrics_empty_relations():
    """
    Test calculate_all_junctions_metrics for empty junction_relations (early return).
    """
    processor = make_processor()
    processor.junction_relations = {}
    result = processor.calculate_all_junctions_metrics(0)
    assert result == {}

def test_edge_methods_all_branches():
    """
    Test all exception branches for each private edge method.
    """
    processor = make_processor()
    TraCIException = patch_traci_exception()
    # Vehicle count exception branch
    with patch("traci.edge.getLastStepVehicleNumber", side_effect=TraCIException("mock error")):
        assert processor._get_edge_vehicle_count(['a']) == 0
    # Halting count exception branch
    with patch("traci.edge.getLastStepHaltingNumber", side_effect=TraCIException("mock error")):
        assert processor._get_edge_waiting_vehicle_count(['a']) == 0
    # Occupancy exception branch
    with patch("traci.edge.getLastStepOccupancy", side_effect=TraCIException("mock error")):
        assert processor._get_edge_occupancy(['a']) == 0
    # getLightStateCharacter exception branch
    with patch("traci.trafficlight.getRedYellowGreenState", side_effect=TraCIException("mock error")):
        assert processor._get_light_state_character("tls1", 0) == 'N/A'
    # Out of index branch for getLightStateCharacter
    with patch("traci.trafficlight.getRedYellowGreenState", return_value="GR"):
        assert processor._get_light_state_character("tls1", 3) == 'N/A'
    # getNextSwitch exception branch
    with patch("traci.trafficlight.getNextSwitch", side_effect=TraCIException("mock error")):
        assert processor._get_next_switch_time("tls1") == -1

def test_determine_congestion_status():
    """
    Test _determine_congestion_status for both branches (congested and non-congested).
    """
    processor = make_processor()
    assert processor._determine_congestion_status(0.7) == 'Congested'
    assert processor._determine_congestion_status(0.1) == 'Non-Congested'


