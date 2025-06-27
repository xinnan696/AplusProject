import uvicorn
from fastapi import FastAPI, Request

app = FastAPI()

@app.post("/test-webhook-endpoint")
async def receive_webhook(request: Request):
    """
    这个端点用来接收并打印来自TraCI服务的回调通知。
    """
    payload = await request.json()
    print("--- Webhook 收到通知 ---")
    print(f"任务ID: {payload.get('taskId')}")
    print(f"状态: {payload.get('status')}")
    if payload.get('error'):
        print(f"错误信息: {payload.get('error')}")
    print("--------------------------\n")
    return {"status": "webhook received"}

if __name__ == "__main__":
    # 在不同的端口运行，例如 9000
    uvicorn.run(app, host="0.0.0.0", port=9000)