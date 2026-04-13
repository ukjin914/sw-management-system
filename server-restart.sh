#!/bin/bash
# === SW Manager Server Restart (bash native) ===
# Avoids cmd.exe/PowerShell variable escaping issues entirely

PROJECT_DIR="C:/Users/PUJ/eclipse-workspace/swmanager"
LOG_FILE="$PROJECT_DIR/server.log"
PORT=9090

echo "[RESTART] Stopping existing server on port $PORT..."

# Find and kill process on port using pure bash + Windows netstat
PID=$(netstat -ano 2>/dev/null | grep ":${PORT}" | grep "LISTENING" | awk '{print $5}' | head -1)
if [ -n "$PID" ] && [ "$PID" != "0" ]; then
    echo "[RESTART] Killing PID $PID..."
    taskkill //F //PID "$PID" >/dev/null 2>&1
    sleep 2
else
    echo "[RESTART] No server running on port $PORT"
fi

# Clear old log
> "$LOG_FILE"

echo "[RESTART] Starting server..."
cd "$PROJECT_DIR"
./mvnw.cmd spring-boot:run > "$LOG_FILE" 2>&1 &
SERVER_PID=$!
echo "[RESTART] Maven PID: $SERVER_PID"

# Wait for startup
echo "[RESTART] Waiting for startup..."
for i in $(seq 1 30); do
    sleep 2
    if grep -q "Started SwManagerApplication" "$LOG_FILE" 2>/dev/null; then
        echo "[RESTART] Server started successfully! (~$((i*2)) seconds)"
        echo "[RESTART] http://localhost:$PORT"
        exit 0
    fi
    if grep -q "APPLICATION FAILED TO START" "$LOG_FILE" 2>/dev/null; then
        echo "[RESTART] ERROR: Application failed to start!"
        grep -A 20 "APPLICATION FAILED TO START" "$LOG_FILE"
        exit 1
    fi
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
        echo "[RESTART] ERROR: Process died unexpectedly"
        tail -30 "$LOG_FILE"
        exit 1
    fi
done

echo "[RESTART] TIMEOUT: Server did not start in 60s"
tail -30 "$LOG_FILE"
exit 1
