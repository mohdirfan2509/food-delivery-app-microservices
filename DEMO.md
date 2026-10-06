# 5-10 Minute Evaluation & Demo Guide

This document provides a streamlined, step-by-step walkthrough to demonstrate the **Online Food Ordering & Delivery Management System** microservices to an evaluator.

Centralized Configuration Repository: **`https://github.com/mohdirfan2509/config-repo`**  
Main Application Repository: **`https://github.com/mohdirfan2509/food-delivery-app-microservices`**

---

## 1. Quick Startup Sequence

### Step 1: Start Docker Infrastructure
```bash
docker compose up -d
# Wait 10 seconds and verify health
docker compose ps
```
*Expected*: `food-delivery-mysql`, `food-delivery-redis`, and `food-delivery-kafka` report `(healthy)`.

### Step 2: Build Project
```bash
mvn clean install -DskipTests
```

### Step 3: Start Spring Cloud Config Server (:8888)
```bash
java -jar config-server/target/config-server-1.0.0-SNAPSHOT.jar &
sleep 5
```

### Step 4: Show Config Server Health & GitHub Configuration Retrieval
```bash
# 1. Config Server Actuator Health
curl -s http://localhost:8888/actuator/health | jq .status
# Expected: "UP"

# 2. Config Server retrieving configuration from GitHub (https://github.com/mohdirfan2509/config-repo)
curl -s http://localhost:8888/user-service/default | jq '{name: .name, source: .propertySources[0].name}'
curl -s http://localhost:8888/food-service/default | jq '{name: .name, source: .propertySources[0].name}'
```
*Expected*: Responses show `source: "https://github.com/mohdirfan2509/config-repo.git/..."`, proving configuration is cloned and served directly from GitHub.

### Step 5: Start Application Microservices & API Gateway
```bash
java -jar user-service/target/user-service-1.0.0-SNAPSHOT.jar &
java -jar food-service/target/food-service-1.0.0-SNAPSHOT.jar &
java -jar payment-service/target/payment-service-1.0.0-SNAPSHOT.jar &
java -jar order-service/target/order-service-1.0.0-SNAPSHOT.jar &
java -jar notification-service/target/notification-service-1.0.0-SNAPSHOT.jar &
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar &
sleep 10
```

### Step 6: Verify Service & API Gateway Health
```bash
for port in 8888 8081 8082 8084 8083 8085 8080; do
  echo "Checking port $port: $(curl -s http://localhost:$port/actuator/health | jq -r .status)"
done
```
*Expected*: All 7 services report `UP`.

---

## 2. Interactive Demo Walkthrough (Through Gateway :8080)

### Step 1: Register and Login a Customer
```bash
# Register Customer
curl -s -X POST http://localhost:8080/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "David Miller",
    "email": "david@foodapp.com",
    "password": "password123",
    "phone": "9876543210",
    "address": "123 Oak Street"
  }' | jq .

# Login Customer -> Store JWT
CUSTOMER_TOKEN=$(curl -s -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{\"email\":\"david@foodapp.com\",\"password\":\"password123\"}' | jq -r .token)
echo "Customer JWT: $CUSTOMER_TOKEN"
```

---

### Step 2: Login as Seed Admin & Create Catalog Items
```bash
# Login Admin -> Store JWT
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{\"email\":\"admin@foodapp.com\",\"password\":\"Admin@1234\"}' | jq -r .token)

# Admin Creates Restaurant
REST_ID=$(curl -s -X POST http://localhost:8080/restaurants \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"name":"Tokyo Sushi Bar","address":"50 Sakura Lane","phone":"9871112233","active":true}' | jq -r .restaurantId)

# Admin Creates Food Item
FOOD_ID=$(curl -s -X POST http://localhost:8080/foods \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{\"restaurantId\":$REST_ID,\"name\":\"Salmon Nigiri Set\",\"description\":\"Fresh Atlantic salmon\",\"price\":18.00,\"category\":\"Main Course\",\"available\":true}" | jq -r .foodId)
echo "Created Restaurant ID: $REST_ID, Food ID: $FOOD_ID"
```

---

### Step 3: Customer Places Order (Distributed Feign & Kafka Flow)
```bash
# Customer creates order with 2x Salmon Nigiri Set (2 * 18.00 = 36.00)
ORDER_RESP=$(curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d "{\"deliveryAddress\":\"123 Oak Street\",\"items\":[{\"foodId\":$FOOD_ID,\"quantity\":2}]}")
echo "$ORDER_RESP" | jq .
ORDER_ID=$(echo "$ORDER_RESP" | jq -r .orderId)
```
*Key Highlights for Evaluator*:
- Order total `$36.00` was authoritative and calculated by the backend via OpenFeign call to Food Service.
- Payment was processed synchronously via OpenFeign call to Payment Service.
- Order status is `CONFIRMED`.

---

### Step 4: Show Payment & Kafka Event Notification
```bash
# 1. View Payment Record
curl -s -X GET http://localhost:8080/payments/order/$ORDER_ID \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq .

# 2. View Asynchronously Consumed Kafka Notification
sleep 2
curl -s -X GET http://localhost:8080/notifications/my \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq .
```
*Key Highlights for Evaluator*:
- Payment status is `SUCCESS`.
- Notification Service received the `order-created` event via Kafka and stored the notification without calling Order Service.

---

### Step 5: Demonstrate Security & Authorization Controls
```bash
# 1. Register a second customer (Eve)
EVE_TOKEN=$(curl -s -X POST http://localhost:8080/users/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Eve","email":"eve@foodapp.com","password":"password123","phone":"9876543219","address":"77 Dark Alley"}' > /dev/null && \
  curl -s -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"eve@foodapp.com","password":"password123"}' | jq -r .token)

# 2. Eve tries to access David's order (HTTP 403 Forbidden)
curl -i -X GET http://localhost:8080/orders/$ORDER_ID \
  -H "Authorization: Bearer $EVE_TOKEN"

# 3. Customer tries to access Admin-only listing of all orders (HTTP 403 Forbidden)
curl -i -X GET http://localhost:8080/orders \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# 4. Admin accesses all orders (HTTP 200 OK)
curl -s -X GET http://localhost:8080/orders \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.[0:3]'

# 5. Unauthenticated request without JWT (HTTP 401 Unauthorized)
curl -i -X GET http://localhost:8080/orders
```

---

### Step 6: Demonstrate Redis Caching
```bash
# Inspect cached food in Redis
docker compose exec redis redis-cli KEYS "food::*"

# Check TTL (Time to Live)
docker compose exec redis redis-cli TTL "food::$FOOD_ID"
```
*Key Highlights for Evaluator*:
- Catalog entries are cached with a 10-minute TTL.
- Cache is automatically evicted upon any food update or deletion.

---

### Step 7: Clean Shutdown
```bash
pkill -f "SNAPSHOT.jar"
docker compose down
```
