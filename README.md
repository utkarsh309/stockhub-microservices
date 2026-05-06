# StockHub — Backend Microservices

**Trainee:** Utkarsh  
**Project:** StockHub Inventory Management System — Spring Boot Microservices Backend  
**Spring Boot Version:** 3.5.13  
**Last Updated:** May 2026  

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Architecture Overview](#architecture-overview)
4. [Microservices Summary](#microservices-summary)
5. [Service Port Map](#service-port-map)
6. [Database Setup](#database-setup)
7. [Getting Started](#getting-started)
8. [Service Details](#service-details)
   - [Eureka Server](#1-eureka-server)
   - [API Gateway](#2-api-gateway)
   - [Auth Service](#3-auth-service)
   - [Product Service](#4-product-service)
   - [Warehouse Service](#5-warehouse-service)
   - [Supplier Service](#6-supplier-service)
   - [Purchase Service](#7-purchase-service)
   - [Movement Service](#8-movement-service)
   - [Alert Service](#9-alert-service)
   - [Report Service](#10-report-service)
9. [Inter-Service Communication](#inter-service-communication)
10. [Messaging — RabbitMQ](#messaging--rabbitmq)
11. [Caching — Redis](#caching--redis)
12. [Security & JWT](#security--jwt)
13. [API Documentation (Swagger)](#api-documentation-swagger)
14. [Running Tests](#running-tests)
15. [Configuration Reference](#configuration-reference)
16. [Startup Order](#startup-order)

---

## Project Overview

StockHub is a full-stack inventory management system built on a **microservices architecture**. The backend is composed of **10 independent Spring Boot services** that communicate via REST (OpenFeign), an event queue (RabbitMQ), and are discovered through a Eureka registry. All external traffic enters through a single **API Gateway** on port `8080`, which handles JWT authentication and routes requests to the correct downstream service using load-balanced service names (`lb://`).

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Spring Boot | 3.5.13 | Core framework for all microservices |
| Spring Cloud Netflix Eureka | Latest | Service discovery and registry |
| Spring Cloud Gateway | Latest | API Gateway with reactive WebFlux |
| Spring Cloud OpenFeign | Latest | Declarative HTTP client for inter-service calls |
| Spring Security | Latest | Authentication in auth-service |
| Spring Data JPA | Latest | ORM for all data services |
| Spring Data Redis | Latest | Caching in product-service and warehouse-service |
| Spring AMQP / RabbitMQ | Latest | Async event messaging for alerts |
| Spring Mail | Latest | Email notifications for critical alerts |
| MySQL | 8.x | Primary database for all services |
| Redis | Latest | In-memory cache |
| RabbitMQ | Latest | Message broker |
| JJWT | 0.11.5 | JWT token creation and validation |
| Resilience4j | Latest | Circuit breaker for Feign clients |
| SpringDoc OpenAPI | 2.8.x | Swagger UI documentation |
| Lombok | Latest | Boilerplate reduction |
| JUnit / Mockito | Latest | Unit and integration testing |

---

## Architecture Overview

```
                          ┌─────────────────────────────┐
                          │       Angular Frontend        │
                          │      localhost:4200           │
                          └──────────────┬───────────────┘
                                         │ HTTP
                                         ▼
                          ┌─────────────────────────────┐
                          │         API Gateway           │
                          │       localhost:8080          │
                          │   JWT Filter + Route Rules    │
                          └────────────┬────────────────┘
                                       │ lb:// (Eureka)
              ┌────────────────────────┼────────────────────────┐
              │                        │                        │
    ┌─────────▼──────┐    ┌────────────▼──────┐    ┌──────────▼──────┐
    │  auth-service  │    │  product-service  │    │warehouse-service │
    │   port: 8081   │    │   port: 8082      │    │   port: 8083     │
    │  MySQL + JWT   │    │  MySQL + Redis    │    │  MySQL + Redis   │
    └────────────────┘    └───────────────────┘    └─────────────────┘

    ┌────────────────┐    ┌───────────────────┐    ┌─────────────────┐
    │purchase-service│    │  supplier-service  │    │movement-service │
    │   port: 8084   │    │   port: 8085      │    │   port: 8086    │
    │  MySQL+Feign  │    │      MySQL         │    │     MySQL       │
    └────────────────┘    └───────────────────┘    └─────────────────┘

    ┌────────────────┐    ┌───────────────────┐
    │  alert-service │    │  report-service   │
    │   port: 8087   │    │   port: 8088      │
    │MySQL+RabbitMQ  │    │  MySQL + Feign    │
    │+Feign+Scheduler│    │                   │
    └────────────────┘    └───────────────────┘

              ┌───────────────────────────────────┐
              │          Eureka Server             │
              │         localhost:8761             │
              └───────────────────────────────────┘

              ┌──────────────┐    ┌────────────────┐
              │   RabbitMQ   │    │     Redis       │
              │  port: 5672  │    │  port: 6379    │
              └──────────────┘    └────────────────┘
```

---

## Microservices Summary

| Service | Port | Database | Special Dependencies |
|---|---|---|---|
| `eureka-server` | 8761 | — | — |
| `api-gateway` | 8080 | — | Eureka, JJWT, WebFlux |
| `auth-service` | 8081 | stockhub_auth | Spring Security, JJWT |
| `product-service` | 8082 | stockhub_product | Redis |
| `warehouse-service` | 8083 | stockhub_warehouse | Redis, Feign |
| `purchase-service` | 8084 | stockhub_purchase | Feign (warehouse, supplier, movement) |
| `supplier-service` | 8085 | stockhub_supplier | — |
| `movement-service` | 8086 | stockhub_movement | — |
| `alert-service` | 8087 | stockhub_alert | RabbitMQ, Feign, Spring Mail, Resilience4j, Scheduler |
| `report-service` | 8088 | stockhub_report | Feign |

---

## Service Port Map

```
8761  →  Eureka Dashboard       http://localhost:8761
8080  →  API Gateway            http://localhost:8080
8080  →  Swagger UI (unified)   http://localhost:8080/swagger-ui.html
8081  →  Auth Service
8082  →  Product Service
8083  →  Warehouse Service
8084  →  Purchase Service
8085  →  Supplier Service
8086  →  Movement Service
8087  →  Alert Service
8088  →  Report Service
5672  →  RabbitMQ (AMQP)
15672 →  RabbitMQ Management UI
6379  →  Redis
```

---

## Database Setup

Each service owns its own MySQL schema. Create all databases before starting the services:

```sql
CREATE DATABASE stockhub_auth;
CREATE DATABASE stockhub_product;
CREATE DATABASE stockhub_warehouse;
CREATE DATABASE stockhub_purchase;
CREATE DATABASE stockhub_supplier;
CREATE DATABASE stockhub_movement;
CREATE DATABASE stockhub_alert;
CREATE DATABASE stockhub_report;
```

Default credentials used across all services (configured in `application-dev.properties`):

```
Username: root
Password: ****
Host:     localhost:3306
```

> **Note:** Tables are auto-created on first startup via `spring.jpa.hibernate.ddl-auto=update`. You only need to create the empty databases.

---

## Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **MySQL 8.x** running on port 3306
- **Redis** running on port 6379
- **RabbitMQ** running on port 5672 (default guest/guest credentials)

### Run Each Service

Each service is a standalone Spring Boot application. Run them in the order listed in the [Startup Order](#startup-order) section.

```bash
# Navigate into any service folder and run:
cd eureka-server
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Or using Maven directly:
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

All services use a `dev` profile. The active config file is `application-dev.properties`.

---

## Service Details

### 1. Eureka Server

**Port:** `8761`
**Role:** Service registry. All other services register here on startup and use it to discover each other by name instead of hardcoded URLs.

**Key config:**
```properties
server.port=8761
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
eureka.server.enable-self-preservation=false
```

**Dashboard:** `http://localhost:8761` — shows all registered services and their instances.

---

### 2. API Gateway

**Port:** `8080`
**Role:** The single entry point for all client requests. Validates JWT tokens, enforces CORS, and routes traffic to downstream services using Eureka-resolved `lb://` URIs.

**Key components:**

- `GatewayConfig.java` — Defines all route rules with path patterns, HTTP methods, and JWT filter attachment.
- `JwtAuthenticationFilter.java` — A reactive `GatewayFilter` that intercepts every request, extracts the `Authorization: Bearer <token>` header, validates the JWT using `JwtUtil`, and rejects unauthorized requests with `401`.
- `JwtUtil.java` — Parses and validates JWT tokens. Must share the same secret key as `auth-service`.

**CORS configuration:**
```
Allowed Origins: http://localhost:4200
Allowed Methods: GET, POST, PUT, DELETE, OPTIONS
Allowed Headers: *
Allow Credentials: true
```

**Unified Swagger UI:** The gateway aggregates API docs from all services. Access the full documentation at `http://localhost:8080/swagger-ui.html` and switch between services using the dropdown.

---

### 3. Auth Service

**Port:** `8081`
**Database:** `stockhub_auth`
**Role:** User registration, login, JWT generation, and user lifecycle management.

**Key endpoints (`/api/auth`):**

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/login` | Public | Authenticate and receive a JWT token |
| `POST` | `/register` | Public | Register a new user |
| `GET` | `/users` | Admin | List all users |
| `GET` | `/users/{id}` | Authenticated | Get user by ID |
| `PUT` | `/users/{id}/profile` | Authenticated | Update user profile |
| `PUT` | `/users/{id}/activate` | Admin | Activate a user account |
| `PUT` | `/users/{id}/deactivate` | Admin | Deactivate a user account |
| `GET` | `/users/role/{role}` | Internal (Alert Scheduler) | Get users by role |

**JWT Configuration:**
```properties
jwt.secret=stockhub_secret_key_2026_very_long_secure_key
jwt.expiration=28800000   # 8 hours in milliseconds
```

**User Roles:** `ADMIN`, `MANAGER`, `OFFICER`, `STAFF`

**Security:** Uses Spring Security with BCrypt password encoding. On first startup, a default ADMIN user is seeded using `admin.password=admin123`.

**Tests:** `AuthServiceImplTest.java`, `JwtAuthFilterTest.java`, `GlobalExceptionHandlerTest.java`

---

### 4. Product Service

**Port:** `8082`
**Database:** `stockhub_product`
**Role:** Full product catalog management with Redis caching.

**Key endpoints (`/api/products`):**

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/` | Admin, Manager | Create a product |
| `GET` | `/` | All | List all products |
| `GET` | `/{productId}` | All | Get product by ID |
| `GET` | `/sku/{sku}` | All | Get product by SKU |
| `GET` | `/search?name=` | All | Search products by name |
| `GET` | `/category/{category}` | All | Filter by category |
| `PUT` | `/{productId}` | Admin, Manager | Update product |
| `PUT` | `/{productId}/deactivate` | Admin, Manager | Deactivate product |

**Caching:** Product data is cached in Redis. Cache is evicted on create/update/deactivate operations to keep data consistent.

**Tests:** `ProductServiceImplTest.java`

---

### 5. Warehouse Service

**Port:** `8083`
**Database:** `stockhub_warehouse`
**Role:** Manages warehouse records and all stock level operations. This service is the single source of truth for inventory quantities.

**Warehouse endpoints (`/api/warehouses`):**

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/` | Admin | Create warehouse |
| `GET` | `/` | All | List all warehouses |
| `GET` | `/{warehouseId}` | All | Get warehouse by ID |
| `PUT` | `/{warehouseId}` | Admin | Update warehouse |

**Stock endpoints (`/api/stock`):**

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/add` | Staff, Manager, Admin | Add stock to a warehouse |
| `PUT` | `/deduct` | Staff, Manager, Admin | Deduct stock from a warehouse |
| `POST` | `/reserve` | Manager, Officer, Admin | Reserve stock for a purchase order |
| `POST` | `/release` | Manager, Officer, Admin | Release reserved stock back to available |
| `POST` | `/transfer` | Staff, Manager, Admin | Transfer stock between warehouses |
| `POST` | `/adjust/add` | Manager, Admin | Adjust stock upward |
| `POST` | `/adjust/deduct` | Manager, Admin | Adjust stock downward |
| `POST` | `/write-off` | Manager, Admin | Write off damaged or lost stock |
| `GET` | `/warehouse/{id}` | All | Get stock levels for a warehouse |
| `GET` | `/{warehouseId}/{productId}` | All | Get stock for a specific product in a warehouse |
| `GET` | `/low-stock` | All | List all items below reorder level |

**Caching:** Warehouse data is cached in Redis. Cache invalidation occurs on all write operations.

**Tests:** `WarehouseServiceImplTest.java`

---

### 6. Supplier Service

**Port:** `8085`
**Database:** `stockhub_supplier`
**Role:** Manages supplier records.

**Key endpoints (`/api/suppliers`):**

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/` | Admin, Officer | Create supplier |
| `GET` | `/` | Admin, Officer | List all suppliers |
| `GET` | `/{supplierId}` | Admin, Officer | Get supplier by ID |
| `PUT` | `/{supplierId}` | Admin, Officer | Update supplier |
| `DELETE` | `/{supplierId}` | Admin | Delete supplier |

**Tests:** `SupplierServiceImplTest.java`, `SupplierIntegrationTest.java` (uses H2 in-memory database)

---

### 7. Purchase Service

**Port:** `8084`
**Database:** `stockhub_purchase`
**Role:** Manages purchase orders and the goods receiving process. Orchestrates cross-service calls via Feign when goods arrive.

**Key endpoints (`/api/purchase-orders`):**

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/` | Admin, Officer | Create a new purchase order |
| `GET` | `/` | All | List all purchase orders |
| `GET` | `/{poId}` | All | Get PO by ID |
| `GET` | `/supplier/{supplierId}` | All | Get POs by supplier |
| `GET` | `/warehouse/{warehouseId}` | All | Get POs by warehouse |
| `GET` | `/status/{status}` | All | Filter POs by status |
| `PUT` | `/{poId}/approve` | Admin, Manager | Approve a PO |
| `PUT` | `/{poId}/cancel` | Admin, Manager | Cancel a PO |
| `POST` | `/{poId}/receive` | Admin, Staff | Receive goods — triggers stock update and movement recording |

**PO Statuses:** `DRAFT` → `PENDING` → `APPROVED` → `RECEIVED` / `CANCELLED`

**Feign Clients used:**
- `WarehouseClient` — Calls `/api/stock/add` to update stock on goods receipt
- `SupplierClient` — Validates supplier existence
- `MovementClient` — Records a `STOCK_IN` movement to `movement-service` after receipt

**Tests:** `PurchaseServiceImplTest.java`, `GlobalExceptionHandlerTest.java`

---

### 8. Movement Service

**Port:** `8086`
**Database:** `stockhub_movement`
**Role:** Append-only log of all stock movements. Records are created by other services (primarily purchase-service) via Feign. This service itself never modifies existing records.

**Key endpoints (`/api/movements`):**

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/` | Internal (Feign) | Record a stock movement |
| `GET` | `/` | All | List all movements |
| `GET` | `/warehouse/{warehouseId}` | All | Movements for a warehouse |
| `GET` | `/product/{productId}` | All | Movements for a product |

**Tests:** `MovementServiceImplTest.java`

---

### 9. Alert Service

**Port:** `8087`
**Database:** `stockhub_alert`
**Role:** The most complex service. Receives alert events from RabbitMQ, runs a scheduled low-stock checker, sends email notifications for critical alerts, and exposes alert management endpoints.

**Key endpoints (`/api/alerts`):**

| Method | Path | Description |
|---|---|---|
| `GET` | `/recipient/{id}` | Get all alerts for a user |
| `GET` | `/unread/{id}` | Get unread alerts for a user |
| `GET` | `/count/{id}` | Get unread alert count for a user |
| `PUT` | `/{alertId}/read` | Mark an alert as read |
| `PUT` | `/read-all/{recipientId}` | Mark all alerts as read |
| `PUT` | `/{alertId}/acknowledge` | Acknowledge an alert |

**Alert Severity Levels:** `INFO`, `WARNING`, `CRITICAL`

**Alert Types:** Defined in `AlertType.java` enum — includes low stock, out of stock, overstock, PO events, and more.

**Scheduler — `AlertScheduler.java`:**
Runs automatically every 60 seconds (with a 5-second initial delay). On each tick it:
1. Fetches all active warehouses via `WarehouseClient` (Feign).
2. For each warehouse, fetches stock levels.
3. Compares available quantity against the product's reorder level via `ProductClient` (Feign).
4. If stock is low or out, publishes an `AlertRequest` to RabbitMQ for async processing.

**RabbitMQ Consumer — `AlertConsumer.java`:**
Listens to `alert.notification.queue`. On receiving a message, calls `AlertService.sendAlert()` which saves the alert to the database and, if severity is `CRITICAL`, sends an email via `EmailService`.

**Circuit Breaker (Resilience4j):**
Feign calls to `warehouse-service`, `product-service`, and `auth-service` are wrapped in circuit breakers with fallback factories. Configuration:
```properties
sliding-window-size=5
failure-rate-threshold=50%
wait-duration-in-open-state=30s
permitted-calls-in-half-open=2
```

**Email Configuration:**
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
app.alert.admin-email=<admin-email>
app.alert.from-email=<sender-email>
```

**Tests:** `AlertServiceImplTest.java`, `AlertSchedulerTest.java`, `GlobalExceptionHandlerTest.java`

---

### 10. Report Service

**Port:** `8088`
**Database:** `stockhub_report`
**Role:** Generates inventory valuation reports and tracks product movement frequency. Pulls data from warehouse-service and product-service via Feign.

**Key endpoints (`/api/reports`):**

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/snapshot` | Admin, Manager | Manually capture a stock valuation snapshot |
| `GET` | `/snapshot?date=` | Admin, Manager | Retrieve snapshot for a specific date |
| `GET` | `/valuation` | Admin, Manager | Total stock value (defaults to today) |
| `GET` | `/valuation?date=` | Admin, Manager | Total stock value on a specific date |
| `GET` | `/valuation/warehouse/{id}` | Admin, Manager | Stock value for a specific warehouse |
| `GET` | `/top-moving` | Admin, Manager | Products with highest movement frequency |
| `GET` | `/slow-moving` | Admin, Manager | Products with low movement activity |
| `GET` | `/dead-stock` | Admin, Manager | Products with zero movement |

**Tests:** `ReportServiceImplTest.java`

---

## Inter-Service Communication

All synchronous service-to-service calls use **Spring Cloud OpenFeign** with Eureka-based load balancing. Services call each other by registered name, not by hardcoded URL.

```java
// Example: purchase-service calling movement-service
@FeignClient(name = "movement-service")
public interface MovementClient {
    @PostMapping("/api/movements")
    Map<String, Object> recordMovement(@RequestBody MovementRequest request);
}
```

**Feign timeout settings** (configured in services that use Feign):
```properties
spring.cloud.openfeign.client.config.default.connect-timeout=5000
spring.cloud.openfeign.client.config.default.read-timeout=5000
```

**Who calls whom:**

| Caller | Calls | Purpose |
|---|---|---|
| `purchase-service` | `warehouse-service` | Add stock on goods receipt |
| `purchase-service` | `supplier-service` | Validate supplier |
| `purchase-service` | `movement-service` | Record STOCK_IN movement |
| `alert-service` | `warehouse-service` | Fetch warehouses and stock for low-stock check |
| `alert-service` | `product-service` | Fetch product reorder levels |
| `alert-service` | `auth-service` | Fetch users by role for alert routing |
| `report-service` | `warehouse-service` | Fetch stock data for valuation |
| `report-service` | `product-service` | Fetch product cost/selling prices |

---

## Messaging — RabbitMQ

The alert pipeline uses RabbitMQ to decouple the scheduler from the alert persistence and email delivery logic.

**Exchange:** `stockhub.alerts` (Direct Exchange)
**Queue:** `alert.notification.queue` (durable — survives RabbitMQ restart)
**Routing Key:** `alert.notification`
**Message Format:** JSON (via `Jackson2JsonMessageConverter`)

**Flow:**
```
AlertScheduler detects low stock
        │
        ▼
AlertPublisher.publish(AlertRequest)
        │
        ▼
RabbitMQ Exchange: stockhub.alerts
        │  routing key: alert.notification
        ▼
Queue: alert.notification.queue
        │
        ▼
AlertConsumer.consume(AlertRequest)
        │
        ▼
AlertService.sendAlert()
  ├── Save alert to MySQL
  └── If CRITICAL → EmailService.sendEmail()
```

**Prerequisites:** RabbitMQ must be running with default credentials (`guest` / `guest`) on `localhost:5672`.

---

## Caching — Redis

Two services use Redis for read-through caching:

**product-service** — Caches product entities. Cache is evicted when a product is created, updated, or deactivated.

**warehouse-service** — Caches warehouse records. Cache is evicted on any write operation (create, update, stock change).

Both services have a `RedisConfig.java` that configures the `RedisTemplate` with Jackson JSON serialization.

**Prerequisites:** Redis must be running on `localhost:6379` with no authentication.

---

## Security & JWT

### How It Works

1. The client sends `POST /api/auth/login` with email and password.
2. `auth-service` validates credentials and returns a JWT token signed with the shared secret.
3. The client includes the token in every subsequent request as `Authorization: Bearer <token>`.
4. The `api-gateway`'s `JwtAuthenticationFilter` intercepts each request, validates the token using `JwtUtil`, and forwards the request to the downstream service if valid. `OPTIONS` (preflight) requests bypass the filter.
5. Downstream services trust the request that has passed through the gateway.

### JWT Details

```
Secret:     stockhub_secret_key_2026_very_long_secure_key
Expiry:     8 hours (28,800,000 ms)
Algorithm:  HS256 (HMAC-SHA256)
```

> **Important:** The `jwt.secret` in `api-gateway` and `auth-service` must be identical. If they differ, the gateway will reject all tokens issued by the auth service.

---

## API Documentation (Swagger)

Swagger UI is available per service and also aggregated at the gateway level.

| Service | Direct Swagger URL |
|---|---|
| Unified (all services) | `http://localhost:8080/swagger-ui.html` |
| Auth Service | `http://localhost:8081/swagger-ui.html` |
| Product Service | `http://localhost:8082/swagger-ui.html` |
| Warehouse Service | `http://localhost:8083/swagger-ui.html` |
| Purchase Service | `http://localhost:8084/swagger-ui.html` |
| Supplier Service | `http://localhost:8085/swagger-ui.html` |
| Movement Service | `http://localhost:8086/swagger-ui.html` |
| Alert Service | `http://localhost:8087/swagger-ui.html` |
| Report Service | `http://localhost:8088/swagger-ui.html` |

The gateway Swagger UI displays a dropdown to switch between all registered services' API docs in one place.

---

## Running Tests

Each service contains JUnit/Mockito unit tests. Run tests for a specific service:

```bash
cd <service-folder>
./mvnw test
```

### Test Coverage Summary

| Service | Test Files |
|---|---|
| `alert-service` | `AlertServiceImplTest`, `AlertSchedulerTest`, `GlobalExceptionHandlerTest` |
| `auth-service` | `AuthServiceImplTest`, `JwtAuthFilterTest`, `GlobalExceptionHandlerTest` |
| `purchase-service` | `PurchaseServiceImplTest`, `GlobalExceptionHandlerTest` |
| `supplier-service` | `SupplierServiceImplTest`, `SupplierIntegrationTest` |
| `product-service` | `ProductServiceImplTest` |
| `warehouse-service` | `WarehouseServiceImplTest` |
| `movement-service` | `MovementServiceImplTest` |
| `report-service` | `ReportServiceImplTest` |

> `supplier-service` uses an **H2 in-memory database** for its integration test so no MySQL setup is needed for that test.

---

## Configuration Reference

Every service has two property files:

- `application.properties` — Sets the active profile: `spring.profiles.active=dev`
- `application-dev.properties` — All actual configuration values

Common properties across all services:

```properties
# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Eureka
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.prefer-ip-address=true

# Swagger
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs

# Logging
logging.level.com.stockhub=DEBUG
```

---

## Startup Order

Services must be started in this order to avoid registration and dependency failures:

```
1. MySQL         (database — must be running)
2. Redis         (cache — must be running)
3. RabbitMQ      (message broker — must be running)
4. eureka-server (service registry — everything registers here)
5. api-gateway   (entry point — discovers services via Eureka)
6. auth-service
7. product-service
8. warehouse-service
9. supplier-service
10. movement-service
11. purchase-service
12. report-service
13. alert-service  (last — depends on all others via Feign)
```

> **Tip:** The `alert-service` has Resilience4j circuit breakers on all its Feign calls. If it starts before other services are ready, the circuit breakers will handle failures gracefully with fallback responses instead of crashing.
