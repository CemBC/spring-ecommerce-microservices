package com.ecommerce.api_gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.ServerSocket;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayUnavailableServiceIntegrationTest {

    private static final int UNUSED_PORT = findUnusedPort();

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void properties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "ORDER_SERVICE_URL",
                () -> "http://localhost:" + UNUSED_PORT
        );
    }

    @Test
    void shouldReturn503WhenDownstreamServiceIsUnavailable() {
        webTestClient
                .get()
                .uri("/api/orders/1")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentTypeCompatibleWith("application/json")
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.message")
                .isEqualTo("Downstream service is unavailable");
    }

    private static int findUnusedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Could not allocate an unused port",
                    ex
            );
        }
    }
}
