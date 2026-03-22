# Banking Project Spring Boot

Production-style banking microservices platform built with Spring Boot, Spring Cloud, React, Tailwind CSS, Kafka, MySQL, Redis, Prometheus, and Grafana.

This repository contains a complete end-to-end sample system with:

- service discovery through Eureka
- gateway routing, JWT validation, correlation IDs, and rate limiting
- account and payment microservices
- Kafka-based event publishing and audit logging
- resilience patterns with retry and circuit breaker
- observability with Micrometer, Prometheus, and Grafana
- Dockerized runtime for the full stack
- React frontend for operator workflows

## Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Services](#services)
- [Frontend](#frontend)
- [Tech Stack](#tech-stack)
- [Tools And Libraries](#tools-and-libraries)
- [Security](#security)
- [Messaging And Data Flow](#messaging-and-data-flow)
- [Observability](#observability)
- [Testing And Quality](#testing-and-quality)
- [Project Structure](#project-structure)
- [Ports](#ports)
- [Environment Variables](#environment-variables)
- [Quick Start](#quick-start)
- [Frontend Usage Guide](#frontend-usage-guide)
- [API Guide](#api-guide)
- [Operations](#operations)
- [Troubleshooting](#troubleshooting)
- [Maintenance Notes](#maintenance-notes)

## Overview

The platform models a simple banking workflow:

- users register and log in through the payment service
- all external traffic goes through the API gateway
- accounts are managed by `banking-account-service`
- transactions are handled by `banking-payment-service`
- transaction events are published to Kafka
- audit events are consumed and persisted
- service discovery is handled by Eureka
- dashboards and metrics are available through Prometheus and Grafana

The frontend is a lightweight operator dashboard for:

- user registration
- login
- account creation
- account lookup
- transaction creation
- recent transaction viewing
- gateway health visibility

## Architecture

```text
Browser
  |
  v
banking-frontend (Nginx + React)
  |
  v
banking-api-gateway
  |
  +--> banking-payment-service ----> MySQL (banking_payments_db)
  |           |                     Redis
  |           |
  |           +--> Kafka topics --> audit and notification consumers
  |           |
  |           +--> Feign + Eureka --> banking-account-service
  |
  +--> banking-account-service ----> MySQL (banking_accounts_db)
  |
  v
banking-eureka-server

Prometheus <--- metrics from gateway, payment, account, eureka
Grafana    <--- dashboards from Prometheus
```

## Services

### `banking-frontend`

- React + Tailwind CSS frontend
- served by Nginx in Docker
- public entry point for browser users
- proxies `/api/**` and `/actuator/**` to the API gateway

### `banking-api-gateway`

- external API entry point
- routes requests to backend services
- validates JWTs before forwarding protected requests
- enforces rate limiting per IP
- creates or propagates correlation IDs

### `banking-eureka-server`

- service registry for internal service discovery
- used by gateway and Feign clients

### `banking-payment-service`

- authentication and transaction service
- user registration and login
- transaction creation and lookup
- Kafka event publishing
- audit event consumption
- Redis-backed support components
- Feign client integration with account service
- Resilience4j retry and circuit breaker

### `banking-account-service`

- account creation and lookup
- balance updates
- independent MySQL schema
- JWT validation using the shared secret

### Infrastructure Services

- `mysql`
- `redis`
- `zookeeper`
- `kafka`
- `prometheus`
- `grafana`

## Frontend

The frontend is part of the platform and now runs through Docker with the rest of the stack.

Main URL:

```text
http://localhost:5173
```

Main frontend capabilities:

- register a user
- log in and store the JWT in browser local storage
- create an account
- load an account by account number
- create debit and credit transactions
- show recent transactions for the selected account
- show gateway health status

UI sections:

- `Authentication desk`
- `Lookup and inspect`
- `Create a fresh account`
- `Post a transaction`
- `Recent transactions`
- `Gateway health snapshot`

## Tech Stack

### Backend

- Java 17
- Spring Boot 3
- Spring Cloud
- Spring Security
- Spring Data JPA
- Flyway
- OpenFeign
- Resilience4j
- Micrometer

### Frontend

- React 18
- Vite
- Tailwind CSS

### Data And Messaging

- MySQL 8
- Redis 7
- Apache Kafka
- ZooKeeper

### Platform And Monitoring

- Docker
- Docker Compose
- Prometheus
- Grafana
- Nginx

### Testing

- JUnit 5
- Mockito
- Spring Boot Test
- MockMvc
- H2
- JaCoCo

## Tools And Libraries

This section lists the main tools and libraries used across the repository.

### Spring And Java Libraries

- `spring-boot-starter-web`
- `spring-boot-starter-security`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-actuator`
- `spring-cloud-starter-netflix-eureka-server`
- `spring-cloud-starter-netflix-eureka-client`
- `spring-cloud-starter-gateway`
- `spring-cloud-starter-openfeign`
- `resilience4j-spring-boot`
- `micrometer-registry-prometheus`
- JWT support libraries

### Frontend Libraries

- `react`
- `react-dom`
- `vite`
- `tailwindcss`
- `postcss`
- `autoprefixer`

### Operational Tooling

- Nginx for serving the built frontend
- Prometheus for scraping metrics
- Grafana for dashboards
- Docker Compose for orchestration
- Maven for Java builds
- npm for frontend dependency management

## Security

Security is enforced primarily at the gateway and service layer.

### JWT

- login returns a JWT
- protected routes require `Authorization: Bearer <token>`
- gateway validates token signature and expiry
- backend services also validate JWT where required

### Public Routes

- `POST /api/auth/register`
- `POST /api/auth/login`

### Protected Routes

- all other `/api/**` routes

### Role Rules

- `TELLER`
  - can create transactions
  - can view transactions
- `ADMIN`
  - can create transactions
  - can view transactions
  - can access protected actuator endpoints on the payment service
- `CUSTOMER`
  - can view transactions
  - cannot create transactions

### Gateway Protections

- JWT validation
- rate limiting: `10` requests per second per IP
- correlation ID propagation

### Logging Compliance

- passwords must never be logged
- full account numbers must not be logged
- account numbers are masked in service logs
- correlation IDs are added to logs for traceability

## Messaging And Data Flow

### Transaction Flow

1. user submits transaction through the frontend
2. frontend calls the gateway
3. gateway routes to `banking-payment-service`
4. payment service checks JWT and role
5. payment service uses Feign to verify the account via `banking-account-service`
6. payment service validates active status and available balance
7. payment service updates balance through account service
8. payment service stores transaction state
9. payment service publishes Kafka events
10. audit consumers persist audit entries

### Kafka Usage

Kafka is used for:

- transaction initiated events
- transaction completed events
- audit events
- downstream notification or audit consumers

## Observability

Observability is implemented across the services.

### Logging

- Logback configuration per service
- structured format with:
  - timestamp
  - level
  - correlation ID
  - service
  - class
  - message

### Metrics

Exposed through:

```text
/actuator/prometheus
```

Important metrics include:

- transaction counters
- failed transaction counters
- transaction processing timer
- Kafka consumer lag gauge
- JVM metrics
- HTTP request metrics
- Resilience4j circuit breaker state metrics

### Health

Exposed through:

```text
/actuator/health
```

Custom health indicators:

- database health
- Kafka health

### Dashboards

Grafana dashboard tracks:

- transaction success rate
- transaction processing time
- JVM memory usage
- Kafka consumer lag
- circuit breaker state

## Testing And Quality

The payment service includes a strong test suite.

### Unit Tests

- transaction service
- JWT provider
- transaction analyzer

### Integration Tests

- secured endpoint flow with valid JWT
- unauthorized flow
- forbidden flow
- transaction retrieval

### Repository Tests

- JPA repository methods
- custom query behavior

### Coverage

- JaCoCo report generation
- minimum target of `80%`

## Project Structure

```text
banking-project-springboot/
|-- banking-account-service/
|   |-- src/main/java/
|   |-- src/main/resources/
|   |-- Dockerfile
|   `-- pom.xml
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
|-- banking-payment-service/
|   |-- src/main/java/
|   |-- src/main/resources/
|   |-- Dockerfile
|   `-- pom.xml
|-- banking-frontend/
|   |-- src/
|   |-- public/
|   |-- Dockerfile
|   |-- nginx.conf
|   |-- package.json
|   `-- vite.config.js
|-- infra/
|   |-- grafana/
|   |-- mysql-init/
|   `-- prometheus/
|-- docker-compose.yml
|-- .env
|-- .gitignore
`-- README.md
```

## Ports

### Public Ports

- Frontend: `5173`
- API Gateway: `8080`
- Eureka Dashboard: `8761`
- Grafana: `3000`
- Prometheus: `9090`
- MySQL: `3306`
- Redis: `6379`
- Kafka: `9092`
- ZooKeeper: `2181`

### Internal Service Ports

- Payment service: `8081`
- Account service: `8082`
- Kafka internal listener: `29092`

## Environment Variables

The root `.env` file is the main source of runtime configuration for Docker.

Important variables:

- `SPRING_PROFILES_ACTIVE`
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_PORT`
- `DB_HOST`
- `DB_PORT`
- `DB_USERNAME`
- `DB_PASSWORD`
- `PAYMENT_DB_NAME`
- `ACCOUNT_DB_NAME`
- `JWT_SECRET`
- `REDIS_HOST`
- `REDIS_PORT`
- `ZOOKEEPER_PORT`
- `KAFKA_PORT`
- `KAFKA_INTERNAL_PORT`
- `KAFKA_BOOTSTRAP_SERVERS`
- `EUREKA_SERVER_PORT`
- `EUREKA_SERVER_URL`
- `GATEWAY_PORT`
- `FRONTEND_PORT`
- `PAYMENT_SERVICE_PORT`
- `ACCOUNT_SERVICE_PORT`
- `PROMETHEUS_PORT`
- `GRAFANA_PORT`
- `GRAFANA_ADMIN_USER`
- `GRAFANA_ADMIN_PASSWORD`

## Quick Start

### Prerequisites

Install:

- Docker Desktop
- Java 17
- Maven
- Node.js 20 or later
- npm

### Start Everything With Docker

From the repository root:

```powershell
docker compose up -d --build
```

### Main URLs

- Frontend: `http://localhost:5173`
- Gateway: `http://localhost:8080`
- Eureka: `http://localhost:8761`
- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`

### Check Status

```powershell
docker compose ps -a
```

### Stop The Stack

```powershell
docker compose down
```

### Reset All Data

```powershell
docker compose down -v
```

## Frontend Usage Guide

The recommended way to use the system is through:

```text
http://localhost:5173
```

### First-Time Operator Flow

1. Start the full stack:

```powershell
docker compose up -d --build
```

2. Open the frontend:

```text
http://localhost:5173
```

3. In `Authentication desk`, register a user.

Recommended initial role:

- `TELLER`

Example:

```text
Username: teller1
Password: Password123
Role: TELLER
```

4. Log in with the same credentials in `Open session`.

Expected result:

- the UI shows the signed-in username
- the role is displayed
- account and transaction features become available

5. In `Create a fresh account`, create an account.

Example:

```text
Account number: ACC1001
Account holder name: Demo Customer
Opening balance: 5000.00
Status: ACTIVE
```

6. Use `Lookup and inspect` to load an existing account later.

7. Use `Post a transaction` to create a debit or credit.

Example:

```text
Amount: 100.00
Type: DEBIT
```

8. Check `Recent transactions` for the refreshed ledger list.

9. Check `Gateway health snapshot` to confirm gateway readiness.

### Notes

- the frontend stores the JWT in browser local storage
- the frontend talks only to the gateway
- account and transaction actions require login
- transactions require a currently loaded account
- customer-role users can view but not create transactions

## API Guide

All client-facing APIs should be accessed through the gateway.

### Authentication

#### Register

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

#### Login

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

### Accounts

#### Get Account

```text
GET http://localhost:8080/api/accounts/ACC1001
```

#### Create Account

```text
POST http://localhost:8080/api/accounts
```

Example payload:

```json
{
  "accountNumber": "ACC1001",
  "accountHolderName": "Demo Customer",
  "balance": 5000.00,
  "status": "ACTIVE"
}
```

### Transactions

#### Create Transaction

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

#### Get Transactions By Account

```text
GET http://localhost:8080/api/payments/transactions/account/ACC1001
```

### Recommended Headers

```text
Authorization: Bearer <jwt>
Idempotency-Key: <uuid>
X-Correlation-ID: <optional client value>
```

## Operations

### Eureka Dashboard

Open:

```text
http://localhost:8761
```

Expected registered services:

- `BANKING-API-GATEWAY`
- `BANKING-PAYMENT-SERVICE`
- `BANKING-ACCOUNT-SERVICE`

### Grafana

Open:

```text
http://localhost:3000
```

Default credentials come from `.env`:

- username: `admin`
- password: `banking-grafana-admin-password`

### Prometheus

Open:

```text
http://localhost:9090
```

### Useful Commands

Check containers:

```powershell
docker compose ps -a
```

Check payment logs:

```powershell
docker compose logs payment-service --tail=200
```

Check gateway logs:

```powershell
docker compose logs api-gateway --tail=200
```

Check frontend logs:

```powershell
docker compose logs frontend --tail=200
```

Query audit rows:

```powershell
docker compose exec -T mysql mysql -uroot -p<MYSQL_ROOT_PASSWORD> -D banking_payments_db -e "SELECT id, transaction_id, account_number, event_type, topic_name FROM audit_logs ORDER BY id DESC LIMIT 10;"
```

## Troubleshooting

### `localhost:5173` not opening

- make sure the `frontend` container is running
- run `docker compose ps -a`
- run `docker compose up -d --build frontend`

### Login works but transaction creation fails with `403`

- your logged-in role is probably `CUSTOMER`
- use `TELLER` or `ADMIN` for transaction creation

### Transaction creation fails with `401`

- the JWT may be missing or expired
- log in again from the frontend

### Transaction creation fails with `503`

- account service may be unavailable
- check:
  - `docker compose ps -a`
  - `docker compose logs payment-service --tail=200`
  - `docker compose logs account-service --tail=200`

### Kafka fails after restart

- restart `zookeeper` first
- then start `kafka`

### Need a clean reset

```powershell
docker compose down -v
docker compose up -d --build
```

## Maintenance Notes

This README should remain the single source of truth for:

- architecture
- ports
- environment variables
- service responsibilities
- frontend usage
- auth behavior
- gateway routes
- observability setup
- Docker commands
- troubleshooting steps

Whenever the project changes, update this document to reflect actual current behavior, not intended behavior.
