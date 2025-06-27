import traci
import time

sumoBinary = "E:/sumo/sumo-1.23.1/bin/sumo"
sumoCmd = [sumoBinary, "-c", "E:/sumo/sumo-1.23.1/tools/2025-06-22-11-26-25/osm.sumocfg", "--start"]
TRACI_PORT = 8813

try:
    traci.start(sumoCmd, port=TRACI_PORT)

    # 运行 30 分钟 (1800 秒)
    start_real_time = time.monotonic()
    for step in range(3600):  # 让仿真跑一个小时 (3600步)
        traci.simulationStep()

        # 每 100 步打印一次状态
        if step % 30 == 0:
            current_sim_time = traci.simulation.getTime()
            vehicles_on_target_edge = traci.edge.getLastStepVehicleNumber("327046546")

            print(f"真实时间流逝: {time.monotonic() - start_real_time:.2f} s | "
                  f"仿真时间: {current_sim_time:.2f} s | "
                  f"目标路段车辆数: {vehicles_on_target_edge}")

except traci.TraCIException as e:
    print(f"TraCI Error: {e}")
finally:
    traci.close()
    print("Simulation closed.")