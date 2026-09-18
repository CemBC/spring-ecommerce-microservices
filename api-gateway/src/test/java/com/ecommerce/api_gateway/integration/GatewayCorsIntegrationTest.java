package com.ecommerce.api_gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayCorsIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldHandleCorsPreflightForAllowedOrigin() {
        webTestClient
                .options()
                .uri("/api/products")
                .header(
                        HttpHeaders.ORIGIN,
                        "http://localhost:5173"
                )
                .header(
                        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                        "GET"
                )
                .exchange()
                .expectStatus().isOk()
                .expectHeader()
                .valueEquals(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173"
                );
    }
}
