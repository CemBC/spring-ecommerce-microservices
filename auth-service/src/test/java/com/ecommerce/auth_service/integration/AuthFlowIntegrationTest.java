package com.ecommerce.auth_service.integration;

import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.entity.Role;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.repository.RefreshTokenRepository;
import com.ecommerce.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthFlowIntegrationTest {

    private static final String PASSWORD =
            "StrongPassword123!";

    private static final String NEW_PASSWORD =
            "NewStrongPassword123!";

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("auth_flow_test_db")
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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRotateRefreshTokenRejectOldTokenAndLogoutNewToken()
            throws Exception {

        register(
                "rotation@example.com",
                PASSWORD
        );

        AuthResponse first =
                login(
                        "rotation@example.com",
                        PASSWORD
                );

        AuthResponse second =
                refresh(first.refreshToken());

        assertNotEquals(
                first.refreshToken(),
                second.refreshToken()
        );

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "refreshToken": "%s"
                                        }
                                        """.formatted(
                                        first.refreshToken()
                                ))
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "refreshToken": "%s"
                                        }
                                        """.formatted(
                                        second.refreshToken()
                                ))
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "refreshToken": "%s"
                                        }
                                        """.formatted(
                                        second.refreshToken()
                                ))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLogoutAllSessions()
            throws Exception {

        register(
                "logoutall@example.com",
                PASSWORD
        );

        AuthResponse first =
                login(
                        "logoutall@example.com",
                        PASSWORD
                );

        AuthResponse second =
                login(
                        "logoutall@example.com",
                        PASSWORD
                );

        mockMvc.perform(
                        post("/api/auth/logout-all")
                                .header(
                                        "Authorization",
                                        "Bearer " + first.accessToken()
                                )
                )
                .andExpect(status().isNoContent());

        assertRefreshIsUnauthorized(
                first.refreshToken()
        );

        assertRefreshIsUnauthorized(
                second.refreshToken()
        );
    }

    @Test
    void shouldChangePasswordRevokeRefreshTokensAndRejectOldPassword()
            throws Exception {

        register(
                "password@example.com",
                PASSWORD
        );

        AuthResponse login =
                login(
                        "password@example.com",
                        PASSWORD
                );

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .header(
                                        "Authorization",
                                        "Bearer " + login.accessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "currentPassword": "%s",
                                          "newPassword": "%s"
                                        }
                                        """.formatted(
                                        PASSWORD,
                                        NEW_PASSWORD
                                ))
                )
                .andExpect(status().isNoContent());

        assertRefreshIsUnauthorized(
                login.refreshToken()
        );

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "password@example.com",
                                          "password": "%s"
                                        }
                                        """.formatted(PASSWORD))
                )
                .andExpect(status().isUnauthorized());

        login(
                "password@example.com",
                NEW_PASSWORD
        );
    }

    @Test
    void userShouldReceive403AdminShouldReceive200AndDisabledUserCannotLogin()
            throws Exception {

        register(
                "user@example.com",
                PASSWORD
        );

        register(
                "admin@example.com",
                PASSWORD
        );

        register(
                "target@example.com",
                PASSWORD
        );

        User admin = userRepository
                .findByEmailIgnoreCase(
                        "admin@example.com"
                )
                .orElseThrow();

        admin.setRole(Role.ADMIN);
        userRepository.saveAndFlush(admin);

        User target = userRepository
                .findByEmailIgnoreCase(
                        "target@example.com"
                )
                .orElseThrow();

        AuthResponse normalUser =
                login(
                        "user@example.com",
                        PASSWORD
                );

        AuthResponse adminLogin =
                login(
                        "admin@example.com",
                        PASSWORD
                );

        mockMvc.perform(
                        get("/api/admin/users")
                                .header(
                                        "Authorization",
                                        "Bearer " + normalUser.accessToken()
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/admin/users")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminLogin.accessToken()
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        patch(
                                "/api/admin/users/{id}/active",
                                target.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + adminLogin.accessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "active": false
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.active")
                                .value(false)
                );

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "target@example.com",
                                          "password": "%s"
                                        }
                                        """.formatted(PASSWORD))
                )
                .andExpect(status().isForbidden());
    }

    private void register(
            String email,
            String password
    ) throws Exception {

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fullName": "Test User",
                                          "email": "%s",
                                          "password": "%s"
                                        }
                                        """.formatted(
                                        email,
                                        password
                                ))
                )
                .andExpect(status().isCreated());
    }

    private AuthResponse login(
            String email,
            String password
    ) throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content("""
                                                {
                                                  "email": "%s",
                                                  "password": "%s"
                                                }
                                                """.formatted(
                                                email,
                                                password
                                        ))
                        )
                        .andExpect(status().isOk())
                        .andReturn();

        return objectMapper.readValue(
                result.getResponse()
                        .getContentAsString(),
                AuthResponse.class
        );
    }

    private AuthResponse refresh(
            String refreshToken
    ) throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post("/api/auth/refresh")
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content("""
                                                {
                                                  "refreshToken": "%s"
                                                }
                                                """.formatted(
                                                refreshToken
                                        ))
                        )
                        .andExpect(status().isOk())
                        .andReturn();

        return objectMapper.readValue(
                result.getResponse()
                        .getContentAsString(),
                AuthResponse.class
        );
    }

    private void assertRefreshIsUnauthorized(
            String refreshToken
    ) throws Exception {

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "refreshToken": "%s"
                                        }
                                        """.formatted(
                                        refreshToken
                                ))
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }
}
