import traci
import pandas as pd
import re
import json
from collections import defaultdict


# junction_data_processor.py

class JunctionDataProcessor:
    def __init__(self, sql_file_path: str, junction_to_tls_map: dict):
        """
        初始化处理器。
        :param sql_file_path: junction_flow_relations.sql 文件的路径。
        :param junction_to_tls_map: 从 withRedis.py 传入的 junction_id -> tls_id 映射。
        """
        self.sql_file_path = sql_file_path
        self.junction_to_tls_map = junction_to_tls_map
        self.junction_relations = {}
        self.all_tls_ids_cache = []

    def load_and_process_data(self):
        """
        加载并处理SQL文件，构建交叉口关系数据。
        这个方法应该在服务启动时被调用一次。
        """
        print(f"JunctionProcessor: Parsing relations from {self.sql_file_path}...")
        relations_df = self._parse_sql_file()
        if relations_df is not None and not relations_df.empty:
            self.junction_relations = self._process_junction_data(relations_df)
            print(f"[JunctionRedis] JunctionProcessor: Successfully processed data for {len(self.junction_relations)} junctions.")
        else:
            print("[JunctionRedis] JunctionProcessor: Warning - Could not parse SQL file or it was empty.")

    def _parse_sql_file(self):
        """[私有] 解析SQL文件内容，提取INSERT语句中的数据"""
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
            print(f"JunctionProcessor: 解析SQL文件错误: {e}")
            return None

    def _process_junction_data(self, relations_df):
        """
        【已更新为最终逻辑】
        [私有] 处理交叉口数据，使用基于图着色的“关系传播”算法将车流划分到两个互斥的组。
        """
        junction_data = {}

        # 按 junction_id 对所有关系进行分组，独立处理每个交叉口
        for junction_id, group_df in relations_df.groupby('junction_id'):

            # 1. 构建邻接表、识别所有车流并记录其link_index
            adj = defaultdict(list)
            all_streams = set()
            stream_to_link_index = {}  # 记录每个车流的link_index

            for _, row in group_df.iterrows():
                e1p = (row['from_edge_id_1'], row['to_edge_id_1'])
                e2p = (row['from_edge_id_2'], row['to_edge_id_2'])
                all_streams.add(e1p)
                all_streams.add(e2p)

                # 记录link_index
                stream_to_link_index[e1p] = int(row['linkindex_1'])
                stream_to_link_index[e2p] = int(row['linkindex_2'])

                # 添加双向关系
                adj[e1p].append((e2p, row['relationship_type']))
                adj[e2p].append((e1p, row['relationship_type']))

            # 2. 图着色过程
            colors = {}  # 存储每个节点的颜色（组号）

            for stream in all_streams:
                if stream not in colors:
                    # 如果当前车流未被着色，开始新一轮的BFS
                    colors[stream] = 1  # 给予初始颜色 1
                    queue = [stream]

                    while queue:
                        current_stream = queue.pop(0)
                        current_color = colors[current_stream]

                        for neighbor, relationship in adj[current_stream]:
                            if neighbor not in colors:
                                if relationship == 'Non-Conflicting':
                                    colors[neighbor] = current_color
                                else:  # Conflicting
                                    colors[neighbor] = 3 - current_color  # 切换颜色 (1 -> 2, 2 -> 1)
                                queue.append(neighbor)

            # 3. 根据着色结果分配到最终的组
            group1_streams = set()
            group2_streams = set()
            for stream, color in colors.items():
                if color == 1:
                    group1_streams.add(stream)
                else:
                    group2_streams.add(stream)

            # 4. 构建与主函数兼容的输出格式
            # 我们将组1定义为 'non_conflicting'，组2定义为 'conflicting'
            # 这个命名只是为了与旧代码兼容，实质上它们是两个互斥的相位组
            junction_data[junction_id] = {
                'non_conflicting_edges': list(group1_streams),
                'conflicting_edges': list(group2_streams),
                'non_conflicting_link_indices': [stream_to_link_index[s] for s in group1_streams],
                'conflicting_link_indices': [stream_to_link_index[s] for s in group2_streams]
            }

        return junction_data

    def calculate_all_junctions_metrics(self, current_time: float) -> dict:
        """
        在每个仿真步长，计算所有已定义交叉口的指标。
        :param current_time: 当前仿真时间。
        :return: 一个字典，键是junction_id，值是包含指标的JSON字符串。
        """
        if not self.junction_relations:
            return {}

        # 第一次调用时，缓存所有信号灯ID
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

    # --- 私有辅助方法 ---
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
