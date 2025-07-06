# test_client.py

import asyncio
import websockets
import json
import uuid

# TraCI模块的WebSocket地址
SERVER_URI = "ws://localhost:8000/ws/events"


async def run_test_scenario():
    """
    连接到TraCI模块，并按顺序执行一系列测试事件。
    """
    # 使用 async with 语句可以确保连接在结束后被正确关闭
    async with websockets.connect(SERVER_URI, open_timeout=100) as websocket:
        print(f"成功连接到TraCI模块: {SERVER_URI}")
        print("-" * 40)

        # --- 测试场景 1: 触发单车故障 ---
        print(">>> 场景1: 触发单车故障，持续4秒...")
        breakdown_payload = {
            "event_type": "vehicle_breakdown",
            "event_id": "test_vehicle_breakdown_1",
            "duration": 4
        }
        await websocket.send(json.dumps(breakdown_payload))
        response = await websocket.recv()
        print(f"<<< 收到响应: {json.loads(response)}")
        print("-" * 40)

        # 等待一段时间，让仿真继续进行
        await asyncio.sleep(10)

        # --- 测试场景 2: 触发两车相撞 ---
        print(">>> 场景2: 触发两车相撞，持续5秒...")
        collision_payload = {
            "event_type": "vehicle_collision",
            "event_id": f"test_vehicle_collision_1",
            "duration": 5
        }
        await websocket.send(json.dumps(collision_payload))
        response = await websocket.recv()
        print(f"<<< 收到响应: {json.loads(response)}")
        print("-" * 40)

        await asyncio.sleep(10)

        # --- 测试场景 3: 触发车道封闭 ---
        # 请确保 lane_ids 是您路网中真实存在的
        # 您可以从SUMO-GUI中点击车道查看其ID
        target_lanes = ["405573977#0_0", "405573977#0_1"]  # 示例ID，请替换为您自己的
        print(f">>> 场景3: 封闭车道 {target_lanes}，持续5秒...")
        closure_payload = {
            "event_type": "lane_closure",
            "event_id": f"test_lane_closure_1",
            "duration": 5,
            "lane_ids": target_lanes
        }
        await websocket.send(json.dumps(closure_payload))
        response = await websocket.recv()
        print(f"<<< 收到响应: {json.loads(response)}")
        print("-" * 40)

        # --- 观察自动清除 ---
        print(">>> 所有事件已触发，现在等待30秒，观察事件是否会自动清除...")
        print(">>> 请重点观察SUMO-GUI和TraCI模块的控制台输出。")
        await asyncio.sleep(30)

        print("测试结束。")


if __name__ == "__main__":
    try:
        asyncio.run(run_test_scenario())
    except ConnectionRefusedError:
        print("\n[错误] 连接被拒绝。请确保您的TraCI模块 (withRedis.py) 正在运行。")
    except Exception as e:
        print(f"\n发生未知错误: {e}")