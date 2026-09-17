# Order Service

Order Service is a Spring Boot microservice responsible for managing orders and order items in the e-commerce system.

This version is standalone-complete: it owns its PostgreSQL database and implements order creation, server-side monetary calculations, lifecycle transitions, validation, filtering, pagination, Flyway migrations, health checks, OpenAPI, and automated tests.

## Features

### Order Management

Supports:

- Create order
- Get order by ID
- Get all orders
- Get orders by user ID
- Confirm order
- Complete order
- Cancel order
- Pagination
- Sorting
- Filtering by user
- Filtering by status
- Filtering by creation date range

### Order Items

Each order item stores a product snapshot:

- `productId`
- `productName`
- `quantity`
- `unitPrice`
- `subtotal`

`productName` and `unitPrice` are stored with the order so later Product Service changes do not modify historical orders.

```

## Project Structure

```text
order-service/
│
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/order_service/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   ├── specification/
│   │   │   └── OrderServiceApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/
│   │           ├── V1__create_orders_table.sql
│   │           └── V2__create_order_items_table.sql
│   └── test/
│       └── java/com/ecommerce/order_service/
│           ├── controller/
│           ├── service/
│           └── integration/
│
├── .env.example
├── pom.xml
└── README.md
```


## Port

```text
8084
```


## OpenAPI

```text
GET http://localhost:8084/v3/api-docs
GET http://localhost:8084/v3/api-docs.yaml
```

Swagger UI is intentionally not included.

## Health

```text
GET http://localhost:8084/actuator/health
```

## Tests

Included:

- Service unit tests
- Controller MockMvc tests
- PostgreSQL Testcontainers integration test
- Full order lifecycle integration test
- Filtering integration test
- Flyway validation through Testcontainers

Docker must be running for integration tests.
