import requests
import time
from urllib.parse import quote

BASE_URL = "http://localhost:8000"
WEBHOOK_RECEIVER_URL = "http://localhost:9000/test-webhook-endpoint"

# !!! 请确保这个ID在您的SUMO路网中真实存在 !!!
TEST_TLS_ID = "joinedS_12136692966_5056283101_5101079521_5101079523_#3more"
# 使用 urllib.parse.quote 对ID进行编码，特别是处理'#'字符
ENCODED_TLS_ID = quote(TEST_TLS_ID)

# 一个正确的相位状态
CORRECT_STATE = "rrrrrrrrrrGGrrrrrrrrrrrrrrrrrrr"
# 一个故意设置的错误相位状态，用于测试失败场景
WRONG_STATE = "r"

def test_status():
    print("\n--- 1. 测试 /status 接口 ---")
    try:
        response = requests.get(f"{BASE_URL}/status")
        response.raise_for_status()
        print("状态检查成功:", response.json())
    except requests.RequestException as e:
        print("状态检查失败:", e)


def test_simple_duration_change():
    print(f"\n--- 2. 测试模式一: 为 {TEST_TLS_ID} 设置时长 ---")
    try:
        # 注意：这里我们使用编码后的ID来构建URL
        url = f"{BASE_URL}/trafficlight/{ENCODED_TLS_ID}/set_duration"
        print(f"  > 正在请求URL: {url}")

        payload = {"duration": 10}
        response = requests.post(url, json=payload)
        response.raise_for_status()
        print(f" 设置时长成功: {response.json()}")
    except requests.RequestException as e:
        print(f" 设置时长失败: {e.response.text if e.response else e}")


def test_complex_task_success():
    print(f"\n--- 3. 测试模式二 (成功场景): 为 {TEST_TLS_ID} 创建复杂任务 ---")
    try:
        # 注意：这里我们使用编码后的ID来构建URL
        url = f"{BASE_URL}/trafficlight/{ENCODED_TLS_ID}/set_state_duration"
        print(f"  > 正在请求URL: {url}")

        payload = {
            "state": CORRECT_STATE,
            "duration": 15,
            "webhookUrl": WEBHOOK_RECEIVER_URL
        }
        response = requests.post(url, json=payload)
        response.raise_for_status()
        print(f" 复杂任务提交成功: {response.json()}")
        print("  > 请观察 Webhook 接收器的终端输出，应在几秒内收到 'VERIFIED_AND_RUNNING' 通知。")
    except requests.RequestException as e:
        print(f" 复杂任务提交失败: {e.response.text if e.response else e}")


def test_complex_task_failure():
    print(f"\n--- 4. 测试模式二 (失败场景): 为 {TEST_TLS_ID} 创建一个会验证失败的任务 ---")
    try:
        # 注意：这里我们使用编码后的ID来构建URL
        url = f"{BASE_URL}/trafficlight/{ENCODED_TLS_ID}/set_state_duration"
        print(f"  > 正在请求URL: {url}")

        payload = {
            "state": WRONG_STATE,
            "duration": 20,
            "webhookUrl": WEBHOOK_RECEIVER_URL
        }
        response = requests.post(url, json=payload)
        response.raise_for_status()
        print(f" 失败任务提交成功: {response.json()}")
        print("  > 请观察 Webhook 接收器的终端输出，应在几秒内收到 'FAILED_VERIFICATION' 通知。")
    except requests.RequestException as e:
        print(f" 失败任务提交失败: {e.response.text if e.response else e}")


def test_junction_existence():
    """
    测试 /junction/{junction_id}/exists 接口。
    """
    print("\n--- 5. 测试 /junction/{junction_id}/exists 接口 ---")

    # --- 测试一个存在的交叉口 ---
    # 我们假设交通信号灯的ID与其控制的交叉口ID相同
    existing_junction_id = "30846825"
    url = f"{BASE_URL}/junction/{existing_junction_id}/exists"
    print(f" > 检查存在的交叉口 '{existing_junction_id}'")
    print(f" > 正在请求URL: {url}")
    try:
        response = requests.get(url)
        response.raise_for_status()
        data = response.json()
        print(f" > 收到响应: {data}")
        if data.get("exists") is True:
            print(" 测试成功: 接口正确返回 'exists' 为 true。")
        else:
            print(f"测试失败: 接口未返回 'exists' 为 true。响应: {data}")
    except requests.RequestException as e:
        print(f"请求失败: {e}")

if __name__ == "__main__":
    # 等待服务启动
    print("等待5秒让TraCI服务和SUMO完全启动...")
    time.sleep(5)

    test_status()
    time.sleep(2)

    test_junction_existence()
