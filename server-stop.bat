@echo off
REM === SW Manager Server Stop Script ===
echo [STOP] Finding process on port 9090...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :9090 ^| findstr LISTENING') do (
    echo [STOP] Killing PID %%a ...
    taskkill /F /PID %%a >nul 2>&1
)
echo [STOP] Server stopped.
