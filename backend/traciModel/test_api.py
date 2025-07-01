import requests
import json
import time

# --- 测试配置 ---
# 请将此地址修改为您 FastAPI 应用的实际运行地址
BASE_URL = "http://127.0.0.1:8000"

# 请根据您的 SUMO 场景，提供一个真实存在的交通灯ID (Junction ID)
TEST_JUNCTION_ID = "181747232"  # !! 重要: 请替换为您的测试数据 !!


# 一个确定不存在的ID，用于测试错误处理
NON_EXISTENT_ID = "non_existent_id_12345"


def print_response(test_name, response):
    """一个辅助函数，用于格式化打印响应信息"""
    print(f"--- {test_name} ---")
    try:
        print(f"请求 URL: {response.url}")
        print(f"状态码: {response.status_code}")
        # 尝试以JSON格式打印响应体，如果失败则打印原始文本
        print(f"响应内容: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
    except json.JSONDecodeError:
        print(f"响应内容 (非JSON): {response.text}")
    except Exception as e:
        print(f"处理响应时发生错误: {e}")
    print("-" * (len(test_name) + 8) + "\n")


def test_get_status():
    """测试 /status 接口，检查服务是否就绪"""
    test_name = "1. 测试服务状态 (/status)"
    try:
        response = requests.get(f"{BASE_URL}/status")
        print_response(test_name, response)
        # 断言：检查状态码是否为200 (OK)
        assert response.status_code == 200
        data = response.json()
        # 断言：检查关键字段是否存在
        assert "sumo_connected" in data['connection']
        assert "redis_connected" in data['connection']
        print(f"{test_name}: 成功\n")
    except requests.exceptions.RequestException as e:
        print(f"{test_name}: 请求失败 - {e}\n")


def test_check_junction_exists():
    """测试 /junction/exists 接口，检查存在的和不存在的交叉口"""
    # Case 1: 测试一个存在的Junction
    test_name_1 = "2.1. 测试交叉口是否存在 (存在的ID)"
    try:
        # 使用 GET 请求，并通过 params 参数传递查询字符串
        params = {"junctionId": TEST_JUNCTION_ID}
        response = requests.get(f"{BASE_URL}/junction/exists", params=params)
        print_response(test_name_1, response)
        assert response.status_code == 200
        assert response.json().get("exists") is True
        print(f"{test_name_1}: 成功\n")
    except requests.exceptions.RequestException as e:
        print(f"{test_name_1}: 请求失败 - {e}\n")

    # Case 2: 测试一个不存在的Junction
    test_name_2 = "2.2. 测试交叉口是否存在 (不存在的ID)"
    try:
        params = {"junction_id": NON_EXISTENT_ID}
        response = requests.get(f"{BASE_URL}/junction/exists", params=params)
        print_response(test_name_2, response)
        assert response.status_code == 200
        assert response.json().get("exists") is False
        print(f"{test_name_2}: 成功\n")
    except requests.exceptions.RequestException as e:
        print(f"{test_name_2}: 请求失败 - {e}\n")


def test_set_duration():
    """测试 /trafficlight/set_duration 接口"""
    test_name = "3. 测试设置交通灯相位时长 (/trafficlight/set_duration)"
    try:
        payload = {
            "junctionId": TEST_JUNCTION_ID,
            "duration": 3  # 设置一个新的时长
        }
        response = requests.post(f"{BASE_URL}/trafficlight/set_duration", json=payload)
        print_response(test_name, response)
        assert response.status_code == 200
        assert response.json().get("status") == "success"
        print(f"{test_name}: 成功\n")
    except requests.exceptions.RequestException as e:
        print(f"{test_name}: 请求失败 - {e}\n")


def test_set_state_duration():
    """测试 /trafficlight/set_state_duration 接口，这是一个复杂场景"""
    test_name = "4. 测试设置交通灯状态和时长 (/trafficlight/set_state_duration)"
    print("--- 4. 测试设置交通灯状态和时长 (/trafficlight/set_state_duration) ---")
    print("注意: 此接口涉及后台验证，请求可能会等待几秒钟。")
    try:
        payload = {
            "junctionId": TEST_JUNCTION_ID,
            "state": "g",  # 尝试将第一个信号灯设置为绿色
            "duration": 30,
            "lightIndex": 1  # 修改第0个信号灯
        }
        # 设置一个超时时间，因为这个请求需要等待后端验证
        response = requests.post(f"{BASE_URL}/trafficlight/set_state_duration", json=payload, timeout=1000)
        print_response(test_name, response)

        # 此接口成功时，状态码依然是200
        assert response.status_code == 200
        # 成功的响应体应包含 'VERIFIED_AND_RUNNING'
        assert response.json().get("status") == "VERIFIED_AND_RUNNING"
        print(f"✅ {test_name}: 成功\n")

    except requests.exceptions.Timeout:
        print(f"{test_name}: 请求超时。后端可能未能及时验证状态变更。\n")
    except requests.exceptions.RequestException as e:
        print(f"{test_name}: 请求失败 - {e}\n")
    except AssertionError:
        print(f"{test_name}: 断言失败。响应内容不符合预期，可能是验证失败。请检查服务端的日志。\n")




if __name__ == "__main__":
    print("======== 开始 API 接口测试 ========\n")

    # 首先检查服务是否在线
    #test_get_status()

    #等待一秒，确保仿真已经运行了一会儿
    print("等待2秒...\n")
    time.sleep(2)

    # 执行其他测试
    #test_check_junction_exists()
    #
    # print("等待1秒...\n")
    # time.sleep(2)
    #
    test_set_duration()

    # print("等待1秒...\n")
    # time.sleep(2)
    #
    # test_set_state_duration()


    print("======== API 接口测试结束 ========")