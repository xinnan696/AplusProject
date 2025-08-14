@echo off
echo Starting UrbanFlow frontend Docker build...

cd /d "C:\Users\x2357\Desktop\AplusProjectTest\frontend\urbanflow_frontend"

echo Building Docker image...
docker build -t urbanflow-frontend:latest .

if %ERRORLEVEL% EQU 0 (
    echo Docker image build successful!
    echo Run the following command to start container:
    echo docker run -d -p 5173:5173 --name urbanflow-frontend urbanflow-frontend:latest
    echo Access URL: http://localhost:5173
) else (
    echo Docker image build failed!
    echo Please check error messages and retry
)

pause