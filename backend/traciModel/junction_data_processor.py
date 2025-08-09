import traci
import pandas as pd
import re
import json
from collections import defaultdict

class JunctionDataProcessor:
    def __init__(self, sql_file_path: str, junction_to_tls_map: dict):
        """
        Initialize the processor.
        :param sql_file_path: Path to the junction_flow_relations.sql file.
        :param junction_to_tls_map: Mapping from junction_id to tls_id passed from withRedis.py.
        """
        self.sql_file_path = sql_file_path
        self.junction_to_tls_map = junction_to_tls_map
        self.junction_relations = {}
        self.all_tls_ids_cache = []

    def load_and_process_data(self):
        """
        Load and process the SQL file to build junction relation data.
        This method should be called once when the service starts.
        """
        print(f"JunctionProcessor: Parsing relations from {self.sql_file_path}...")
        relations_df = self._parse_sql_file()
        if relations_df is not None and not relations_df.empty:
            self.junction_relations = self._process_junction_data(relations_df)
            print(f"[JunctionRedis] JunctionProcessor: Successfully processed data for {len(self.junction_relations)} junctions.")
        else:
            print("[JunctionRedis] JunctionProcessor: Warning - Could not parse SQL file or it was empty.")

    def _parse_sql_file(self):
        """Parse the SQL file and extract data from INSERT statements."""
        try:
            with open(self.sql_file_path, 'r', encoding='utf-8') as file:
                content = file.read()
            pattern = r"INSERT INTO junction_flow_relations.*?VALUES\s*(.*?);"
            matches = re.findall(pattern, content, re.DOTALL)
            data = []
            for match in matches:
                values_pattern = r"\('([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)'\)"
                values_matches = re.findall(values_pattern, match)
                for values in values_matches:
                    data.append({
                        'junction_id': values[0],
                        'from_edge_id_1': values[1],
                        'to_edge_id_1': values[2],
                        'linkindex_1': values[3],
                        'from_edge_id_2': values[4],
                        'to_edge_id_2': values[5],
                        'linkindex_2': values[6],
                        'relationship_type': values[7]
                    })
            return pd.DataFrame(data)
        except Exception as e:
            print(f"JunctionProcessor: Error parsing SQL file: {e}")
            return None

    def _process_junction_data(self, relations_df):
        """
        Process junction data, using a graph coloring-based "relation propagation" algorithm
        to divide traffic streams into two mutually exclusive groups.
        """
        junction_data = {}
        # Group all relations by junction_id and process each junction independently
        for junction_id, group_df in relations_df.groupby('junction_id'):
            # 1. Build adjacency list, identify all traffic streams, and record their link_index
            adj = defaultdict(list)
            all_streams = set()
            stream_to_link_index = {}
            for _, row in group_df.iterrows():
                e1p = (row['from_edge_id_1'], row['to_edge_id_1'])
                e2p = (row['from_edge_id_2'], row['to_edge_id_2'])
                all_streams.add(e1p)
                all_streams.add(e2p)
                # Record link_index
                stream_to_link_index[e1p] = int(row['linkindex_1'])
                stream_to_link_index[e2p] = int(row['linkindex_2'])
                # Add bidirectional relationship
                adj[e1p].append((e2p, row['relationship_type']))
                adj[e2p].append((e1p, row['relationship_type']))

            # 2. Graph coloring process
            colors = {}
            for stream in all_streams:
                if stream not in colors:
                    # If the current stream is not yet colored, start a new BFS
                    colors[stream] = 1  # Assign initial color 1
                    queue = [stream]
                    while queue:
                        current_stream = queue.pop(0)
                        current_color = colors[current_stream]
                        for neighbor, relationship in adj[current_stream]:
                            if neighbor not in colors:
                                if relationship == 'Non-Conflicting':
                                    colors[neighbor] = current_color
                                else:  # Conflicting
                                    colors[neighbor] = 3 - current_color  # Switch color (1 -> 2, 2 -> 1)
                                queue.append(neighbor)

            # 3. Assign streams to groups according to coloring result
            group1_streams = set()
            group2_streams = set()
            for stream, color in colors.items():
                if color == 1:
                    group1_streams.add(stream)
                else:
                    group2_streams.add(stream)

            # 4. Build output format compatible with main function
            # Define group 1 as 'non_conflicting' and group 2 as 'conflicting'
            junction_data[junction_id] = {
                'non_conflicting_edges': list(group1_streams),
                'conflicting_edges': list(group2_streams),
                'non_conflicting_link_indices': [stream_to_link_index[s] for s in group1_streams],
                'conflicting_link_indices': [stream_to_link_index[s] for s in group2_streams]
            }

        return junction_data

    def calculate_all_junctions_metrics(self, current_time: float) -> dict:
        """
        At each simulation step, calculate metrics for all defined junctions.
        :param current_time: Current simulation time.
        :return: A dictionary, where the key is junction_id and the value is a JSON string containing metrics.
        """
        if not self.junction_relations:
            return {}

        # cache all TLS IDs
        if not self.all_tls_ids_cache:
            try:
                self.all_tls_ids_cache = traci.trafficlight.getIDList()
            except traci.TraCIException as e:
                print(f"[JunctionRedis] JunctionProcessor: Failed to get TLS ID list: {e}")
                return {}

        junctions_to_cache = {}
        for junction_id, data in self.junction_relations.items():
            tlsid = self.junction_to_tls_map.get(junction_id)
            if not tlsid or tlsid not in self.all_tls_ids_cache:
                continue

            edge1_edges = list(set([edge for pair in data['non_conflicting_edges'] for edge in pair]))
            edge2_edges = list(set([edge for pair in data['conflicting_edges'] for edge in pair]))
            edge1_vehicle_count = self._get_edge_vehicle_count(edge1_edges)
            edge2_vehicle_count = self._get_edge_vehicle_count(edge2_edges)
            edge1_waiting_vehicle_count = self._get_edge_waiting_vehicle_count(edge1_edges)
            edge2_waiting_vehicle_count = self._get_edge_waiting_vehicle_count(edge2_edges)

            edge1_light_state = 'N/A'
            if data['non_conflicting_link_indices']:
                edge1_light_state = self._get_light_state_character(tlsid, data['non_conflicting_link_indices'][0])

            edge2_light_state = 'N/A'
            if data['conflicting_link_indices']:
                edge2_light_state = self._get_light_state_character(tlsid, data['conflicting_link_indices'][0])

            next_switch = self._get_next_switch_time(tlsid)
            next_switch_time = next_switch - current_time if next_switch > 0 else 0

            edge1_occupancy = self._get_edge_occupancy(edge1_edges)
            edge2_occupancy = self._get_edge_occupancy(edge2_edges)
            edge1_congestion = self._determine_congestion_status(edge1_occupancy)
            edge2_congestion = self._determine_congestion_status(edge2_occupancy)

            junction_output = {
                'junctionid': junction_id,
                'edge1_vehicle_count': edge1_vehicle_count,
                'edge2_vehicle_count': edge2_vehicle_count,
                'edge1_waiting_vehicle_count': edge1_waiting_vehicle_count,
                'edge2_waiting_vehicle_count': edge2_waiting_vehicle_count,
                'edge1_light_state': edge1_light_state,
                'edge2_light_state': edge2_light_state,
                'nextswitchtime': next_switch_time,
                'edge1_congestion': edge1_congestion,
                'edge2_congestion': edge2_congestion
            }
            junctions_to_cache[junction_id] = json.dumps(junction_output)

        return junctions_to_cache

    # --- Private helper methods ---
    def _get_edge_vehicle_count(self, edges):
        total_count = 0
        for edge in edges:
            try:
                total_count += traci.edge.getLastStepVehicleNumber(edge)
            except traci.TraCIException:
                continue
        return total_count

    def _get_edge_waiting_vehicle_count(self, edges):
        total_count = 0
        for edge in edges:
            try:
                total_count += traci.edge.getLastStepHaltingNumber(edge)
            except traci.TraCIException:
                continue
        return total_count

    def _get_edge_occupancy(self, edges):
        max_occupancy = 0
        for edge in edges:
            try:
                occupancy = traci.edge.getLastStepOccupancy(edge)
                max_occupancy = max(max_occupancy, occupancy)
            except traci.TraCIException:
                continue
        return max_occupancy

    def _get_light_state_character(self, tls_id, link_index):
        try:
            state = traci.trafficlight.getRedYellowGreenState(tls_id)
            return state[link_index] if 0 <= link_index < len(state) else 'N/A'
        except traci.TraCIException:
            return 'N/A'

    def _get_next_switch_time(self, tls_id):
        try:
            return traci.trafficlight.getNextSwitch(tls_id)
        except traci.TraCIException:
            return -1

    def _determine_congestion_status(self, occupancy):
        return 'Congested' if occupancy > 0.60 else 'Non-Congested'
