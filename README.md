# 👗 Fashion E-Commerce Microservices Platform

[![Java](https://img.shields.io/badge/Java-20%20%7C%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2025.1.2-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Apache Kafka](https://img.shields.io/badge/Kafka-KRaft_3.8-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

An enterprise-grade, high-throughput, event-driven e-commerce platform architected as an isolated multi-module microservices ecosystem using Java, Spring Boot 4, Spring Cloud, PostgreSQL, Redis, Kafka (KRaft mode), and AWS S3/LocalStack.

---

## 🏛️ High-Level System Architecture

```
                                  [ CLIENTS ]
                    (React / Next.js / iOS / Android / Postman)
                                       │
                                       │ HTTP / REST
                                       ▼
                     ┌───────────────────────────────────┐
                     │            API GATEWAY            │
                     │           (Port: 8088)            │
                     │  • OWASP Security Headers (CSP)   │
                     │  • WebFlux Reactive CORS Filter   │
                     │  • Distributed Tracing (MDC)      │
                     │  • Standardized JSON Envelope     │
                     └─────────────────┬─────────────────┘
                                       │
            ┌──────────────────────────┼──────────────────────────┐
            ▼                          ▼                          ▼
 ┌──────────────────────┐   ┌──────────────────────┐   ┌──────────────────────┐
 │  DISCOVERY SERVICE   │   │    CONFIG SERVER     │   │     AUTH SERVICE     │
 │    Netflix Eureka    │   │  Spring Cloud Config │   │   JWT HS512 / Auth   │
 │     (Port: 8761)     │   │     (Port: 8888)     │   │     (Port: 8081)     │
 └──────────────────────┘   └──────────────────────┘   └──────────┬───────────┘
                                                                  │
                                                                  ▼
 ┌────────────────────────────────────────────────────────────────────────────┐
 │                            DOMAIN MICROSERVICES                            │
 │                                                                            │
 │  ┌─────────────────┐   ┌─────────────────┐   ┌──────────────────────────┐  │
 │  │ CATALOG SERVICE │   │  ORDER SERVICE  │   │     PAYMENT SERVICE      │  │
 │  │  (Port: 8082)   │   │  (Port: 8083)   │   │       (Port: 8084)       │  │
 │  └────────┬────────┘   └────────┬────────┘   └────────────┬─────────────┘  │
 │           │                     │                         │                │
 │  ┌────────┴────────┐   ┌────────┴────────┐                │                │
 │  │ COUPON SERVICE  │   │  NOTIFICATION   │                │                │
 │  │  (Port: 8085)   │   │  (Port: 8086)   │                │                │
 │  └─────────────────┘   └─────────────────┘                │                │
 └─────────────────────────────────┬──────────────────────────┴────────────────┘
                                   │
                                   ▼
 ┌────────────────────────────────────────────────────────────────────────────┐
 │                         BACKED INFRASTRUCTURE LAYER                        │
 │                                                                            │
 │  ┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────┐  │
 │  │    POSTGRESQL 16     │  │       REDIS 7        │  │   APACHE KAFKA   │  │
 │  │ (6 Isolated DBs)     │  │ (Blacklist & Cache)  │  │  (KRaft Mode 3.8)│  │
 │  │ Port: 5432           │  │ Port: 6379           │  │ Port: 9092       │  │
 │  └──────────────────────┘  └──────────────────────┘  └──────────────────┘  │
 │  ┌──────────────────────────────────────────────────────────────────────┐  │
 │  │                  LOCALSTACK S3 (Product Media / Assets)              │  │
 │  │                  Port: 4566                                          │  │
 │  └──────────────────────────────────────────────────────────────────────┘  │
 └────────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Project Modules & Directory Layout

```text
FashionEcommerce-Microservices/
├── docs/                                # Master architecture handbook & diagrams
├── fashion-parent/                      # Maven multi-module reactor (v1.0.0-SNAPSHOT)
│   ├── pom.xml                          # Central DependencyManagement (BOM)
│   ├── .env.example                     # Environment variables template
│   ├── docker-compose.infra.yml         # Postgres, Redis, Kafka (KRaft), LocalStack
│   │
│   ├── common-core/                     # Shared cross-cutting utility library
│   │   ├── ApiResponse<T>               # Standardized JSON response envelope
│   │   ├── GlobalExceptionHandler       # Centralized REST advice with zero leak
│   │   ├── CorrelationIdFilter          # MDC distributed tracing filter
│   │   └── SecurityConstants            # Header & JWT claim definitions
│   │
│   ├── common-events/                   # Shared Kafka event schemas & contracts
│   │
│   ├── discovery-service/               # Netflix Eureka Service Registry (8761)
│   ├── config-server/                   # Centralized Configuration Server (8888)
│   ├── api-gateway/                     # Spring Cloud Gateway Ingress Proxy (8088)
│   │
│   ├── auth-service/                    # Authentication, RBAC, JWT HS512 (8081)
│   ├── catalog-service/                 # Products, Categories, S3 Images (8082)
│   ├── order-service/                   # Shopping Cart, Checkout, Saga (8083)
│   ├── payment-service/                 # Stripe/Razorpay Webhooks & Ledger (8084)
│   ├── coupon-service/                  # Discount Rules & Promo Engine (8085)
│   └── notification-service/            # Kafka Email, SMS, Push Dispatcher (8086)
├── .gitignore                           # Enterprise Git protection rules
└── README.md                            # Main project documentation
```

---

## 🌐 Service & Port Allocation Matrix

| Service / Infrastructure | Container / Artifact | Port | Description |
| :--- | :--- | :--- | :--- |
| **Discovery Service** | `discovery-service` | **`8761`** | Netflix Eureka Service Registry |
| **Config Server** | `config-server` | **`8888`** | Spring Cloud Central Config |
| **API Gateway** | `api-gateway` | **`8088`** | Reactive Gateway Proxy & Ingress |
| **Auth Service** | `auth-service` | **`8081`** | Identity, Registration, JWT Auth |
| **Catalog Service** | `catalog-service` | **`8082`** | Products, Inventory, Categories |
| **Order Service** | `order-service` | **`8083`** | Orders, Carts, Saga Orchestration |
| **Payment Service** | `payment-service` | **`8084`** | Payment Gateway Integration |
| **Coupon Service** | `coupon-service` | **`8085`** | Discounts, Vouchers, Promo Rules |
| **Notification Service** | `notification-service` | **`8086`** | Event-Driven Email/SMS Consumer |
| **PostgreSQL 16** | `fashion-postgres` | **`5432`** | 6 isolated schema databases |
| **Redis 7** | `fashion-redis` | **`6379`** | Token Blacklist & Session Cache |
| **Apache Kafka** | `fashion-kafka` | **`9092`** | KRaft mode (ZooKeeper-less) broker |
| **LocalStack AWS S3** | `fashion-localstack` | **`4566`** | Local cloud S3 storage simulator |

---

## ⚡ Prerequisites

Make sure the following software is installed on your machine:
* **Java Development Kit (JDK)**: Java 20 or Java 21
* **Docker Desktop**: Docker Engine 24+ & Docker Compose v2+
* **Git**: v2.30+
* **IDE**: IntelliJ IDEA (Ultimate/Community) or VS Code

---

## 🚀 Quick Start Guide

### **Step 1: Clone the Repository**
```bash
git clone <YOUR_REPOSITORY_URL>
cd FashionEcommerce-Microservices/fashion-parent
```

### **Step 2: Configure Environment Variables**
Copy the example environment template to `.env`:
```bash
cp .env.example .env
```
*(Verify that `DB_PASSWORD`, `JWT_SECRET`, and port mappings match your local settings).*

### **Step 3: Start Docker Infrastructure**
Launch PostgreSQL (with automatic schema initialization for all 6 DBs), Redis, Kafka KRaft, and LocalStack:
```bash
docker compose -f docker-compose.infra.yml up -d
```
Verify container health:
```bash
docker compose -f docker-compose.infra.yml ps
```

### **Step 4: Build All Modules**
Run the multi-module Maven build using the included Maven Wrapper:
```bash
# Windows
.\mvnw.cmd clean install -DskipTests

# Linux / macOS
./mvnw clean install -DskipTests
```

### **Step 5: Start the Microservices (Startup Order)**
Start the services in IntelliJ IDEA or terminal in the following sequence:

1. **`DiscoveryServiceApplication`** (`http://localhost:8761`) — *Eureka Dashboard*
2. **`ConfigServerApplication`** (`http://localhost:8888`) — *Configuration Provider*
3. **`ApiGatewayApplication`** (`http://localhost:8088`) — *Ingress Reverse Proxy*
4. **Domain Services**: `AuthServiceApplication`, `CatalogServiceApplication`, `OrderServiceApplication`, etc.

---

## 🛡️ Enterprise Security & Architecture Standards

### **1. Ingress Security & OWASP Hardening**
* **Strict OWASP Headers**: `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `X-XSS-Protection`, and `Permissions-Policy` applied at gateway boundary.
* **Non-Blocking CORS**: Fine-grained reactive `CorsWebFilter` with environment-driven origin whitelisting.

### **2. Distributed Tracing & Correlation**
* Every client request receives or generates a unique **`X-Correlation-Id`** (UUID).
* Binds to SLF4J **MDC** across all downstream servlet calls so logs across all microservices can be queried in ELK/Loki with a single Trace ID.

### **3. Zero-Leak Global Error Handling**
* Centralized `@RestControllerAdvice` in `common-core`.
* Handles DTO validation errors (`MethodArgumentNotValidException`) and maps field errors cleanly.
* Masks unexpected 500 server stack traces to prevent database and JVM information disclosure to external clients.

### **4. Standardized API Response Structure**
All endpoints return a uniform payload:
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Operation completed successfully",
  "data": { ... },
  "errors": null,
  "timestamp": "2026-08-26T10:00:00Z"
}
```

---

## 📜 Kafka Event-Driven Architecture (KRaft Mode)

Kafka runs in **ZooKeeper-less KRaft mode** (`apache/kafka:3.8.0`), utilizing internal Raft consensus for high throughput, sub-second failovers, and lightweight resource usage.

### **Key Event Topics:**
* `auth.user-registered.v1` ➡️ Sent by `auth-service` to trigger welcome emails.
* `order.created.v1` ➡️ Sent by `order-service` to initiate payment and inventory reservation.
* `payment.processed.v1` ➡️ Sent by `payment-service` to confirm orders and notify logistics.
* `inventory.low-stock.v1` ➡️ Sent by `catalog-service` to alert administrators.

---

## 👥 Contributors & Maintainers
* **Lead Architect / Developer**: PMRG Solutions Team
* **License**: Proprietary / MIT License
