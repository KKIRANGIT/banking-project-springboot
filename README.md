# Banking Project Spring Boot

Professional reference documentation for the Phase 9 Dockerized banking microservices platform in this repository.

## Overview

This repository contains four Spring Boot services and one shared Docker Compose stack:

- `banking-eureka-server`
  - Service registry
  - Port `8761`
- `banking-api-gateway`
  - Public entry point
  - Port `8080`
  - JWT validation, rate limiting, correlation ID propagation
- `banking-payment-service`
  - Transaction and authentication service
  - Internal port `8081`
  - Uses MySQL, Redis, Kafka, Feign, Resilience4j, Eureka
- `banking-account-service`
  - Account lookup and balance service
  - Internal port `8082`
  - Uses MySQL, JWT validation, Eureka
- `banking-frontend`
  - React + Tailwind operator dashboard
  - Public port `5173`
  - Uses the API gateway as its backend entry point

All services are Dockerized and started from the single root [`docker-compose.yml`](./docker-compose.yml).

## Docker Runtime

### Base Image

Each service Dockerfile uses:

```text
amazoncorretto:17
```

### JVM Settings

Each service container runs with:

```text
-Xms256m -Xmx512m
```

### Service Healthchecks

Each application container includes a Docker `HEALTHCHECK` instruction:

- Eureka: `http://localhost:8761/actuator/health`
- Gateway: `http://localhost:8080/actuator/health`
- Payment: `http://localhost:8081/actuator/health`
- Account: `http://localhost:8082/actuator/health`
- Frontend: `http://localhost/`

Compose also waits on health where it matters:

- `mysql` must be healthy before payment and account start
- `kafka` must be healthy before payment starts
- `eureka-server` must be healthy before gateway, payment, and account start

## Project Structure

```text
banking-project-springboot/
|-- banking-eureka-server/
|   |-- src/main/java/
|   |-- src/main/resources/
|   |-- Dockerfile
|   `-- pom.xml
|-- banking-api-gateway/
|   |-- src/main/java/
|   |-- src/main/resources/
|   |-- Dockerfile
|   `-- pom.xml
|-- banking-payment-service/
|   |-- src/main/java/
|   |-- src/main/resources/
|   |-- Dockerfile
|   `-- pom.xml
|-- banking-account-service/
|   |-- src/main/java/
|   |-- src/main/resources/
|   |-- Dockerfile
|   `-- pom.xml
|-- banking-frontend/
|   |-- src/
|   |-- package.json
|   |-- tailwind.config.js
|   `-- vite.config.js
|-- infra/
|   `-- mysql-init/
|       `-- 01-create-databases.sql
|-- docker-compose.yml
|-- .env
|-- .gitignore
`-- README.md
```

## Technology Stack

- Java 17
- Spring Boot 3.4.4
- Spring Cloud 2024.0.1
- Netflix Eureka
- Spring Cloud Gateway
- Spring Security with JWT
- OpenFeign
- Resilience4j
- Spring Data JPA
- Flyway
- MySQL 8.4
- Redis
- Apache Kafka
- Docker Compose
- React 18
- Vite
- Tailwind CSS

## Runtime Ports

Public:

- API Gateway: `8080`
- Frontend Dev Server: `5173`
- Eureka Dashboard: `8761`
- MySQL: `3306`
- Redis: `6379`
- Kafka: `9092`
- ZooKeeper: `2181`

Internal Docker network:

- Payment Service: `8081`
- Account Service: `8082`
- Kafka internal listener: `29092`

## Databases

The MySQL init script creates two schemas:

- `banking_payments_db`
- `banking_accounts_db`

Source file:

- [`infra/mysql-init/01-create-databases.sql`](./infra/mysql-init/01-create-databases.sql)

## Environment Variables

All runtime variables are defined in the root `.env` file.

Key variables:

- `MYSQL_ROOT_PASSWORD`
- `DB_PASSWORD`
- `JWT_SECRET`
- `PAYMENT_DB_NAME`
- `ACCOUNT_DB_NAME`
- `EUREKA_SERVER_URL`
- `KAFKA_BOOTSTRAP_SERVERS`
- `GATEWAY_PORT`

The compose file reads these variables and injects them into the containers.

## Docker Compose Services

The root compose file starts:

- `mysql`
- `redis`
- `zookeeper`
- `kafka`
- `eureka-server`
- `api-gateway`
- `frontend`
- `payment-service`
- `account-service`

### Persistence

The Compose stack persists MySQL data using:

- `mysql-data`

## Security and Routing

### Gateway Routes

