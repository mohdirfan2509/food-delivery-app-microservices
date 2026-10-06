# Online Food Ordering & Delivery Management System (Microservices)

## 1. Project Title & Overview
**System**: Online Food Ordering and Delivery Management System  
**Architecture**: Cloud-Native Spring Boot Microservices  
**Runtime**: Java 21 LTS | Spring Boot 3.3.4 | Spring Cloud 2023.0.3  

This project is an academic multi-module microservices application modeling a production-grade online food ordering and delivery management system. The architecture implements decentralized microservice patterns, database-per-service isolation, centralized externalized configuration management via Spring Cloud Config Server backed by a dedicated GitHub repository, an API reverse proxy gateway, Redis catalog caching, OpenFeign declarative inter-service communication, and asynchronous event-driven notifications powered by Apache Kafka (KRaft).

### Repositories
- **Main Application Repository**: `https://github.com/mohdirfan2509/food-delivery-app-microservices`
- **Separate Configuration Repository**: `https://github.com/mohdirfan2509/config-repo`

---

## 2. High-Level Architecture Overview

```
                                     CLIENT
                                       │
                                       │ HTTP REST + Bearer JWT
                                       ▼
                             ┌───────────────────┐
                             │    API GATEWAY    │ (:8080)
                             │   Reverse Proxy   │
                             └─────────┬─────────┘
                                       │ Transparent Authorization Forwarding
      ┌──────────────────┬─────────────┼──────────────┬──────────────────┐
      │                  │             │              │                  │
      ▼                  ▼             ▼              ▼                  ▼
  /users/**          /foods/**     /orders/**     /payments/**     /notifications/**
┌──────────────┐   ┌────────────┐┌────────────┐ ┌─────────────┐   ┌─────────────────┐
│ User Service │   │Food Service││Order Service││Payment Service│  │Notification Svc │
│    :8081     │   │   :8082    ││   :8083    │ │    :8084    │   │      :8085      │
└──────┬───────┘   └────┬───────┘└────┬───────┘ └──────┬──────┘   └────────┬────────┘
       │                 │             │               │                   ▲
       ▼                 ▼             │ Feign         ▼                   │ Kafka
     userdb            fooddb          ├───► Food Svc  paymentdb           │ order-created
                         │             │     (:8082)                       │
                         ▼             │ Feign                             │
                    Redis Cache        └───► Payment Svc                   │
                     (:6380)                 (:8084)                       │
                                       │                                   │
                                       └────────── Kafka Event ────────────┘
                                                  (order-created)          │
                                                         │                 ▼
                                                         ▼          notificationdb
                                                 Apache Kafka Broker
                                                       (:9092)
```

---

## 3. Technology Stack

| Layer / Concern | Technology | Version | Purpose |
|---|---|---|---|
| Language | Java (OpenJDK) | 21 LTS | Core programming language |
| Framework | Spring Boot | 3.3.4 | Microservice application framework |
| Cloud Components | Spring Cloud Gateway & Config | 2023.0.3 | Routing and externalized configuration |
| Declarative REST | Spring Cloud OpenFeign | 4.1.3 | Synchronous inter-service RPC |
| Security | Spring Security & JJWT | 6.3.3 / 0.12.6 | Stateless JWT authentication & RBAC |
| Password Hashing | BCrypt | Standard | Adaptive one-way password hashing |
| Relational DB | MySQL | 8.4 LTS | Multi-database persistence per bounded context |
| ORM / Persistence | Spring Data JPA / Hibernate | 6.5.3 | Entity management and transactional repository |
| Cache Engine | Redis | 7.2 Alpine | In-memory catalog and menu cache with TTL |
| Event Streaming | Apache Kafka (KRaft mode) | 3.7.1 | Asynchronous message broker for domain events |
| Build Tool | Apache Maven | 3.9+ | Multi-module reactor build system |
| Containerization | Docker Compose | 2.20+ | Reproducible local infrastructure orchestration |

---

## 4. Repository & Module Structure

