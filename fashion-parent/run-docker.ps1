# Fashion E-Commerce Microservices - PowerShell Launcher
$ErrorActionPreference = "Stop"

Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "  FASHION E-COMMERCE MICROSERVICES - DOCKER PLATFORM LAUNCHER" -ForegroundColor Cyan
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host ""

Set-Location -Path $PSScriptRoot

# 1. Check Docker status
Write-Host "[*] Checking Docker status..." -ForegroundColor Yellow
try {
    $null = docker info 2>&1
    Write-Host "[OK] Docker daemon is running." -ForegroundColor Green
} catch {
    Write-Host "[!] Docker is not running. Launching Docker Desktop..." -ForegroundColor Yellow
    Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    $ready = $false
    for ($i = 0; $i -lt 10; $i++) {
        Start-Sleep -Seconds 5
        try {
            $null = docker info 2>&1
            $ready = $true
            break
        } catch { }
    }
    if (-not $ready) {
        Write-Error "Docker Desktop did not start in time. Please start it manually and retry."
        exit 1
    }
    Write-Host "[OK] Docker daemon is now ready." -ForegroundColor Green
}

# 2. Check JAR files
Write-Host "`n[*] Checking microservice JAR artifacts..." -ForegroundColor Yellow
if (-not (Test-Path "auth-service\target\auth-service-1.0.0-SNAPSHOT.jar")) {
    Write-Host "[*] JAR artifacts not found. Building with Maven wrapper..." -ForegroundColor Yellow
    & .\mvnw.cmd package -DskipTests -T 1C
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Maven build failed."
        exit 1
    }
} else {
    Write-Host "[OK] Microservice JAR artifacts are present." -ForegroundColor Green
}

# 3. Port Check Notification
Write-Host "`n[*] Database Port Architecture:" -ForegroundColor Yellow
Write-Host "    - Local Windows PostgreSQL runs on default port 5432."
Write-Host "    - Docker PostgreSQL is mapped to host port 5433 (HOST_DB_PORT=5433)."
Write-Host "    - Microservices inside Docker network connect directly to 'postgres:5432'."
Write-Host "    - Host database clients (pgAdmin, DBeaver) connect to 'localhost:5433'."

# 4. Launch Docker Compose
Write-Host "`n[*] Starting all microservices and infrastructure via Docker Compose..." -ForegroundColor Yellow
docker compose up -d

if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker Compose failed to start."
    exit 1
}

# 5. Display Access URLs
Write-Host "`n=====================================================================" -ForegroundColor Green
Write-Host "  PLATFORM STATUS & SERVICE ACCESS URLS" -ForegroundColor Green
Write-Host "=====================================================================" -ForegroundColor Green
Write-Host "  API Gateway (Entrypoint)   : http://localhost:8088"
Write-Host "  Eureka Service Discovery   : http://localhost:8761"
Write-Host "  Spring Cloud Config Server : http://localhost:8888"
Write-Host "  Auth Service               : http://localhost:8081  (or http://localhost:8088/api/v1/auth)"
Write-Host "  Catalog Service            : http://localhost:8082  (or http://localhost:8088/api/v1/products)"
Write-Host "  Order Service              : http://localhost:8083  (or http://localhost:8088/api/v1/orders)"
Write-Host "  Payment Service            : http://localhost:8084  (or http://localhost:8088/api/v1/payments)"
Write-Host "  Coupon Service             : http://localhost:8085  (or http://localhost:8088/api/v1/coupons)"
Write-Host "  Notification Service       : http://localhost:8086"
Write-Host "  Inventory Service          : http://localhost:8087  (or http://localhost:8088/api/v1/inventory)"
Write-Host ""
Write-Host "  INFRASTRUCTURE (DOCKER):"
Write-Host "  PostgreSQL (Docker)        : localhost:5433 (user: postgres, pass: root)"
Write-Host "    Databases: auth_db, catalog_db, order_db, payment_db, coupon_db, inventory_db, notification_db"
Write-Host "  Redis Cache                : localhost:6379"
Write-Host "  Apache Kafka               : localhost:9092"
Write-Host "  LocalStack (S3)            : http://localhost:4566"
Write-Host "=====================================================================" -ForegroundColor Green
Write-Host "`nTo view live logs: docker compose logs -f [service-name]" -ForegroundColor Cyan
