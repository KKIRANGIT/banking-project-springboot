# Banking Project Spring Boot

Professional reference documentation for the banking microservices platform in this repository.

## Overview

This repository now contains four Spring Boot applications and one shared infrastructure stack:

- `banking-api-gateway`
  - Public entry point on `8080`
  - Validates JWTs before forwarding protected requests
  - Applies per-IP rate limiting
  - Propagates correlation IDs
- `banking-eureka-server`
  - Service registry on `8761`
  - Tracks the gateway, payment service, and account service
- `banking-payment-service`
  - Internal service on `8081`
  - Handles user registration, login, and transaction processing
  - Uses MySQL, Redis, Kafka, OpenFeign, Resilience4j, and Eureka discovery
- `banking-account-service`
  - Internal service on `8082`
  - Handles account lookup, creation, and balance updates
  - Uses its own MySQL schema, JWT validation, and Eureka discovery

Use the root [`docker-compose.yml`](./docker-compose.yml) as the single source of truth for local runtime.

## Architecture

### Request Flow

1. Client calls the API gateway on `http://localhost:8080`.
2. Gateway creates or forwards `X-Correlation-ID`.
3. Gateway validates JWTs for protected `/api/**` routes.
4. Gateway rate-limits requests to `10` requests per second per IP.
5. Gateway routes requests by service name through Eureka:
   - `/api/payments/**` -> `banking-payment-service`
   - `/api/accounts/**` -> `banking-account-service`
6. Payment service calls account service through Feign using Eureka service discovery.
7. Account service validates account state and balance.

### Service Responsibilities

#### API Gateway

- Route incoming requests to downstream services
- Reject invalid or expired JWTs with `401`
- Reject bursts above `10` requests per second per IP with `429`
- Add `X-Correlation-ID` to forwarded requests and responses

#### Eureka Server

- Registry for all discoverable services
- Dashboard available at `http://localhost:8761`
- Self-registration disabled

#### Payment Service

- Register and authenticate users
- Issue JWT tokens
- Create, list, fetch, and delete transactions
- Publish Kafka transaction and audit events
- Validate account state and funds through Feign
- Protect downstream account access with Resilience4j retry and circuit breaker

#### Account Service

- Create accounts
- Fetch account details by account number
- Update balances
- Validate JWTs using the shared secret

## Project Structure

