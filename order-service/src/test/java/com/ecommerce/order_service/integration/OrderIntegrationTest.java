package com.ecommerce.order_service.integration;

import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderItem;
import com.ecommerce.order_service.entity.OrderStatus;
import com.ecommerce.order_service.repository.OrderRepository;
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
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("order_test_db")
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
    private OrderRepository orderRepository;

    @Test
    void shouldPersistOrderWithItemsAfterFlywayMigrations() {
        Order order = Order.builder()
                .userId(25L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        order.addItem(
                OrderItem.builder()
                        .productId(9L)
                        .productName("Test Product")
                        .quantity(2)
                        .unitPrice(new BigDecimal("50.00"))
                        .subtotal(new BigDecimal("100.00"))
                        .build()
        );

        Order saved =
                orderRepository.saveAndFlush(order);

        assertNotNull(saved.getId());
        assertEquals(1, saved.getItems().size());
        assertNotNull(saved.getItems().get(0).getId());
        assertEquals(OrderStatus.PENDING, saved.getStatus());
    }
}
