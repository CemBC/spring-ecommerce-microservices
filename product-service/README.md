# Product Service

Product Service is a Spring Boot microservice responsible for managing products and categories in the e-commerce system.

It provides CRUD operations, validation, filtering, pagination, database migrations, health checks, OpenAPI documentation, and automated tests.

## Features

### Product Management

Supports:

- Create product
- Get product by ID
- Get all products
- Update product
- Delete product
- Search products by name
- Filter by category
- Filter by active status
- Pagination
- Sorting
- Unique SKU validation

### Category Management

Supports:

- Create category
- Get category by ID
- Get all categories
- Update category
- Delete category
- Search categories by name
- Pagination
- Sorting

A category cannot be deleted while products are assigned to it.

## Project Structure

```text
product-service/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.ecommerce.product_service/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── entity/
│   │   │       ├── exception/
│   │   │       ├── mapper/
│   │   │       ├── repository/
│   │   │       ├── service/
│   │   │       ├── specification/
│   │   │       └── ProductServiceApplication.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/
│   │           └── migration/
│   │               ├── V1__create_categories_table.sql
│   │               └── V2__create_products_table.sql
│   │
│   └── test/
│       └── java/
│           └── com.ecommerce.product_service/
│               ├── controller/
│               ├── service/
│               └── integration/
│
├── pom.xml
├── mvnw
└── mvnw.cmd