- `/api/auth/**` -> `banking-payment-service`
- `/api/payments/**` -> `banking-payment-service`
- `/api/accounts/**` -> `banking-account-service`

### JWT Behavior

Public gateway endpoints:

- `POST /api/auth/register`
- `POST /api/auth/login`

Protected:

- all other `/api/**` routes require `Authorization: Bearer <token>`

Invalid or expired token:

- gateway returns `401`

### Rate Limit

- max `10` requests per second per IP
- excess requests return `429`

### Correlation ID

- gateway forwards incoming `X-Correlation-ID`
- if absent, gateway generates a UUID
- gateway adds `X-Correlation-ID` to forwarded requests and responses
- payment and account services log the correlation ID

## Service Discovery

Registered in Eureka:

- `BANKING-EUREKA-SERVER` is the registry itself and exposed at `http://localhost:8761`
- `BANKING-API-GATEWAY`
- `BANKING-PAYMENT-SERVICE`
- `BANKING-ACCOUNT-SERVICE`

Feign in payment service resolves account service by service name through Eureka.

## Build and Run

### Build JARs

From the repository root:

```powershell
mvn -q -DskipTests -f banking-eureka-server\pom.xml package
mvn -q -DskipTests -f banking-api-gateway\pom.xml package
mvn -q -DskipTests -f banking-account-service\pom.xml package
mvn -q -DskipTests -f banking-payment-service\pom.xml package
```

### Start the Full Stack

```powershell
docker compose up -d --build
```

### Open the Frontend

After Docker starts, open:

```text
http://localhost:5173
```

The Dockerized frontend serves the React build through Nginx and proxies `/api/**` and `/actuator/**` to the API gateway.

### Frontend Local Dev Mode

Use this only when you want hot reload during frontend development:

```powershell
cd banking-frontend
npm install
npm run dev
```

Vite dev mode also proxies `/api/**` and `/actuator/**` to `http://localhost:8080`.

### Check Container Health

```powershell
docker compose ps -a
```

### Stop the Stack

```powershell
docker compose down
```

### Stop and Remove MySQL Volume

```powershell
docker compose down -v
```

## API Summary

All client traffic should go through the gateway.

The frontend follows the same rule and talks only to routed gateway paths.

## Frontend Usage Guide

The intended day-to-day entry point is the frontend:

```text
http://localhost:5173
```

### What the frontend does

- registers and logs in users through the gateway
- stores the JWT in browser local storage for the current browser session
- creates and loads accounts through account-service routes
- posts transactions through payment-service routes
- shows recent transactions for the selected account
- shows a live gateway health snapshot from `/actuator/health`

### Recommended first-time flow

1. Start the full stack:

```powershell
docker compose up -d --build
```

2. Open the UI:

```text
http://localhost:5173
```

3. In `Authentication desk`, create a user.
Recommended role:
- `TELLER`

Use example values:

```text
Username: teller1
Password: Password123
Role: TELLER
```

4. In `Open session`, log in with the same credentials.

Expected result:
- the UI shows `Signed in as teller1`
- the role appears in the auth panel
- account and transaction actions are now enabled

5. In `Create a fresh account`, create an account if you do not already have one.

Example values:

```text
Account number: ACC1001
Account holder name: Demo Customer
Opening balance: 5000.00
Status: ACTIVE
```

Expected result:
- the account is created
- the selected account card updates
- the account number becomes the active account in the dashboard

6. In `Lookup and inspect`, you can reload any existing account by entering its account number and clicking `Load account`.

7. In `Post a transaction`, enter:

```text
Amount: 100.00
Type: DEBIT
```

Then click `Submit transaction`.

Expected result:
- a success banner appears
- account balance refreshes automatically
- the recent transactions table updates

### Role behavior

- `TELLER`: can create transactions and view transactions
- `ADMIN`: can create transactions and view transactions
- `CUSTOMER`: can view transactions but cannot create transactions

If you log in as `CUSTOMER`, transaction submission will fail with `403 Forbidden`.

### Notes while using the UI

- account lookup and creation require a valid login
- transaction submission requires an active loaded account
- account numbers and balances shown in the UI are live values returned from the backend
- the frontend talks only to the gateway, not directly to internal services
- if the gateway or backend is unavailable, the UI shows an error banner with the backend message

### Register

```text
POST http://localhost:8080/api/auth/register
```

Example payload:

```json
{
  "username": "teller1",
  "password": "Password123",
  "role": "TELLER"
}
```

### Login

```text
POST http://localhost:8080/api/auth/login
```

Example payload:

```json
{
  "username": "teller1",
  "password": "Password123"
}
```

