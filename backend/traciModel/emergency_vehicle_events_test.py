import asyncio
import json
import redis
import websockets
import time
from typing import Dict, Any

# --- 配置部分 ---

# 1. Traci模块的WebSocket服务器地址
TRACI_WEBSOCKET_URL = "ws://localhost:8000/ws/events"

# 2. Redis服务器配置
REDIS_CONFIG = {
    'host': 'localhost',
    'port': 6379,
    'db': 0,
    'decode_responses': True  # 自动将Redis的响应从bytes解码为str
}

# 3. 紧急车辆事件数据 (直接在此处配置)
#    数据结构与数据库表单一一对应
EMERGENCY_EVENT_CONFIG: Dict[str, Any] = {
    "event_id": "emergency_event_1",
    "event_type": "emergency_event",  # 在发送时会被 "emergency_vehicle" 覆盖
    "vehicle_id": "e1",
    "vehicle_type": "emergency",
    "organization": "Dublin Emergency Services",
    "trigger_time": 3,
    "start_edge_id": "327046540",
    "end_edge_id": "33924202",
    "route_edges": [
        "327046540", "1314474849", "143299839", "592071140#1", "25094842",
    "327046546", "1314507320", "1314507321#1", "25094845#1", "25094845#3",
    "25094845#4", "25094845#5", "25094845#6", "25094845#7", "25094845#8",
    "25094845#9", "1314507326", "1314507325#1", "38023762#1", "38023762#2",
    "38023762#3", "38023762#4", "38023762#5", "1314507334#1", "405573977#0",
    "405573977#1", "405573977#2", "405573977#3", "750298029#1", "750298029#3",
    "750298029#4", "750298029#5", "750298029#7", "33924202"
    ],
    "signalized_junctions": [
        "cluster_1420096799_47154676", "cluster_5282839762_5528420376",
        "181747232", "47217361", "cluster_2455590523_389713_47212064", "cluster_181310323_5380872437"
    ],
    "event_status": "pending"  # 初始状态为“待处理”
}


# --- 主逻辑 ---

