# Banking Project Spring Boot

Professional reference documentation for the multi-service banking example in this repository.

## Overview

This repository contains two Spring Boot services that run together as a small banking platform:

- `banking-payment-service`
  - Port: `8081`
  - Handles authentication and transaction processing
  - Uses MySQL, Redis, Kafka, OpenFeign, and Resilience4j
- `banking-account-service`
  - Port: `8082`
  - Handles account lookup, account creation, and balance updates
  - Uses its own MySQL schema and validates JWTs with the same secret as the payment service

The repository has one root Docker Compose file:

- [`docker-compose.yml`](./docker-compose.yml)

## Architecture

### Services

#### Payment Service

Responsibilities:

- User registration and login
- JWT token generation
- Transaction create, list, fetch, and delete flows
- Fraud and sanctions checks
- Kafka event publishing and consumption
- Account validation and balance updates through Feign

Primary path:

1. Client authenticates with `banking-payment-service`
2. Client sends transaction request with JWT
3. Payment service calls account service through Feign
4. Account status and balance are validated
5. Transaction is persisted
6. Async fraud and sanctions checks run
7. Account balance is updated remotely if the transaction succeeds

#### Account Service

Responsibilities:

- Account persistence
- Account lookup by account number
- Account creation
- Balance updates
- JWT validation for protected APIs

## Project Structure

```text
banking-project-springboot/
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
- Spring Security with JWT
- Spring Data JPA
- MySQL 8.4
- Flyway
- Redis
- Apache Kafka
- OpenFeign
- Resilience4j
- Docker Compose

## Runtime Ports

- Payment service: `8081`
- Account service: `8082`
- MySQL: `3306`
- Redis: `6379`
- Kafka: `9092`
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
- Secret property:
  - `security.jwt.secret`
  - environment override: `JWT_SECRET`

### Roles

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

## API Summary

### Authentication

Base URL: `http://localhost:8081`

- `POST /api/auth/register`
- `POST /api/auth/login`

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

### Account Service

Base URL: `http://localhost:8082`

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

### Payment Service

Base URL: `http://localhost:8081`

- `POST /api/transactions`
- `GET /api/transactions`
- `GET /api/transactions/{id}`
- `GET /api/transactions/account/{accountNumber}`
- `DELETE /api/transactions/{id}`

Example transaction payload:

```json
{
  "accountNumber": "ACC1001",
  "amount": 100.00,
  "type": "DEBIT"
}
```

Recommended header for transaction creation:

```text
Idempotency-Key: <valid UUID>
```

## Feign and Resilience

The payment service calls the account service through `AccountServiceClient`.

### Retry

- Resilience4j retry name: `accountService`
- Total attempts: `3`
  - initial call + `2` retries
- Wait duration: `1s`

### Circuit Breaker

- Resilience4j circuit breaker name: `accountService`
- Opens after `3` recorded failures
- Open-state wait duration: `5s`
- Half-open permitted calls: `2`
- Automatic transition to half-open: enabled

### Fallback

If the account service is unavailable, the fallback returns a default account object with:

- `status = UNKNOWN`

The payment service then converts that situation into a `503 Service Unavailable` business response.

### Logged State Changes

Circuit transitions are logged by the payment service:

- `CLOSED -> OPEN`
- `OPEN -> HALF_OPEN`
- `HALF_OPEN -> CLOSED`

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

Stop everything:

```powershell
docker compose down
```

Stop and remove volumes:

```powershell
docker compose down -v
```

### Build Services Locally

```powershell
mvn -q -DskipTests -f banking-account-service\pom.xml package
mvn -q -DskipTests -f banking-payment-service\pom.xml package
```

## Functional Validation Guide

### 1. Start the platform

```powershell
docker compose up -d --build
```

### 2. Register and log in a teller

Register:

```powershell
$body = @{
  username = "teller1"
  password = "Password123"
  role     = "TELLER"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/api/auth/register" `
  -ContentType "application/json" `
  -Body $body
```

Login:

```powershell
$login = @{
  username = "teller1"
  password = "Password123"
} | ConvertTo-Json

$auth = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/api/auth/login" `
  -ContentType "application/json" `
  -Body $login

$token = $auth.token
```

### 3. Normal flow

Create a transaction:

```powershell
$tx = @{
  accountNumber = "ACC1001"
  amount        = 100.00
  type          = "DEBIT"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/api/transactions" `
  -Headers @{
    Authorization = "Bearer $token"
    "Idempotency-Key" = "77777777-7777-7777-7777-777777777777"
  } `
  -ContentType "application/json" `
  -Body $tx
```

Verify account balance:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8082/api/accounts/ACC1001" `
  -Headers @{ Authorization = "Bearer $token" }
```

### 4. Account service down

Stop account service:

```powershell
docker compose stop account-service
```

Send multiple transaction requests. Expected behavior:

- First request is delayed because retries run
- Later requests fail quickly once the breaker is open
- Payment service responds with `503`

Check logs:

```powershell
docker compose logs payment-service --since 2m
```

Look for:

- fallback log entries
- `Circuit breaker accountService state changed from CLOSED to OPEN`

### 5. Account service recovery

Restart account service:

```powershell
docker compose start account-service
```

Wait slightly more than `5` seconds, then send another transaction.

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

## Operational Notes

- Use the root Compose file only
- Both services are configured to use `prod` profile in Docker
- Local development defaults to `dev`
- MySQL 8.4 is used; Flyway may log a compatibility warning, but startup completes
- Redis is used for idempotency support
- Kafka topics are created by the payment service

## Maintenance Standard

Keep this README current whenever the project changes.

Update this document when any of the following change:

- service names
- ports
- environment variables
- Docker workflow
- database names
- JWT/auth behavior
- endpoint contracts
- resilience settings
- test instructions

When updating in future, keep the document professional:

- describe the actual current behavior, not planned behavior
- prefer repository-root commands unless there is a clear reason not to
- keep one source of truth for infrastructure instructions
- include request examples for any new externally used endpoint
- record important operational caveats and recovery steps

## Recommended Future README Updates

Add these sections if the project grows:

- API contract tables by service
- sequence diagrams for transaction flow
- troubleshooting playbook
- release notes / change log
- test strategy and automation commands
- environment-specific deployment notes
