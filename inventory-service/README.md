# Inventory Service

Inventory Service is a Spring Boot microservice responsible for managing product stock and inventory operations in the e-commerce system.

It provides stock management, reservations, validation, filtering, pagination, database migrations, health checks, OpenAPI documentation, and automated tests.

## Tech Stack

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- Lombok
- Jakarta Validation
- Spring Boot Actuator
- OpenAPI
- JUnit 5
- Mockito
- Testcontainers
- Maven

## Features

### Inventory Management

Supports:

- Create inventory for a product
- Get inventory by product ID
- Get all inventory records
- Increase stock
- Decrease stock
- Reserve stock
- Release reserved stock
- Calculate available quantity
- Prevent duplicate inventory records for the same product
- Prevent stock operations when available quantity is insufficient
- Pagination
- Sorting
- Filtering
- Optimistic locking with `@Version`

### Stock Filtering

Supports filtering by:

- Product ID
- Minimum total quantity
- Maximum total quantity
- Minimum available quantity
- Maximum available quantity
- Whether reserved stock exists

Filters can be combined with pagination and sorting.

## Project Structure

```text
inventory-service/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.ecommerce.inventory_service/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── entity/
│   │   │       ├── exception/
│   │   │       ├── mapper/
│   │   │       ├── repository/
│   │   │       ├── service/
│   │   │       ├── specification/
│   │   │       └── InventoryServiceApplication.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/
│   │           └── migration/
│   │               └── V1__create_inventory_table.sql
│   │
│   └── test/
│       └── java/
│           └── com.ecommerce.inventory_service/
│               ├── controller/
│               ├── service/
│               └── integration/
│
├── pom.xml
├── mvnw
└── mvnw.cmd