```text
banking-project-springboot/
|-- banking-api-gateway/
|   |-- src/main/java/
|   |-- src/main/resources/
|   |-- Dockerfile
|   `-- pom.xml
|-- banking-eureka-server/
|   |-- src/main/java/
|   |-- src/main/resources/
|   |-- Dockerfile
|   `-- pom.xml
|-- banking-account-service/
|   |-- src/main/java/
|   |-- src/main/resources/
|   |-- Dockerfile
|   `-- pom.xml
|-- banking-payment-service/
|   |-- src/main/java/
|   |-- src/main/resources/
|   |-- Dockerfile
|   `-- pom.xml
|-- infra/
|   `-- mysql-init/
|       `-- 01-create-databases.sql
|-- docker-compose.yml
|-- .gitignore
`-- README.md
```

## Technology Stack

- Java 17
- Spring Boot 3.4.4
- Spring Cloud 2024.0.1
- Spring Security with JWT
- Spring Cloud Gateway
- Netflix Eureka
- OpenFeign
- Resilience4j
- Spring Data JPA
- Flyway
- MySQL 8.4
- Redis
- Apache Kafka
- Docker Compose

## Runtime Ports

Public endpoints:

- API gateway: `8080`
- Eureka dashboard: `8761`

Internal service ports inside Docker network:

- Payment service: `8081`
- Account service: `8082`

Infrastructure:

- MySQL: `3306`
- Redis: `6379`
- Kafka external listener: `9092`
- Kafka internal listener: `29092`
- ZooKeeper: `2181`

## Databases

MySQL initializes two schemas from [`infra/mysql-init/01-create-databases.sql`](./infra/mysql-init/01-create-databases.sql):

- `banking_payment_service_dev`
- `banking_accounts_db`

## Security Model

### JWT

- JWTs are issued by `banking-payment-service`
- `banking-account-service` validates the same JWT secret
- `banking-api-gateway` validates JWT signature and expiry before forwarding protected requests
- Shared secret property:
  - `security.jwt.secret`
  - environment override: `JWT_SECRET`

### Gateway Protection Rules

- Public:
  - `POST /api/payments/auth/register`
  - `POST /api/payments/auth/login`
- Protected:
  - all other `/api/**` routes require `Authorization: Bearer <token>`
- Invalid or expired token response:
  - `401 Unauthorized`
- Rate limit:
  - `10` requests per second per IP
  - excess requests return `429 Too Many Requests`

### Service Roles

#### Payment Service

- `POST /api/auth/**` -> public
- `GET /api/transactions/**` -> `CUSTOMER`, `TELLER`, `ADMIN`
- `POST /api/transactions/**` -> `TELLER`, `ADMIN`
- `DELETE /api/transactions/**` -> `ADMIN`
- `/actuator/**` -> `ADMIN`

#### Account Service

- `GET /api/accounts/**` -> `CUSTOMER`, `TELLER`, `ADMIN`
- `POST /api/accounts/**` -> `TELLER`, `ADMIN`
- `PUT /api/accounts/**` -> `TELLER`, `ADMIN`
- `/actuator/health` and `/actuator/info` -> public

## Service Discovery

### Eureka Registration

The following services register with Eureka on startup:

- `BANKING-API-GATEWAY`
- `BANKING-PAYMENT-SERVICE`
- `BANKING-ACCOUNT-SERVICE`

Dashboard:

```text
http://localhost:8761
```

### Feign Discovery

`banking-payment-service` resolves `banking-account-service` by service name through Eureka. There is no hardcoded account-service URL in the payment application config anymore.

## API Summary

All client traffic should go through the gateway.

### Authentication

Base URL: `http://localhost:8080`

- `POST /api/payments/auth/register`
- `POST /api/payments/auth/login`

Example register payload:

```json
{
  "username": "teller1",
  "password": "Password123",
  "role": "TELLER"
}
```

Example login payload:

```json
{
  "username": "teller1",
  "password": "Password123"
}
```

### Accounts

Base URL: `http://localhost:8080`

- `GET /api/accounts/{accountNumber}`
- `POST /api/accounts`
- `PUT /api/accounts/{accountNumber}/balance`

Example create account payload:

```json
{
  "accountNumber": "ACC2001",
  "accountHolderName": "John Smith",
  "balance": 2500.00,
  "status": "ACTIVE"
}
```

Example balance update payload:

```json
{
  "amount": -100.00
}
```

### Payments and Transactions

Base URL: `http://localhost:8080`

- `POST /api/payments/transactions`
- `GET /api/payments/transactions`
- `GET /api/payments/transactions/{id}`
- `GET /api/payments/transactions/account/{accountNumber}`
- `DELETE /api/payments/transactions/{id}`

Example transaction payload:

```json
{
  "accountNumber": "ACC1001",
  "amount": 100.00,
  "type": "DEBIT"
}
```

Recommended request headers:

```text
Authorization: Bearer <jwt>
Idempotency-Key: <valid UUID>
X-Correlation-ID: <optional client value>
```

## Feign and Resilience

The payment service calls the account service through `AccountServiceClient` using the service name `banking-account-service`.

### Account Validation Before Transaction Processing

Before processing a transaction, the payment service:

1. Fetches the account through Feign
2. Verifies the account exists
3. Verifies the account is active
4. Verifies sufficient funds for debits

Failure behavior:

- missing account -> business exception
- insufficient balance -> `InsufficientFundsException`
- unavailable account service -> fallback account with `status = UNKNOWN`, then `503`

### Retry

- Retry name: `accountService`
- Total attempts: `3`
  - initial call + `2` retries
- Wait duration: `1s`

### Circuit Breaker

- Circuit breaker name: `accountService`
- Opens after `3` recorded failures
- Open-state wait duration: `5s`
- Half-open permitted calls: `2`
- Automatic transition to half-open: enabled

### Logged State Changes

Payment service logs circuit transitions:

- `CLOSED -> OPEN`
- `OPEN -> HALF_OPEN`
- `HALF_OPEN -> CLOSED`

## Correlation IDs

### Gateway Behavior

- Uses incoming `X-Correlation-ID` if present
- Generates a UUID if not present
- Adds `X-Correlation-ID` to forwarded requests
- Adds `X-Correlation-ID` to responses

### Downstream Logging

Both payment and account services log incoming requests with the correlation ID in the log pattern and request log entry.

## Quick Start

### Prerequisites

- Java 17
- Maven 3.9+
- Docker Desktop or Docker Engine with Compose support

### Run with Docker Compose

From the repository root:

```powershell
docker compose up -d --build
```

Check runtime state:

```powershell
docker compose ps -a
```

Stop everything:

```powershell
docker compose down
```

Remove containers and volumes:

```powershell
docker compose down -v
```

### Build Services Locally

```powershell
mvn -q -DskipTests -f banking-eureka-server\pom.xml package
mvn -q -DskipTests -f banking-api-gateway\pom.xml package
mvn -q -DskipTests -f banking-account-service\pom.xml package
mvn -q -DskipTests -f banking-payment-service\pom.xml package
```

## Functional Validation Guide

### 1. Start the platform

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

### 3. Register and log in through the gateway

Register:

```powershell
$register = @{
  username = "teller1"
  password = "Password123"
  role     = "TELLER"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/payments/auth/register" `
  -ContentType "application/json" `
  -Body $register
```

Login:

```powershell
$login = @{
  username = "teller1"
  password = "Password123"
} | ConvertTo-Json

$auth = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/payments/auth/login" `
  -ContentType "application/json" `
  -Body $login

$token = $auth.token
```

### 4. Normal gateway flow

Get an account through the gateway:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/accounts/ACC1001" `
  -Headers @{ Authorization = "Bearer $token" }
```

Create a transaction through the gateway:

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
    Authorization   = "Bearer $token"
    "Idempotency-Key" = "77777777-7777-7777-7777-777777777777"
    "X-Correlation-ID" = "corr-demo-001"
  } `
  -ContentType "application/json" `
  -Body $tx
```

Expected result:

- gateway returns `201`
- payment service resolves account service through Eureka
- account balance changes according to transaction type
- logs in gateway, payment, and account services show the same correlation ID

### 5. Invalid JWT rejection

```powershell
curl.exe -i -H "Authorization: Bearer invalid-token" http://localhost:8080/api/accounts/ACC1001
```

Expected result:

- gateway returns `401`
- request does not reach the account service

### 6. Rate limit verification

A real burst test must hit a routed API path, not the gateway actuator endpoint.

Example PowerShell burst:

```powershell
Add-Type -AssemblyName System.Net.Http

$login = '{"username":"teller1","password":"Password123"}'
$loginClient = New-Object System.Net.Http.HttpClient
$loginContent = New-Object System.Net.Http.StringContent($login, [System.Text.Encoding]::UTF8, 'application/json')
$loginResponse = $loginClient.PostAsync('http://localhost:8080/api/payments/auth/login', $loginContent).Result
$token = ($loginResponse.Content.ReadAsStringAsync().Result | ConvertFrom-Json).token

$client = New-Object System.Net.Http.HttpClient
$client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer', $token)
$tasks = for ($i = 0; $i -lt 12; $i++) { $client.GetAsync('http://localhost:8080/api/accounts/ACC1001') }
[System.Threading.Tasks.Task]::WaitAll($tasks)
$tasks | ForEach-Object { [int]$_.Result.StatusCode }
```

Expected result:

- at most `10` requests return `200`
- excess requests return `429`
- gateway logs `Rate limit exceeded for ip=...`

### 7. Account service down and circuit breaker behavior

Stop account service:

```powershell
docker compose stop account-service
```

Send multiple transaction requests through the gateway. Expected behavior:

- first request is delayed because retries run
- later requests fail quickly once the breaker is open
- payment service responds with `503`

Check logs:

```powershell
docker compose logs payment-service --since 2m
```

Look for:

- fallback log entries
- `Circuit breaker accountService state changed from CLOSED to OPEN`

### 8. Account service recovery

Restart account service:

```powershell
docker compose start account-service
```

Wait slightly more than `5` seconds, then send another transaction through the gateway.

Expected behavior:

- breaker moves `OPEN -> HALF_OPEN`
- successful downstream call closes the breaker
- transaction succeeds again

Check logs:

```powershell
docker compose logs payment-service --since 2m
```

Look for:

- `OPEN -> HALF_OPEN`
- `HALF_OPEN -> CLOSED`

## Verified Local Result

Verified in Docker on `March 22, 2026`:

- gateway health endpoint returned `200`
- Eureka dashboard at `http://localhost:8761` showed all three runtime clients as `UP`
- gateway login succeeded and returned a JWT
- `GET /api/accounts/ACC1001` through the gateway returned `200`
- `POST /api/payments/transactions` through the gateway returned `201`
- invalid JWT on a protected gateway route returned `401`
- a 12-request concurrent burst against `/api/accounts/ACC1001` returned both `200` and `429`
- gateway, payment, and account logs all carried the same correlation IDs during routed requests

## Operational Notes

- Use only the root Compose file
- Client traffic should go through the gateway, not directly to internal services
- Payment and account services are exposed only inside the Docker network in the Compose setup
- Both business services run with `prod` profile in Docker
- Local development defaults to `dev`
- MySQL 8.4 may produce a Flyway compatibility warning, but startup completes
- Kafka and ZooKeeper are part of the existing payment-service infrastructure requirements
- If Kafka fails after an unclean restart with a stale ZooKeeper broker registration, restart `zookeeper` first and then start `kafka`

## Maintenance Standard

Keep this README current whenever the project changes.

Update this document when any of the following change:

- service names
- ports
- environment variables
- Docker workflow
- database names
- JWT and auth behavior
- route mappings
- gateway filters
- resilience settings
- test instructions
- service discovery behavior

When updating in future, keep the document professional:

- describe the actual current behavior, not planned behavior
- prefer repository-root commands unless there is a clear reason not to
- keep one source of truth for infrastructure instructions
- include request examples for any externally used endpoint
- record important operational caveats and recovery steps
- update the verification section when behavior or topology changes
