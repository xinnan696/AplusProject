import traci
import time

SUMO_HOST = "127.0.0.1"
TRACI_PORT = 8813

try:
    print(f"正在尝试连接到 SUMO at {SUMO_HOST}:{TRACI_PORT}...")
    traci.connect(host=SUMO_HOST, port=TRACI_PORT)
    print("成功连接到 SUMO！")

    # 运行一步仿真
    traci.simulationStep()
    current_time = traci.simulation.getTime()
    print(f"仿真成功前进一步，当前仿真时间: {current_time}")

except traci.TraCIException as e:
    print(f"连接或仿真步骤失败: {e}")

