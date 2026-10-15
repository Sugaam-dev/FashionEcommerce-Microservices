@echo off
setlocal enabledelayedexpansion

echo =====================================================================
echo   FASHION E-COMMERCE MICROSERVICES - DOCKER PLATFORM LAUNCHER
echo =====================================================================
echo.

cd /d "%~dp0"

:: 1. Check Docker Daemon
echo [*] Checking Docker status...
docker info >nul 2>&1
if errorlevel 1 (
    echo [!] Docker is not running or not responding.
    echo [*] Starting Docker Desktop...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    echo [*] Waiting for Docker to start (up to 45 seconds)...
    set /a count=0
    :wait_docker
    timeout /t 5 /nobreak >nul
    docker info >nul 2>&1
    if not errorlevel 1 goto docker_ready
    set /a count+=5
    if !count! leq 45 goto wait_docker
    echo [ERROR] Docker could not be started automatically. Please open Docker Desktop manually and retry.
    exit /b 1
)
:docker_ready
echo [OK] Docker daemon is running.

:: 2. Check and Build JARs if missing
echo.
echo [*] Checking microservice JAR artifacts...
if not exist "auth-service\target\auth-service-1.0.0-SNAPSHOT.jar" (
    echo [*] JAR artifacts not found. Compiling multi-module project...
    call .\mvnw.cmd package -DskipTests -T 1C
    if errorlevel 1 (
        echo [ERROR] Maven build failed.
        exit /b 1
    )
) else (
    echo [OK] Microservice JAR artifacts are present.
)

:: 3. Port Check Notification
echo.
echo [*] Database Configuration:
echo     - Local Windows PostgreSQL detected on port 5432.
echo     - Docker PostgreSQL is mapped to host port 5433 (HOST_DB_PORT=5433).
echo     - Microservices inside Docker connect to container 'postgres:5432'.
echo     - Host tools (DBeaver, pgAdmin) connect to 'localhost:5433'.

:: 4. Launch Docker Compose
echo.
echo [*] Starting all 15 microservices and infrastructure containers...
docker compose up -d
if errorlevel 1 (
    echo [ERROR] Docker Compose failed to start containers.
    exit /b 1
)

:: 5. Display Service Status Table
echo.
echo =====================================================================
echo   PLATFORM STATUS & SERVICE ACCESS URLS
echo =====================================================================
echo   API Gateway (Entrypoint)  : http://localhost:8088
echo   Eureka Service Discovery  : http://localhost:8761
echo   Spring Cloud Config Server: http://localhost:8888
echo   Auth Service              : http://localhost:8081  (or http://localhost:8088/api/v1/auth)
echo   Catalog Service           : http://localhost:8082  (or http://localhost:8088/api/v1/products)
echo   Order Service             : http://localhost:8083  (or http://localhost:8088/api/v1/orders)
echo   Payment Service           : http://localhost:8084  (or http://localhost:8088/api/v1/payments)
echo   Coupon Service            : http://localhost:8085  (or http://localhost:8088/api/v1/coupons)
echo   Notification Service      : http://localhost:8086
echo   Inventory Service         : http://localhost:8087  (or http://localhost:8088/api/v1/inventory)
echo.
echo   INFRASTRUCTURE (DOCKER):
echo   PostgreSQL (Docker)       : localhost:5433 (user: postgres, pass: root)
echo     Databases: auth_db, catalog_db, order_db, payment_db, coupon_db, inventory_db, notification_db
echo   Redis Cache               : localhost:6379
echo   Apache Kafka              : localhost:9092
echo   LocalStack (S3)           : http://localhost:4566
echo =====================================================================
echo.
echo View live logs anytime with: docker compose logs -f [service-name]
echo.