### 4.1 Main Application Repository (`food-delivery-app-microservices`)
```
food-delivery-app-microservices/
├── pom.xml                               # Root Maven parent aggregator POM
├── docker-compose.yml                    # Local infrastructure (MySQL, Redis, Kafka)
├── .env.example                          # Environment variable template
├── init-scripts/
│   └── 01-init-databases.sql             # SQL script initializing the 5 separate schemas
├── common-dto/                           # Reusable cross-service DTOs and Kafka events ONLY
│   └── src/main/java/com/fooddelivery/common/
│       ├── dto/                          # PaymentRequest, PaymentResponse
│       ├── enums/                        # PaymentStatus
│       └── event/                        # OrderCreatedEvent
├── config-server/                        # Spring Cloud Config Server (:8888, Git-backed)
├── api-gateway/                          # Spring Cloud Gateway (:8080)
├── user-service/                         # Customer & Admin authentication (:8081)
├── food-service/                         # Restaurant & Food catalog with Redis cache (:8082)
├── payment-service/                      # Idempotent payment processing (:8084)
├── order-service/                        # Order workflow, Feign orchestrator & Kafka producer (:8083)
└── notification-service/                 # Kafka event consumer & notification repository (:8085)
```

### 4.2 Separate Configuration Repository (`config-repo`)
Hosted at **`https://github.com/mohdirfan2509/config-repo`**:
```
config-repo/
├── README.md                             # Configuration repository purpose and documentation
├── application.yml                       # Global shared properties (logging, common actuator, JWT)
├── api-gateway.yml                       # Gateway routing table, CORS policy, management endpoints
├── user-service.yml                      # User Service datasource, JPA, initial seed admin
├── food-service.yml                      # Food Service datasource, Redis cache connection & TTL
├── order-service.yml                     # Order Service datasource, OpenFeign URLs, Kafka topic
├── payment-service.yml                   # Payment Service datasource, JPA
└── notification-service.yml              # Notification Service datasource, Kafka consumer group
```

---

## 5. Centralized Configuration Architecture (Spring Cloud Config Server)

```
GitHub config-repo (https://github.com/mohdirfan2509/config-repo)
        │
        │ Git clone / pull over HTTPS
        ▼
Spring Cloud Config Server :8888 (No database)
        │
        ├───► api-gateway          (:8080)
        ├───► user-service         (:8081)
        ├───► food-service         (:8082)
        ├───► order-service        (:8083)
        ├───► payment-service      (:8084)
        └───► notification-service (:8085)
```

> [!IMPORTANT]
> **Config Server does not use a database. It retrieves centralized configuration from the external Git repository.**

### Key Design Principles:
1. **Git Backend**: Config Server is configured with the Git backend pointing to `https://github.com/mohdirfan2509/config-repo.git` on branch `main` with `clone-on-start: true`.
2. **Independent Repository**: The configuration repository is completely decoupled from the main code repository. No application code, build scripts, or secrets are stored in `config-repo`.
3. **No Secrets Committed**: Sensitive configuration values (passwords, JWT secret tokens, database credentials) use environment variable references (`${USER_DB_PASSWORD}`, `${JWT_SECRET}`) so version control remains clean.
4. **How Services Obtain Configuration**: Each microservice points to Config Server on startup via Spring Boot 3 config import:
   ```yaml
   spring:
     config:
       import: "optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}"
   ```
5. **Config Server Actuator & Retrieval Endpoints**:
   - Health check: `http://localhost:8888/actuator/health`
   - Service profiles:
     - `http://localhost:8888/user-service/default`
     - `http://localhost:8888/food-service/default`
     - `http://localhost:8888/order-service/default`
     - `http://localhost:8888/payment-service/default`
     - `http://localhost:8888/notification-service/default`
     - `http://localhost:8888/api-gateway/default`

---

## 6. Microservice Responsibilities, Ports & Databases

