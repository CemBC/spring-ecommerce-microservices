package com.ecommerce.payment_service.integration;

import com.ecommerce.payment_service.entity.Payment;
import com.ecommerce.payment_service.entity.PaymentStatus;
import com.ecommerce.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class PaymentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("payment_test_db")
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
    private PaymentRepository paymentRepository;

    @Test
    void shouldPersistPaymentAfterFlywayMigrations() {
        Payment payment = Payment.builder()
                .orderId(1L)
                .userId(2L)
                .amount(new BigDecimal("250.00"))
                .currency("TRY")
                .status(PaymentStatus.PENDING)
                .provider("MOCK")
                .build();

        Payment saved =
                paymentRepository.saveAndFlush(payment);

        assertNotNull(saved.getId());
        assertEquals(PaymentStatus.PENDING, saved.getStatus());
        assertEquals(0L, saved.getVersion());
    }
}
