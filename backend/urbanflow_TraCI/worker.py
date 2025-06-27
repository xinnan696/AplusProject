# worker.py
import time
import json
import redis  # 工作进程使用同步的redis库


def caching_worker(queue, redis_config, traci_port):
    """这个函数在独立的进程中运行"""
    import traci

    print("[Worker] Caching worker process started.")

    # 工作进程连接自己的Redis客户端
    r = redis.Redis(
        host=redis_config.get('host'),
        port=redis_config.get('port'),
        db=redis_config.get('db'),
        decode_responses=True
    )

    # 工作进程作为第二个客户端连接到SUMO
    print("[Worker] Connecting to TraCI server...")
    try:
        traci.connect(port=traci_port, numRetries=10, label="caching_worker_client")
        traci.setOrder(1)
        print("[Worker] Successfully connected to TraCI.")
    except traci.TraCIException as e:
        print(f"[Worker] FATAL: Could not connect to TraCI: {e}")
        return

    # Redis键名常量
    KEY_EDGE_PREFIX = "sumo:edge:"
    KEY_TLS_PREFIX = "sumo:tls:"
    # Redis中缓存数据的过期时间（秒）
    REDIS_EXPIRATION_SECONDS = 60

    while True:
        try:
            # 从队列中等待消息，如果队列为空则会阻塞
            message = queue.get()

            # 收到None作为信号，优雅退出
            if message is None:
                print("[Worker] Shutdown signal received. Exiting.")
                break

            current_sim_time = message
            start_time = time.time()
            print(f"[Worker] Received task for simulation time {message}. Starting caching...")

            # --- 开始执行耗时的缓存任务 ---

            # 使用同步的pipeline
            with r.pipeline(transaction=False) as pipe:
                # 缓存所有道路和信号灯状态
                for edgeID in traci.edge.getIDList():
                    edge_data = {
                        "edgeID": edgeID,
                        "timestamp": current_sim_time,
                        "edgeName": traci.edge.getStreetName(edgeID),
                        "laneNumber": traci.edge.getLaneNumber(edgeID),
                        "speed": traci.edge.getLastStepMeanSpeed(edgeID),
                        'vehicleCount': traci.edge.getLastStepVehicleNumber(edgeID),
                        'vehicleIDs': traci.edge.getLastStepVehicleIDs(edgeID),
                        'waitTime': traci.edge.getWaitingTime(edgeID),
                        'waitingVehicleIDs': traci.edge.getPendingVehicles(edgeID),
                        "waitingVehicleCount": traci.edge.getLastStepHaltingNumber(edgeID)
                    }
                    pipe.set(f"{KEY_EDGE_PREFIX}{edgeID}", json.dumps(edge_data), ex=REDIS_EXPIRATION_SECONDS)

                for tlsID in traci.trafficlight.getIDList():
                    tls_data = {
                        "tlsID": tlsID,
                        "timestamp": current_sim_time,
                        "junction_id": tlsID,
                        "phase": traci.trafficlight.getPhase(tlsID),
                        "state": traci.trafficlight.getRedYellowGreenState(tlsID),
                        "duration": traci.trafficlight.getPhaseDuration(tlsID),
                        "connection": traci.trafficlight.getControlledLinks(tlsID),
                        "spendTime": traci.trafficlight.getSpentDuration(tlsID),
                        "nextSwitchTime": traci.trafficlight.getNextSwitch(tlsID)
                    }
                    pipe.set(f"{KEY_TLS_PREFIX}{tlsID}", json.dumps(tls_data), ex=REDIS_EXPIRATION_SECONDS)

                pipe.execute()

            end_time = time.time()
            print(f"[Worker] Caching for time {message} completed in {end_time - start_time:.2f} seconds.")

        except traci.TraCIException as e:
            print(f"[Worker] TraCI connection lost. Worker is shutting down. Error: {e}")
            break
        except Exception as e:
            print(f"[Worker] An unexpected error occurred: {e}")
            time.sleep(1)

    traci.close()
    print("[Worker] Caching worker process finished.")