| Service | Port | Bounded Context / Responsibility | Database / Schema | Direct Storage Access |
|---|---|---|---|---|
| **Config Server** | `8888` | Centralized externalized configuration management | None (External Git repo) | N/A |
| **API Gateway** | `8080` | Unified client entry point, routing, CORS, header forwarding | None | N/A |
| **User Service** | `8081` | Customer registration, login, JWT token issuing, profile RBAC | `userdb` | MySQL `3307` |
| **Food Service** | `8082` | Restaurant & food catalog CRUD, public browsing, Redis caching | `fooddb` + Redis | MySQL `3307` / Redis `6380` |
| **Order Service** | `8083` | Order lifecycle, authoritative price computation, Feign RPC | `orderdb` | MySQL `3307` |
| **Payment Service** | `8084` | Payment execution, unique transaction ref, orderId idempotency | `paymentdb` | MySQL `3307` |
| **Notification Service** | `8085` | Kafka event consumption, idempotent persistence, user inbox | `notificationdb` | MySQL `3307` |

> [!IMPORTANT]
> **Strict Database Isolation**: No service shares tables or directly queries another microservice's database. All inter-service data exchange is strictly achieved via OpenFeign REST clients or Apache Kafka events.

---

## 7. End-to-End Business Workflows

### 7.1 Authentication & Authorization Flow
1. **Public Registration**: `POST /users/register` accepts customer details and automatically assigns role `ROLE_CUSTOMER`. Public registration cannot assign `ADMIN`.
2. **Seed Admin**: An initial administrator (`admin@foodapp.com` / `Admin@1234`) is seeded automatically on startup.
3. **Stateless JWT**: Upon successful `POST /users/login`, the service generates an HMAC-SHA256 signed JWT containing `sub` (email), `customerId`, and `role`.
4. **Gateway Forwarding**: Client sends `Authorization: Bearer <JWT>` to `http://localhost:8080`. Gateway forwards the header unmodified to downstream microservices.
5. **Decentralized Verification**: Each microservice independently parses and validates the JWT signature, enforcing role-based permissions (`CUSTOMER` vs `ADMIN`) and customer data ownership.

### 7.2 Order Lifecycle & Distributed Flow
```
Customer                    API Gateway               Order Service             Food Service          Payment Service         Kafka Broker         Notification Svc
   │                             │                          │                         │                      │                      │                     │
   │─── POST /orders (JWT) ──────►│                          │                         │                      │                      │                     │
   │                             │─── Forward POST ────────►│                         │                      │                      │                     │
   │                             │                          ├─── GET /foods/{id} ────►│                      │                      │                     │
   │                             │                          │    (Authoritative Price)│                      │                      │                     │
   │                             │                          │◄── Food DTO ────────────│                      │                      │                     │
   │                             │                          │                                                │                      │                     │
   │                             │                          ├─── POST /payments (Feign) ────────────────────►│                      │                     │
   │                             │                          │◄── Payment SUCCESS ────────────────────────────│                      │                     │
   │                             │                          │                                                                       │                     │
   │                             │                          ├─── Persist Order (CONFIRMED) in orderdb                               │                     │
   │                             │                          ├─── Publish OrderCreatedEvent ────────────────────────────────────────►│                     │
   │                             │◄── 201 Created (Order) ──│                                                                       │                     │
   │◄── 201 Created ─────────────│                          │                                                                       ├─── Consume Event ──►│
   │                             │                          │                                                                       │    (order-created)  │
   │                             │                          │                                                                       │                     │─── Persist in
   │                             │                          │                                                                       │                        notificationdb
```

1. **Client Request**: Customer submits order items specifying only `foodId` and `quantity`.
2. **Authoritative Price Validation**: Order Service calls Food Service via OpenFeign (`GET /foods/{id}`). The client cannot dictate or tamper with food prices. The backend computes:
   $$\text{Total Amount} = \sum (\text{Authoritative Unit Price} \times \text{Quantity})$$
3. **Payment Execution**: Order Service synchronously calls Payment Service via OpenFeign (`POST /payments`) passing `orderId`, `customerId`, and the computed `amount`.
4. **Order Confirmation**: Upon receiving `PaymentStatus.SUCCESS`, Order Service marks order status as `CONFIRMED` and saves it to `orderdb`.
5. **Event Publication**: Order Service publishes `OrderCreatedEvent` to Kafka topic `order-created`.
6. **Asynchronous Notification**: Notification Service consumes the event in consumer group `notification-service-group` and idempotently stores a notification record in `notificationdb`.

