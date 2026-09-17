package com.ecommerce.payment_service.integration;

import com.ecommerce.payment_service.entity.Payment;
import com.ecommerce.payment_service.entity.PaymentStatus;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class PaymentOptimisticLockIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("payment_lock_test_db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void properties(
            DynamicPropertyRegistry registry
    ) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void shouldRejectStaleConcurrentPaymentUpdate() {
        Long paymentId = createPayment();

        EntityManager first =
                entityManagerFactory.createEntityManager();

        EntityManager second =
                entityManagerFactory.createEntityManager();

        try {
            first.getTransaction().begin();
            second.getTransaction().begin();

            Payment firstCopy =
                    first.find(Payment.class, paymentId);

            Payment secondCopy =
                    second.find(Payment.class, paymentId);

            firstCopy.setStatus(PaymentStatus.PROCESSING);
            first.getTransaction().commit();

            secondCopy.setStatus(PaymentStatus.CANCELLED);

            RollbackException exception =
                    assertThrows(
                            RollbackException.class,
                            () -> second.getTransaction().commit()
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

    private Long createPayment() {
        EntityManager entityManager =
                entityManagerFactory.createEntityManager();

        try {
            entityManager.getTransaction().begin();

            Payment payment = Payment.builder()
                    .orderId(1L)
                    .userId(1L)
                    .amount(new BigDecimal("100.00"))
                    .currency("TRY")
                    .status(PaymentStatus.PENDING)
                    .provider("MOCK")
                    .build();

            entityManager.persist(payment);
            entityManager.getTransaction().commit();

            return payment.getId();

        } finally {
            entityManager.close();
        }
    }
}
