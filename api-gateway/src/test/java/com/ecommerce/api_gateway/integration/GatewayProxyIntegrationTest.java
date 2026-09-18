package com.ecommerce.api_gateway.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayProxyIntegrationTest {

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

        registry.add(
                "PRODUCT_SERVICE_URL",
                () -> "http://localhost:" + backend.port()
        );
    }

    @Test
    void shouldProxyProductRequestAndForwardRequestId() {
        webTestClient
                .get()
                .uri("/api/products/123")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Request-Id")
                .expectBody(String.class)
                .isEqualTo("product-service-ok");
    }

    @Test
    void shouldPreserveDownstream409Status() {
        webTestClient
                .get()
                .uri("/api/products/conflict")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(String.class)
                .isEqualTo("product-conflict");
    }

    @Test
    void shouldPreserveDownstream404Status() {
        webTestClient
                .get()
                .uri("/api/products/not-found")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .isEqualTo("product-not-found");
    }

    @Test
    void shouldForwardAuthorizationHeader() {
        webTestClient
                .get()
                .uri("/api/products/secure")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer integration-token"
                )
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("authorization-forwarded");
    }

    private static synchronized void ensureBackend() {
        if (backend != null) {
            return;
        }

        backend = HttpServer.create()
                .port(0)
                .route(routes -> routes
                        .get(
                                "/api/products/123",
                                (request, response) -> {
                                    String requestId =
                                            request.requestHeaders()
                                                    .get("X-Request-Id");

                                    if (requestId == null
                                            || requestId.isBlank()) {
                                        return response
                                                .status(500)
                                                .sendString(
                                                        Mono.just(
                                                                "missing-request-id"
                                                        )
                                                );
                                    }

                                    return response
                                            .status(200)
                                            .sendString(
                                                    Mono.just(
                                                            "product-service-ok"
                                                    )
                                            );
                                }
                        )
                        .get(
                                "/api/products/conflict",
                                (request, response) ->
                                        response
                                                .status(409)
                                                .sendString(
                                                        Mono.just(
                                                                "product-conflict"
                                                        )
                                                )
                        )
                        .get(
                                "/api/products/not-found",
                                (request, response) ->
                                        response
                                                .status(404)
                                                .sendString(
                                                        Mono.just(
                                                                "product-not-found"
                                                        )
                                                )
                        )
                        .get(
                                "/api/products/secure",
                                (request, response) -> {
                                    String authorization =
                                            request.requestHeaders()
                                                    .get(
                                                            HttpHeaders.AUTHORIZATION
                                                    );

                                    if (!"Bearer integration-token"
                                            .equals(authorization)) {
                                        return response
                                                .status(401)
                                                .sendString(
                                                        Mono.just(
                                                                "missing-authorization"
                                                        )
                                                );
                                    }

                                    return response
                                            .status(200)
                                            .sendString(
                                                    Mono.just(
                                                            "authorization-forwarded"
                                                    )
                                            );
                                }
                        )
                )
                .bindNow();
    }
}
