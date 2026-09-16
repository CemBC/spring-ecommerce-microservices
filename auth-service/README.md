# Auth Service

Auth Service is a Spring Boot microservice responsible for user registration, authentication, JWT access tokens, refresh-token sessions, password security, and role-based authorization in the e-commerce system.

## Features

### Authentication

- Register user
- Login with email/password
- BCrypt password hashing
- JWT access token
- Opaque refresh tokens
- Refresh-token rotation
- Refresh-token hashing before database persistence
- Single-session logout
- Logout all sessions
- Change password
- Current authenticated user endpoint
- Disabled-account protection

### Authorization

Roles:

- `USER`
- `ADMIN`

Admin operations:

- List users with pagination/sorting
- Get user by ID
- Enable/disable user
- Change user role

### Security Behavior

- Access token lifetime: 15 minutes by default
- Refresh token lifetime: 7 days by default
- Refresh tokens are random opaque secrets
- Only SHA-256 hashes of refresh tokens are stored in PostgreSQL
- Refresh tokens are rotated after use
- Used/revoked refresh tokens cannot be refreshed again
- Password changes revoke every refresh token for the user
- Account disable revokes every refresh token
- Role changes revoke every refresh token
- Expired refresh-token rows are cleaned automatically
- API security errors return JSON 401/403 responses
- Stateless Spring Security configuration

## Project Structure

```text
auth-service-complete/
│
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/auth_service/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   ├── service/
│   │   │   └── AuthServiceApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/
│   │           ├── V1__create_users_table.sql
│   │           └── V2__create_refresh_tokens_table.sql
│   └── test/
│       └── java/com/ecommerce/auth_service/
│           ├── integration/
│           └── service/
├── .env.example
├── .gitattributes
├── .gitignore
├── pom.xml
└── README.md
```

## OpenAPI

```text
GET http://localhost:8083/v3/api-docs
GET http://localhost:8083/v3/api-docs.yaml
```

## Health

```text
GET http://localhost:8083/actuator/health
```

## Tests

Included:

- Auth service unit tests
- Refresh-token unit tests
- PostgreSQL Testcontainers integration test
- Flyway migration validation through the integration test

Docker must be running for Testcontainers integration tests.
