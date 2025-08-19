@echo off
echo Starting UrbanFlow frontend container...

rem Stop and remove existing container
docker stop urbanflow-frontend 2>nul
docker rm urbanflow-frontend 2>nul

rem Run new container
docker run -d -p 5173:5173 --name urbanflow-frontend urbanflow-frontend:latest

if %ERRORLEVEL% EQU 0 (
    echo Container started successfully!
    echo Access URL: http://localhost:5173
    echo Check container status: docker ps
    echo Check container logs: docker logs urbanflow-frontend
) else (
    echo Container start failed!
    echo Please check error messages and retry
)

pause