---

## 8. API Routing & Gateway Endpoint Matrix

All external traffic enters through API Gateway at **`http://localhost:8080`**.

| Service | HTTP Method | Gateway Endpoint | Access Level | Description |
|---|---|---|---|---|
| **User** | `POST` | `/users/register` | Public | Register new customer account |
| **User** | `POST` | `/users/login` | Public | Authenticate and obtain JWT token |
| **User** | `GET` | `/users/{id}` | Authenticated | View profile (`CUSTOMER`: own profile, `ADMIN`: any) |
| **User** | `PUT` | `/users/{id}` | Authenticated | Update profile (`CUSTOMER`: own profile, `ADMIN`: any) |
| **User** | `DELETE` | `/users/{id}` | Admin Only | Delete customer account (`ADMIN` only) |
| **Food** | `GET` | `/restaurants` | Public | List active restaurants |
| **Food** | `GET` | `/restaurants/{id}` | Public | Get restaurant details |
| **Food** | `POST` | `/restaurants` | Admin Only | Create new restaurant |
| **Food** | `PUT` | `/restaurants/{id}` | Admin Only | Update existing restaurant |
| **Food** | `DELETE`| `/restaurants/{id}` | Admin Only | Soft/safe delete restaurant |
| **Food** | `GET` | `/foods` | Public | List all available food items |
| **Food** | `GET` | `/foods/{id}` | Public | Get food item details (cached in Redis) |
| **Food** | `POST` | `/foods` | Admin Only | Create food item (evicts Redis cache) |
| **Food** | `PUT` | `/foods/{id}` | Admin Only | Update food item (evicts Redis cache) |
| **Food** | `DELETE`| `/foods/{id}` | Admin Only | Delete food item (evicts Redis cache) |
| **Order** | `POST` | `/orders` | Customer | Create new order (triggers Feign & Kafka) |
| **Order** | `GET` | `/orders/{id}` | Authenticated | View order (`CUSTOMER`: own order, `ADMIN`: any) |
| **Order** | `GET` | `/orders/my-orders` | Customer | View authenticated customer's order history |
| **Order** | `GET` | `/orders` | Admin Only | View all orders across all customers |
| **Payment**| `POST` | `/payments` | Authenticated | Process payment (idempotent on `orderId`) |
| **Payment**| `GET` | `/payments/{id}` | Authenticated | View payment details by payment ID |
| **Payment**| `GET` | `/payments/order/{orderId}` | Authenticated | View payment by order ID |
| **Notification**| `GET` | `/notifications/my` | Authenticated | List notifications for authenticated customer |
| **Notification**| `GET` | `/notifications/{id}` | Authenticated | View notification (`CUSTOMER`: own, `ADMIN`: any) |
| **Notification**| `PATCH`| `/notifications/{id}/read` | Authenticated | Mark notification as read |
| **Gateway** | `GET` | `/actuator/health` | Public | Returns `{"status":"UP"}` |

---

## 9. Development Setup & Execution Instructions

### 9.1 Prerequisites
- **Java**: OpenJDK 21 LTS (`java -version` returns 21)
- **Maven**: Version 3.9+ (`mvn -version`)
- **Docker & Docker Compose**: Docker 24+ and Docker Compose v2+
- **Terminal & Utilities**: `curl`, `jq` (optional for JSON formatting)

### 9.2 Step 1: Start Infrastructure Containers
Start the multi-tenant MySQL 8.4, Redis 7.2, and Apache Kafka 3.7.1 containers:
```bash
docker compose up -d
```
Verify container health:
```bash
docker compose ps
# Output should show food-delivery-mysql, food-delivery-redis, food-delivery-kafka as (healthy)
```
> [!NOTE]
> Docker Compose strictly manages persistent backing infrastructure. It does NOT mount or contain configuration files. Config Server independently pulls configuration directly from GitHub over HTTPS.

### 9.3 Step 2: Build All Modules
Compile and package the entire multi-module reactor:
```bash
mvn clean install -DskipTests
```

### 9.4 Step 3: Launch Microservices in Required Order
Start each microservice in sequence. Ensure **Config Server** is running before the application services:

