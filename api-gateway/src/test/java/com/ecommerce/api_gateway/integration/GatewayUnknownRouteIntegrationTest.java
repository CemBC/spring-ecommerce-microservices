package com.ecommerce.api_gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayUnknownRouteIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturn404ForUnknownRoute() {
        webTestClient
                .get()
                .uri("/api/does-not-exist")
                .exchange()
                .expectStatus().isNotFound();
    }
}
