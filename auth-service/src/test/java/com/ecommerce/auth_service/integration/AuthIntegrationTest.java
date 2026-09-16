package com.ecommerce.auth_service.integration;

import com.ecommerce.auth_service.entity.Role;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.repository.UserRepository;
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
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("auth_test_db")
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

        registry.add(
                "security.jwt.secret",
                () -> "VGhpcy1pcy1hLXRlc3Qtc2VjcmV0LWtleS10aGF0LWlzLWxvbmctZW5vdWdoLTEyMzQ1Njc4OTA="
        );

        registry.add(
                "security.jwt.expiration-ms",
                () -> "900000"
        );

        registry.add(
                "security.refresh-token.expiration-ms",
                () -> "604800000"
        );
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistUserAfterFlywayMigration() {
        User user = User.builder()
                .fullName("Integration User")
                .email("integration@example.com")
                .passwordHash("$2a$10$fakehashfortestonly")
                .role(Role.USER)
                .active(true)
                .build();

        User saved =
                userRepository.saveAndFlush(user);

        assertNotNull(saved.getId());
        assertEquals(
                "integration@example.com",
                saved.getEmail()
        );
        assertEquals(Role.USER, saved.getRole());
    }
}