async def main():
    """主执行函数"""
    print("--- 后端模拟器已启动 (无数据库模式) ---")

    # 初始化Redis连接
    try:
        redis_client = redis.Redis(**REDIS_CONFIG)
        redis_client.ping()  # 检查连接是否成功
        print("[连接] 成功连接到 Redis。")
    except redis.exceptions.ConnectionError as e:
        print(f"[连接] 连接 Redis 失败: {e}")
        return

    triggered_vehicle_id = None  # 用于存储已触发事件的车辆ID

    try:
        while True:
            # --- 阶段一: 检查并触发事件 ---
            if not triggered_vehicle_id and EMERGENCY_EVENT_CONFIG['event_status'] == 'pending':
                sim_time_str = redis_client.get("sumo:simulation_time")
                if not sim_time_str:
                    print("[Redis] 等待仿真时间数据...", end='\r')
                    await asyncio.sleep(1)
                    continue

                current_sim_time = float(sim_time_str)
                event = EMERGENCY_EVENT_CONFIG

                print(f"[状态] 仿真时间: {current_sim_time:.2f} | "
                      f"待处理事件: '{event['event_id']}' (触发时间: {event['trigger_time']})", end='\r')

                if current_sim_time >= event['trigger_time']:
                    print(f"\n[触发] 条件满足！准备触发事件 '{event['event_id']}'...")

                    command = {
                        "event_type": "emergency_event",
                        "event_id": event['event_id'], "vehicle_id": event['vehicle_id'],
                        "vehicle_type": event['vehicle_type'], "agency": event['organization'],
                        "route_edges": event['route_edges'], "junctions_on_path": event['signalized_junctions']
                    }

                    # 带重试逻辑的WebSocket连接
                    websocket = None
                    for attempt in range(5):
                        try:
                            websocket = await websockets.connect(TRACI_WEBSOCKET_URL, open_timeout=100)
                            print(f"[WebSocket] 连接成功 (尝试第 {attempt + 1} 次)。")
                            break
                        except Exception as e:
                            print(f"[WebSocket] 连接失败 (尝试第 {attempt + 1} 次)。将在1秒后重试...")
                            await asyncio.sleep(1)

                    if not websocket:
                        print("[错误] 无法连接到Traci模块，请检查服务是否运行。")
                        await asyncio.sleep(10)
                        continue

                    try:
                        await websocket.send(json.dumps(command))
                        print(f"[WebSocket] 指令已发送。")

                        response_str = await websocket.recv()
                        response = json.loads(response_str)
                        print(f"[WebSocket] 收到回执: {response}")

                        if response.get("status") == "success":
                            EMERGENCY_EVENT_CONFIG['event_status'] = 'triggered'
                            triggered_vehicle_id = event['vehicle_id']
                            print(f"[内存] 事件状态已更新为 'triggered'。")
                            print(f"\n--- 事件触发成功，进入实时追踪阶段 (车辆ID: {triggered_vehicle_id}) ---")
                        else:
                            EMERGENCY_EVENT_CONFIG['event_status'] = 'failed'
                            print(f"[错误] 事件触发失败: {response.get('message', '无详细信息')}。")
                            await asyncio.sleep(10)
                    finally:
                        await websocket.close()

            # --- 阶段二: 实时追踪车辆 ---
            elif triggered_vehicle_id:
                tracking_active = True
                tracking_start_time = time.time()
                TRACKING_TIMEOUT_SECONDS = 300  # 5分钟后超时

                while tracking_active:
                    if time.time() - tracking_start_time > TRACKING_TIMEOUT_SECONDS:
                        print("\n[追踪] 追踪超时，程序退出。")
                        break  # 直接跳出追踪循环

                    try:
                        vehicle_data_str = redis_client.hget("sumo:emergency_vehicles", triggered_vehicle_id)

                        if vehicle_data_str:
                            # 如果找到数据，打印详细信息
                            vehicle_data = json.loads(vehicle_data_str)
                            pos = vehicle_data.get('position', {})
                            print(f"[追踪] 车辆 {triggered_vehicle_id} | "
                                  f"当前时间: {vehicle_data.get('timestamp', 0):.2f} | "
                                  f"当前道路: {vehicle_data.get('currentEdgeID', 'N/A'):<15} | "
                                  f"坐标: (x={pos.get('x', 0):.2f}, y={pos.get('y', 0):.2f})", end='\r')
                        else:
                            # 如果没找到数据，打印等待信息
                            print(f"[追踪] 等待车辆 '{triggered_vehicle_id}' 的数据出现在Redis中...", end='\r')

                    except redis.RedisError as e:
                        print(f"\n[Redis] 追踪时读取数据失败: {e}")

                    # 每次循环都暂停一下，避免CPU占用过高
                    await asyncio.sleep(0.5)

                    # 检查车辆是否已消失 (在循环的最后检查，逻辑更清晰)
                    if not redis_client.hexists("sumo:emergency_vehicles", triggered_vehicle_id):
                        # 为了防止第一次查询时的时序问题，我们再确认一次
                        await asyncio.sleep(1)  # 等待1秒，给withRedis写入的最后机会
                        if not redis_client.hexists("sumo:emergency_vehicles", triggered_vehicle_id):
                            print(f"\n[追踪] 车辆 '{triggered_vehicle_id}' 的追踪数据已确认从Redis消失。")
                            tracking_active = False  # 设置标志，结束追踪

                    # 当追踪结束后 (tracking_active变为False)
                print("\n--- 模拟器完成本次任务，程序将退出 ---")
                break  # 退出主 while 循环

            else:
                await asyncio.sleep(2)

    except KeyboardInterrupt:
        print("\n--- 收到退出信号 ---")
    finally:
        print("--- 后端模拟器已停止 ---")


if __name__ == "__main__":
    asyncio.run(main())