```bash
# 1. Config Server (Port 8888) - Clones config from GitHub
java -jar config-server/target/config-server-1.0.0-SNAPSHOT.jar &

# Wait 5 seconds for Config Server to initialize and clone config-repo
sleep 5

# Verify Config Server Health and GitHub Config Retrieval
curl -s http://localhost:8888/actuator/health | jq .status
curl -s http://localhost:8888/user-service/default | jq .name

# 2. User Service (Port 8081)
java -jar user-service/target/user-service-1.0.0-SNAPSHOT.jar &

# 3. Food Service (Port 8082)
java -jar food-service/target/food-service-1.0.0-SNAPSHOT.jar &

# 4. Payment Service (Port 8084)
java -jar payment-service/target/payment-service-1.0.0-SNAPSHOT.jar &

# 5. Order Service (Port 8083)
java -jar order-service/target/order-service-1.0.0-SNAPSHOT.jar &

# 6. Notification Service (Port 8085)
java -jar notification-service/target/notification-service-1.0.0-SNAPSHOT.jar &

# 7. API Gateway (Port 8080)
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar &
```

### 9.5 Step 4: Verify Actuator Health Endpoints
```bash
curl -s http://localhost:8888/actuator/health | jq .status   # "UP"
curl -s http://localhost:8081/actuator/health | jq .status   # "UP"
curl -s http://localhost:8082/actuator/health | jq .status   # "UP"
curl -s http://localhost:8084/actuator/health | jq .status   # "UP"
curl -s http://localhost:8083/actuator/health | jq .status   # "UP"
curl -s http://localhost:8085/actuator/health | jq .status   # "UP"
curl -s http://localhost:8080/actuator/health | jq .status   # "UP"
```

---

## 10. Comprehensive End-to-End Verification Demo

### Demo Credentials
- **Development Admin**: `admin@foodapp.com` | Password: `Admin@1234`
- **Default Seed Customer**: `customer@foodapp.com` | Password: `Customer@1234`

### Verification Script (cURL Commands)

#### 1. Register a New Customer via Gateway
```bash
curl -X POST http://localhost:8080/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Green",
    "email": "alice@foodapp.com",
    "password": "password123",
    "phone": "9876543210",
    "address": "42 Market Street"
  }'
```

#### 2. Authenticate Customer to Obtain JWT
```bash
CUST_TOKEN=$(curl -s -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@foodapp.com","password":"password123"}' | jq -r .token)
echo "Customer Token: $CUST_TOKEN"
```

#### 3. Authenticate Seed Admin
```bash
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@foodapp.com","password":"Admin@1234"}' | jq -r .token)
echo "Admin Token: $ADMIN_TOKEN"
```

#### 4. Admin Creates Restaurant and Food Item
```bash
# Create Restaurant
REST_ID=$(curl -s -X POST http://localhost:8080/restaurants \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"name":"Bella Italia","address":"10 Roma Way","phone":"9811223344","active":true}' | jq -r .restaurantId)

# Create Food Item
FOOD_ID=$(curl -s -X POST http://localhost:8080/foods \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{\"restaurantId\":$REST_ID,\"name\":\"Margherita Pizza\",\"description\":\"Classic wood-fired\",\"price\":15.50,\"category\":\"Main Course\",\"available\":true}" | jq -r .foodId)
echo "Created Food ID: $FOOD_ID"
```

#### 5. Customer Places Order via Gateway
```bash
ORDER_RESP=$(curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUST_TOKEN" \
  -d "{\"deliveryAddress\":\"42 Market Street\",\"items\":[{\"foodId\":$FOOD_ID,\"quantity\":2}]}")
echo "$ORDER_RESP" | jq .
ORDER_ID=$(echo "$ORDER_RESP" | jq -r .orderId)
```

#### 6. Verify Confirmed Order and Payment
```bash
# View Order
curl -s -X GET http://localhost:8080/orders/$ORDER_ID \
  -H "Authorization: Bearer $CUST_TOKEN" | jq .

# View Payment
curl -s -X GET http://localhost:8080/payments/order/$ORDER_ID \
  -H "Authorization: Bearer $CUST_TOKEN" | jq .
```

