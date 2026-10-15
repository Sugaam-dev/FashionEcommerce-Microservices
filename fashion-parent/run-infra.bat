@echo off
setlocal
cd /d "%~dp0"

echo =====================================================================
echo  STARTING DOCKER INFRASTRUCTURE STACK
echo =====================================================================
echo  - PostgreSQL (Docker mapped to Host 5433 to avoid local 5432 conflict)
echo  - Redis (Port 6379)
echo  - Apache Kafka (Modern KRaft Mode - No ZooKeeper) (Port 9092)
echo  - LocalStack S3 (Port 4566)
echo =====================================================================

docker compose -f docker-compose.infra.yml up -d

echo.
echo Checking infrastructure containers:
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo.
echo =====================================================================
echo  INFRASTRUCTURE IS READY!
echo  You can now start your Spring Boot microservices from IntelliJ.
echo  All services will automatically connect to Docker Postgres on 5433.
echo =====================================================================
pause
