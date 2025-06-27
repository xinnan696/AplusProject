# test_redis.py
import asyncio
import redis.asyncio as redis

REDIS_HOST = "localhost"
REDIS_PORT = 6379

async def main():
    print("--- 最小化Redis连接测试 ---")
    print(f"正在尝试连接到 Redis at {REDIS_HOST}:{REDIS_PORT}...")
    try:
        # 创建客户端，并设置一个5秒的连接超时，避免无限等待
        redis_client = redis.Redis(
            host=REDIS_HOST,
            port=REDIS_PORT,
            db=0,
            socket_connect_timeout=5
        )

        # ping() 是最简单的连接测试命令
        response = await redis_client.ping()

        if response:
            print("\n✅ 成功：已成功连接到Redis并收到响应。")
        else:
            print("\n❌ 失败：连接成功，但ping命令没有返回成功响应。")

    except asyncio.TimeoutError:
        print(f"\n❌ 失败：连接到 {REDIS_HOST}:{REDIS_PORT} 的尝试在5秒后超时。")
    except redis.exceptions.ConnectionError as e:
        print(f"\n❌ 失败：发生了连接错误: {e}")
    except Exception as e:
        print(f"\n❌ 失败：发生了未知错误: {e}")

    finally:
        if 'redis_client' in locals() and redis_client:
            await redis_client.close()
        print("--- 测试结束 ---")

if __name__ == "__main__":
    asyncio.run(main())