package com.ecommerce.order_service.integration;

import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class OrderOptimisticLockIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("order_lock_test_db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void properties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void shouldRejectStaleConcurrentOrderUpdate() {
        Long orderId = createOrder();

        EntityManager first =
                entityManagerFactory.createEntityManager();

        EntityManager second =
                entityManagerFactory.createEntityManager();

        try {
            first.getTransaction().begin();
            second.getTransaction().begin();

            Order firstCopy =
                    first.find(
                            Order.class,
                            orderId
                    );

            Order secondCopy =
                    second.find(
                            Order.class,
                            orderId
                    );

            firstCopy.setStatus(
                    OrderStatus.CONFIRMED
            );

            first.getTransaction().commit();

            secondCopy.setStatus(
                    OrderStatus.CANCELLED
            );

            RollbackException exception =
                    assertThrows(
                            RollbackException.class,
                            () ->
                                    second
                                            .getTransaction()
                                            .commit()
                    );

            assertInstanceOf(
                    OptimisticLockException.class,
                    exception.getCause()
            );

        } finally {
            if (first.getTransaction().isActive()) {
                first.getTransaction().rollback();
            }

            if (second.getTransaction().isActive()) {
                second.getTransaction().rollback();
            }

            first.close();
            second.close();
        }
    }

    private Long createOrder() {
        EntityManager entityManager =
                entityManagerFactory
                        .createEntityManager();

        try {
            entityManager.getTransaction().begin();

            Order order = Order.builder()
                    .userId(1L)
                    .status(OrderStatus.PENDING)
                    .totalAmount(
                            new BigDecimal("100.00")
                    )
                    .build();

            entityManager.persist(order);

            entityManager
                    .getTransaction()
                    .commit();

            return order.getId();

        } finally {
            entityManager.close();
        }
    }
}
