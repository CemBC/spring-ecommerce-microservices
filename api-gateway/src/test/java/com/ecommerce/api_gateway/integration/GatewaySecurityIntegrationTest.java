package com.ecommerce.api_gateway.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewaySecurityIntegrationTest {

    private static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private static DisposableServer backend;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void startBackend() {
        ensureBackend();
    }

    @AfterAll
    static void stopBackend() {
        if (backend != null) {
            backend.disposeNow();
        }
    }

    @DynamicPropertySource
    static void properties(
            DynamicPropertyRegistry registry
    ) {
        ensureBackend();

        String url = "http://localhost:" + backend.port();

        registry.add("PRODUCT_SERVICE_URL", () -> url);
        registry.add("AUTH_SERVICE_URL", () -> url);
        registry.add("ORDER_SERVICE_URL", () -> url);
        registry.add("JWT_SECRET", () -> TEST_SECRET);
    }

    @Test
    void shouldAllowPublicLoginWithoutToken() {
        webTestClient
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("login-public");
    }

    @Test
    void shouldAllowPublicProductGetWithoutToken() {
        webTestClient
                .get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("products-public");
    }

    @Test
    void shouldRejectProtectedOrderWithoutToken() {
        webTestClient
                .get()
                .uri("/api/orders/1")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void shouldRejectInvalidToken() {
        webTestClient
                .get()
                .uri("/api/orders/1")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer not-a-valid-jwt"
                )
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldAllowUserTokenOnAuthenticatedRoute() {
        webTestClient
                .get()
                .uri("/api/orders/1")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(token("USER", false))
                )
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("order-protected");
    }

    @Test
    void shouldRejectExpiredToken() {
        webTestClient
                .get()
                .uri("/api/orders/1")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(token("USER", true))
                )
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRejectUserFromAdminRoute() {
        webTestClient
                .get()
                .uri("/api/admin/users")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(token("USER", false))
                )
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403);
    }

    @Test
    void shouldAllowAdminOnAdminRoute() {
        webTestClient
                .get()
                .uri("/api/admin/users")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(token("ADMIN", false))
                )
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("admin-protected");
    }

    @Test
    void shouldRequireAdminForProductWrite() {
        webTestClient
                .post()
                .uri("/api/products")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(token("USER", false))
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldAllowAdminProductWrite() {
        webTestClient
                .post()
                .uri("/api/products")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(token("ADMIN", false))
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .isEqualTo("product-created");
    }

    private String token(
            String role,
            boolean expired
    ) {
        byte[] keyBytes =
                Base64.getDecoder().decode(TEST_SECRET);

        SecretKey secretKey =
                new SecretKeySpec(keyBytes, "HmacSHA256");

        JwtEncoder encoder = NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();

        Instant now = Instant.now();
        Instant issuedAt = expired
                ? now.minusSeconds(1200)
                : now;
        Instant expiresAt = expired
                ? now.minusSeconds(300)
                : now.plusSeconds(900);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("user@example.com")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("userId", 1L)
                .claim("role", role)
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return encoder.encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private static synchronized void ensureBackend() {
        if (backend != null) {
            return;
        }

        backend = HttpServer.create()
                .port(0)
                .route(routes -> routes
                        .post(
                                "/api/auth/login",
                                (request, response) -> response
                                        .status(200)
                                        .sendString(
                                                Mono.just("login-public")
                                        )
                        )
                        .get(
                                "/api/products",
                                (request, response) -> response
                                        .status(200)
                                        .sendString(
                                                Mono.just("products-public")
                                        )
                        )
                        .post(
                                "/api/products",
                                (request, response) -> response
                                        .status(201)
                                        .sendString(
                                                Mono.just("product-created")
                                        )
                        )
                        .get(
                                "/api/orders/1",
                                (request, response) -> response
                                        .status(200)
                                        .sendString(
                                                Mono.just("order-protected")
                                        )
                        )
                        .get(
                                "/api/admin/users",
                                (request, response) -> response
                                        .status(200)
                                        .sendString(
                                                Mono.just("admin-protected")
                                        )
                        )
                )
                .bindNow();
    }
}
