package com.ecommerce.api_gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GatewayRoutesIntegrationTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void shouldLoadAllConfiguredRoutes() {
        List<String> routeIds = Flux.from(routeLocator.getRoutes())
                .map(Route::getId)
                .collectList()
                .block();

        assertNotNull(routeIds);
        assertEquals(7, routeIds.size());

        assertTrue(routeIds.contains("product-service-products"));
        assertTrue(routeIds.contains("product-service-categories"));
        assertTrue(routeIds.contains("inventory-service"));
        assertTrue(routeIds.contains("auth-service-auth"));
        assertTrue(routeIds.contains("auth-service-admin"));
        assertTrue(routeIds.contains("order-service"));
        assertTrue(routeIds.contains("payment-service"));
    }
}
