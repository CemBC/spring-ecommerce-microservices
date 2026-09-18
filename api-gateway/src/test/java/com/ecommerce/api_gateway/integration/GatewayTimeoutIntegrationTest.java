package com.ecommerce.api_gateway.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayTimeoutIntegrationTest {

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

        registry.add(
                "spring.cloud.gateway.server.webflux.httpclient.response-timeout",
                () -> "250ms"
        );
    }

    @Test
    void shouldReturn504WhenDownstreamResponseTimesOut() {
        webTestClient
                .get()
                .uri("/api/products/slow")
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectBody()
                .jsonPath("$.status").isEqualTo(504)
                .jsonPath("$.message")
                .isEqualTo(
                        "Downstream service response timed out"
                );
    }

    private static synchronized void ensureBackend() {
        if (backend != null) {
            return;
        }

        backend = HttpServer.create()
                .port(0)
                .route(routes ->
                        routes.get(
                                "/api/products/slow",
                                (request, response) ->
                                        response
                                                .status(200)
                                                .sendString(
                                                        Mono.delay(
                                                                        Duration.ofSeconds(
                                                                                1
                                                                        )
                                                                )
                                                                .thenReturn(
                                                                        "late-response"
                                                                )
                                                )
                        )
                )
                .bindNow();
    }
}
