@echo off
REM === SW Manager Server Restart Script ===
cd /d "%~dp0"
echo [RESTART] Restarting SW Manager...
call server-start.bat
