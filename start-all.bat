@echo off
setlocal
title Core Platform - Start All Services
color 0A

echo ============================================================
echo   Core Platform Engine - Full Stack Startup
echo ============================================================
echo.
echo   Startup sequence:
echo     1. Docker (Redis)
echo     2. PostgreSQL          (port 5432)
echo     3. Keycloak            (port 9000)
echo     4. Users Service       (port 9020)
echo     5. Orders Service      (port 9030)
echo     6. BFF Gateway         (port 9010)
echo     7. Frontend            (port 3000)
echo.
echo ============================================================

REM ── Configuration ──────────────────────────────────────────
set "ROOT=%~dp0"
set "BACKEND=%ROOT%backend"
set "FRONTEND=%ROOT%frontend"
set "INFRA=%ROOT%infrastructure"

REM ── 1. Docker Infrastructure (Redis on port 6380) ─────────
echo.
echo [1/7] Starting Docker containers (Redis)...
cd /d "%INFRA%"
docker compose up -d
if %errorlevel% neq 0 (
    echo [ERROR] Docker compose failed. Is Docker Desktop running?
    pause
    exit /b 1
)
echo [OK] Redis started on port 6380.

REM ── Wait for Redis health ─────────────────────────────────
echo       Waiting for Redis to be ready...
:wait_redis
docker exec core-platform-redis-authz redis-cli ping >nul 2>&1
if %errorlevel% neq 0 (
    timeout /t 2 /nobreak >nul
    goto wait_redis
)
echo [OK] Redis is healthy.

REM ── 2. PostgreSQL ─────────────────────────────────────────
echo.
echo [2/7] Checking PostgreSQL (port 5432)...
powershell -Command "try { $c = New-Object System.Net.Sockets.TcpClient('localhost',5432); $c.Close(); exit 0 } catch { exit 1 }" >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARN] PostgreSQL not detected on port 5432.
    echo        Please start PostgreSQL manually and press any key to continue...
    pause >nul
) else (
    echo [OK] PostgreSQL is running on port 5432.
)

REM ── 3. Keycloak ───────────────────────────────────────────
echo.
echo [3/7] Checking Keycloak (port 9000)...
powershell -Command "try { $c = New-Object System.Net.Sockets.TcpClient('localhost',9000); $c.Close(); exit 0 } catch { exit 1 }" >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARN] Keycloak not detected on port 9000.
    echo        Please start Keycloak manually and press any key to continue...
    pause >nul
) else (
    echo [OK] Keycloak is running on port 9000.
)

REM ── 4. Users Service (port 9020) ──────────────────────────
echo.
echo [4/7] Starting Users Service (port 9020)...
cd /d "%BACKEND%"
start "Users Service (9020)" cmd /k "title Users Service (9020) && cd /d %BACKEND% && gradlew.bat :services:users-service:bootRun"

echo       Waiting for Users Service to be ready...
:wait_users
timeout /t 3 /nobreak >nul
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:9020/actuator/health' -UseBasicParsing -TimeoutSec 2; if($r.StatusCode -eq 200){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
if %errorlevel% neq 0 goto wait_users
echo [OK] Users Service is ready on port 9020.

REM ── 5. Orders Service (port 9030) ─────────────────────────
echo.
echo [5/7] Starting Orders Service (port 9030)...
start "Orders Service (9030)" cmd /k "title Orders Service (9030) && cd /d %BACKEND% && gradlew.bat :services:orders-service:bootRun"

echo       Waiting for Orders Service to be ready...
:wait_orders
timeout /t 3 /nobreak >nul
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:9030/actuator/health' -UseBasicParsing -TimeoutSec 2; if($r.StatusCode -eq 200){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
if %errorlevel% neq 0 goto wait_orders
echo [OK] Orders Service is ready on port 9030.

REM ── 6. BFF Gateway (port 9010) ────────────────────────────
echo.
echo [6/7] Starting BFF Gateway (port 9010)...
start "BFF Gateway (9010)" cmd /k "title BFF Gateway (9010) && cd /d %BACKEND% && gradlew.bat :services:bff-gateway:bootRun"

echo       Waiting for BFF Gateway to be ready...
:wait_bff
timeout /t 3 /nobreak >nul
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:9010/actuator/health' -UseBasicParsing -TimeoutSec 2; if($r.StatusCode -eq 200){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
if %errorlevel% neq 0 goto wait_bff
echo [OK] BFF Gateway is ready on port 9010.

REM ── 7. Frontend (port 3000) ───────────────────────────────
echo.
echo [7/7] Starting Frontend (port 3000)...
cd /d "%FRONTEND%"
start "Frontend (3000)" cmd /k "title Frontend (3000) && cd /d %FRONTEND% && npm run dev"

echo       Waiting for Frontend to be ready...
:wait_frontend
timeout /t 3 /nobreak >nul
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:3000' -UseBasicParsing -TimeoutSec 3; if($r.StatusCode -eq 200){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
if %errorlevel% neq 0 goto wait_frontend
echo [OK] Frontend is ready on port 3000.

REM ── Done ──────────────────────────────────────────────────
echo.
echo ============================================================
echo   All services are running!
echo.
echo   Frontend:        http://localhost:3000
echo   BFF Gateway:     http://localhost:9010
echo   Keycloak Admin:  http://localhost:9000/admin
echo   Users Service:   http://localhost:9020
echo   Orders Service:  http://localhost:9030
echo   Redis:           localhost:6380
echo   PostgreSQL:      localhost:5432
echo.
echo   Login:  http://localhost:9010/oauth2/authorization/keycloak
echo ============================================================
echo.
echo Press any key to exit this window (services will keep running)...
pause >nul
