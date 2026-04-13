@echo off
REM === SW Manager Server Start Script ===
cd /d "%~dp0"

REM Stop existing server first
call server-stop.bat

echo [START] Starting SW Manager on port 9090...
start /b mvnw.cmd spring-boot:run > server.log 2>&1

REM Wait for startup
echo [START] Waiting for server to start...
set RETRY=0
:WAIT_LOOP
if %RETRY% GEQ 30 (
    echo [START] ERROR: Server did not start within 60 seconds.
    echo [START] Check server.log for details.
    exit /b 1
)
timeout /t 2 /nobreak >nul
findstr /c:"Started SwManagerApplication" server.log >nul 2>&1
if %ERRORLEVEL%==0 (
    echo [START] Server started successfully!
    echo [START] Access: http://localhost:9090
    exit /b 0
)
set /a RETRY+=1
goto WAIT_LOOP