### Get Account

```text
GET http://localhost:8080/api/accounts/ACC1001
```

### Create Transaction

```text
POST http://localhost:8080/api/payments/transactions
```

Example payload:

```json
{
  "accountNumber": "ACC1001",
  "amount": 100.00,
  "type": "DEBIT"
}
```

Recommended headers:

```text
Authorization: Bearer <jwt>
Idempotency-Key: <valid UUID>
X-Correlation-ID: <optional client value>
```

## Functional Validation Guide

### 1. Start the stack

```powershell
docker compose up -d --build
```

### 2. Open Eureka dashboard

```text
http://localhost:8761
```

Expected result:

- `BANKING-API-GATEWAY` -> `UP`
- `BANKING-PAYMENT-SERVICE` -> `UP`
- `BANKING-ACCOUNT-SERVICE` -> `UP`

### 3. Register a user

```powershell
$register = @{
  username = "teller1"
  password = "Password123"
  role     = "TELLER"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/auth/register" `
  -ContentType "application/json" `
  -Body $register
```

### 4. Login and get JWT

```powershell
$login = @{
  username = "teller1"
  password = "Password123"
} | ConvertTo-Json

$auth = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json" `
  -Body $login

$token = $auth.token
```

### 5. Create transaction through gateway

```powershell
$tx = @{
  accountNumber = "ACC1001"
  amount        = 100.00
  type          = "DEBIT"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/payments/transactions" `
  -Headers @{
    Authorization      = "Bearer $token"
    "Idempotency-Key" = "77777777-7777-7777-7777-777777777777"
    "X-Correlation-ID" = "phase9-demo-001"
  } `
  -ContentType "application/json" `
  -Body $tx
```

### 6. Verify Kafka event flow

Check payment-service logs:

```powershell
docker compose logs payment-service --tail=200
```

Look for:

- `Published event INITIATED ... payment.transaction.initiated`
- `Published event INITIATED ... payment.audit.events`
- `Published event COMPLETED ... payment.transaction.completed`
- `Published event COMPLETED ... payment.audit.events`

### 7. Verify audit log persistence

```powershell
docker compose exec -T mysql mysql -uroot -p<MYSQL_ROOT_PASSWORD> -D banking_payments_db -e "SELECT id, transaction_id, account_number, event_type, topic_name FROM audit_logs ORDER BY id DESC LIMIT 5;"
```

Expected result:

- rows exist in `audit_logs`
- both `INITIATED` and `COMPLETED` events are persisted

### 8. Verify circuit breaker

Stop account service:

```powershell
docker compose stop account-service
```

Send multiple gateway transaction requests.

Expected result:

- first request retries and then returns `503`
- later requests return `503` quickly after the breaker opens
- payment logs show `CLOSED -> OPEN`

Restart account service:

```powershell
docker compose start account-service
```

Wait for health and send another transaction.

Expected result:

- payment logs show `OPEN -> HALF_OPEN`
- recovery transaction succeeds
- payment logs show `HALF_OPEN -> CLOSED`

## Verified Local Result

Verified in Docker on `March 22, 2026`:

- all containers reached healthy state through `docker compose`
- all four service Dockerfiles built on `amazoncorretto:17`
- Eureka dashboard showed payment, account, and gateway as `UP`
- user registration and login worked through the gateway
- transaction creation through the gateway returned `SUCCESS`
- payment logs showed Kafka event publication for `INITIATED` and `COMPLETED`
- payment logs showed `Audit event consumed for account ACC1001 on topic payment.audit.events`
- MySQL query returned audit rows in `banking_payments_db.audit_logs`
- stopping account-service produced `503` responses and opened the circuit breaker
- after account-service restart, the breaker moved `OPEN -> HALF_OPEN -> CLOSED`

## Operational Notes

- use only the root Compose file
- client traffic should go through the gateway
- payment and account services are internal-only in Compose
- `.env` is the source of runtime environment values for local Docker runs
- if Kafka fails after an unclean restart, restart `zookeeper` first and then start `kafka`
- `docker compose down -v` is the clean reset path when you need MySQL to re-run schema initialization

## Maintenance Standard

Keep this README current whenever the project changes.

Update this document whenever these change:

- service names
- ports
- Docker images
- environment variables
- healthchecks
- route mappings
- JWT or gateway behavior
- database names
- Compose startup dependencies
- verification commands

When updating in future, keep it professional:

- document actual current behavior
- prefer repository-root commands
- keep one source of truth for Docker runtime instructions
- include real request examples for externally used endpoints
- record operational caveats and recovery steps
