# API Gateway

API Gateway is the single public entry point for the e-commerce microservices system.

This service is responsible for routing incoming requests to the correct backend service, applying global gateway concerns such as CORS, request correlation, request logging, timeout handling, and returning consistent gateway-level errors.

This version is **standalone complete**. Authentication enforcement, service discovery, circuit breaker/retry policies, rate limiting, and service-to-service business integrations are intentionally left for the next integration phase.

---

## Port

```text
8080
```

---

## Current Microservice Routes

```text
/api/products/**    → product-service   → http://localhost:8081
/api/categories/**  → product-service   → http://localhost:8081

/api/inventory/**   → inventory-service → http://localhost:8082

/api/auth/**        → auth-service      → http://localhost:8083
/api/admin/**       → auth-service      → http://localhost:8083

/api/orders/**      → order-service     → http://localhost:8084

/api/payments/**    → payment-service   → http://localhost:8085
```

The request path is forwarded unchanged because the downstream services already expose the same `/api/...` paths.

Example:

```text
Client:
GET http://localhost:8080/api/products

Gateway forwards to:
GET http://localhost:8081/api/products
```



---


## CORS

Global CORS configuration is enabled for gateway routes.

Default allowed local origins:

```text
http://localhost:3000
http://localhost:5173
```

Allowed methods:

```text
GET
POST
PUT
PATCH
DELETE
OPTIONS
```

---

## Timeout Handling

Global downstream timeouts are configured.

```text
Connect timeout  → 2 seconds
Response timeout → 5 seconds
```

### Connection Failure

If a downstream service is not running or cannot be reached:

```text
503 Service Unavailable
```

Example:

```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Downstream service is unavailable",
  "path": "/api/products"
}
```

### Response Timeout

If the downstream service accepts the connection but does not respond before the configured timeout:

```text
504 Gateway Timeout
```

---

## Gateway Error Handling

Gateway-level failures are handled separately from normal downstream service responses.

Expected behavior:

```text
Unknown gateway route          → 404 Not Found
Downstream service unavailable → 503 Service Unavailable
Downstream response timeout    → 504 Gateway Timeout
Unexpected gateway failure     → 502 Bad Gateway
```

Normal downstream responses are passed through without changing their business HTTP status.

For example:

```text
Product Service returns 404 → Gateway returns 404
Order Service returns 409   → Gateway returns 409
Auth Service returns 401    → Gateway returns 401
Auth Service returns 403    → Gateway returns 403
```

---

## Authorization Header Forwarding

The gateway currently forwards headers, including:

```text
Authorization: Bearer <token>
```

Example:

```text
Client
  ↓
Authorization: Bearer ey...
  ↓
API Gateway
  ↓
Auth Service / downstream service
```

The gateway does **not yet** validate the JWT itself.

JWT validation and route-level authorization will be added during the integration/security phase.

---

## Actuator

Available endpoints:

```http
GET http://localhost:8080/actuator/health
GET http://localhost:8080/actuator/gateway/routes
GET http://localhost:8080/actuator/metrics
```

### Health

```http
GET /actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

### Route Inspection

```http
GET /actuator/gateway/routes
```

This can be used to confirm that all gateway routes were loaded.

---


## Architecture at This Stage

```text
                         ┌──────────────────┐
                         │      Client      │
                         └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │   API Gateway    │
                         │      :8080       │
                         └────────┬─────────┘
                                  │
           ┌──────────────┬───────┼───────┬──────────────┐
           │              │       │       │              │
           ▼              ▼       ▼       ▼              ▼
     Product          Inventory   Auth    Order        Payment
      :8081             :8082     :8083   :8084         :8085
```

