package com.ecommerce.inventory_service.integration;

import com.ecommerce.inventory_service.entity.Inventory;
import com.ecommerce.inventory_service.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class InventoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("inventory_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private InventoryRepository repository;

    @Test
    void shouldPersistInventory() {
        Inventory inventory = Inventory.builder()
                .productId(99L)
                .quantity(50)
                .reservedQuantity(5)
                .build();

        Inventory saved = repository.saveAndFlush(inventory);

        assertNotNull(saved.getId());
        assertEquals(45, saved.getAvailableQuantity());
        assertEquals(0L, saved.getVersion());
    }
}
