# Payment Service

Payment Service is a Spring Boot microservice responsible for managing the payment lifecycle in the e-commerce system.

This version is standalone-complete. It uses a mock payment provider so the payment domain, lifecycle, validation, persistence, filtering, concurrency protection, and automated tests can be completed before real provider integration.

## Lifecycle

```text
PENDING → PROCESSING → SUCCEEDED → REFUNDED
                  ↘ FAILED

PENDING → CANCELLED
```

Rules:

- Only PENDING can be processed.
- Only PROCESSING can succeed or fail.
- Only PENDING can be cancelled.
- Only SUCCEEDED can be refunded.
- Failed/cancelled payments may be retried with a new Payment record.
- PENDING, PROCESSING, SUCCEEDED and REFUNDED block another payment for the same order.

## Model

```text
Payment
-------------------------
id
orderId
userId
amount
currency
status
provider
transactionReference
failureReason
version
createdAt
updatedAt
```

`orderId` and `userId` are external references during standalone development.

## Port

```text
8085
```

## Concurrency

`Payment` uses JPA `@Version`.

Concurrent stale updates are rejected instead of silently overwriting another lifecycle transition.

~~## OpenAPI

```text
GET http://localhost:8085/v3/api-docs
GET http://localhost:8085/v3/api-docs.yaml
```

## Health

```text
GET http://localhost:8085/actuator/health
```