#### 7. Verify Notification Delivery via Kafka
```bash
sleep 2
curl -s -X GET http://localhost:8080/notifications/my \
  -H "Authorization: Bearer $CUST_TOKEN" | jq .
```

#### 8. Verify Cross-Customer Authorization Denial (HTTP 403)
```bash
# Bob registers and logs in
BOB_TOKEN=$(curl -s -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@foodapp.com","password":"Customer@1234"}' | jq -r .token)

# Bob attempts to view Alice's order (Expect 403 Forbidden)
curl -i -X GET http://localhost:8080/orders/$ORDER_ID \
  -H "Authorization: Bearer $BOB_TOKEN"
```

#### 9. Verify CORS Preflight Header
```bash
curl -i -X OPTIONS http://localhost:8080/orders \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Authorization,Content-Type"
# HTTP 200 OK with Access-Control-Allow-Origin: http://localhost:3000
```

---

## 11. Automated Testing & Verification Suite

To run all automated regression tests across all 8 modules:
```bash
mvn clean test
```

### Test Count Breakdown (178 Tests Total)
- `common-dto`: 1 test (JSON serialization & validation)
- `config-server`: 1 test (Context load & Git configuration locator offline)
- `user-service`: 57 tests (Authentication, BCrypt hashing, JWT provider, RBAC, ownership, DatabaseIsolation)
- `food-service`: 43 tests (CRUD, Redis cache hit/miss, cache eviction, DatabaseIsolation)
- `payment-service`: 26 tests (Payment state machine, idempotency, validation, DatabaseIsolation)
- `order-service`: 14 tests (Order placement, Feign mocks, authorative calculation, Kafka producer, DatabaseIsolation)
- `notification-service`: 29 tests (Kafka consumer listener, idempotency, repository, DatabaseIsolation)
- `api-gateway`: 7 tests (Route definitions, Config server client, CORS preflight, route isolation)

**Summary: 178 tests run, 0 failures, 0 errors, 0 skipped (100% PASS).**

---

## 12. Security Architecture Summary

```
                      Client
                        │
                        │ Bearer JWT (Authorization Header)
                        ▼
                 ┌─────────────┐
                 │ API Gateway │ (:8080)
                 └──────┬──────┘
                        │ Transparent Pass-Through
                        ▼
               Individual Microservices
             (:8081, :8082, :8083, :8084, :8085)
                        │
                        ├─► 1. Stateless Filter: Extracts Bearer token
                        ├─► 2. Signature Verification: Validates HS256 signature & expiration
                        ├─► 3. Identity Extraction: Extracts customerId and Role
                        └─► 4. RBAC & Ownership: Enforces method-level security
```

1. **Zero Trust on Client Identity**: Services ignore arbitrary client headers (`X-User-ID`, `X-Role`). Identity is derived strictly from the cryptographically verified JWT claims.
2. **Stateless Security**: `SessionCreationPolicy.STATELESS` is enforced on every microservice. No HTTP sessions are stored.
3. **Defense in Depth**: Every microservice secures its own endpoints independently. If a malicious client bypassed the Gateway, direct service access remains fully protected.

---

## 13. Troubleshooting & Diagnostics

- **Docker Containers Not Healthy**: Run `docker compose ps`. If Kafka or MySQL failed to bind, ensure host ports `3307`, `6380`, and `9092` are not occupied by local native services.
- **Config Server Fails to Clone from GitHub**: Verify internet connectivity or check `CONFIG_REPO_GIT_URI` in `.env`. Check health via `curl http://localhost:8888/actuator/health`.
- **Microservice Fails to Fetch Config**: Ensure Config Server (`:8888`) is running and healthy prior to starting microservices. Check `curl http://localhost:8888/user-service/default`.
- **Database Connection Refused**: Confirm services connect to port `3307` (`MYSQL_PORT=3307`), which maps to MySQL in Docker.
- **Port Conflicts**: Run `ss -tulpn | grep -E "8888|8080|8081|8082|8083|8084|8085"` to check for existing processes. Kill with `pkill -f "SNAPSHOT.jar"`.
