package com.ecommerce.order_service.client;

import com.ecommerce.order_service.client.dto.ProductSnapshotResponse;
import com.ecommerce.order_service.exception.DownstreamServiceUnavailableException;
import com.ecommerce.order_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProductClientTest {

    private MockRestServiceServer server;
    private ProductClient productClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(
                                "http://product-service"
                        );

        server =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        productClient =
                new ProductClient(
                        builder.build()
                );
    }

    @Test
    void shouldReturnProduct() {
        server.expect(
                        once(),
                        requestTo(
                                "http://product-service/api/products/2"
                        )
                )
                .andExpect(
                        method(HttpMethod.GET)
                )
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "id": 2,
                                  "name": "MacBook Air M3",
                                  "description": "13-inch laptop",
                                  "price": 45000.00,
                                  "sku": "MBA-M3-001",
                                  "active": true,
                                  "categoryId": 3,
                                  "categoryName": "Electronics",
                                  "createdAt": "2026-09-18T14:49:40",
                                  "updatedAt": "2026-09-18T14:49:40"
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        ProductSnapshotResponse product =
                productClient.getProduct(2L);

        assertEquals(
                2L,
                product.id()
        );

        assertEquals(
                "MacBook Air M3",
                product.name()
        );

        assertEquals(
                new BigDecimal("45000.00"),
                product.price()
        );

        assertTrue(
                product.active()
        );

        server.verify();
    }

    @Test
    void shouldRejectMissingProduct() {
        server.expect(
                        once(),
                        requestTo(
                                "http://product-service/api/products/999"
                        )
                )
                .andExpect(
                        method(HttpMethod.GET)
                )
                .andRespond(
                        withStatus(
                                HttpStatus.NOT_FOUND
                        )
                );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        productClient
                                .getProduct(999L)
        );

        server.verify();
    }

    @Test
    void shouldReturnUnavailableWhenProductServiceFails() {
        server.expect(
                        once(),
                        requestTo(
                                "http://product-service/api/products/2"
                        )
                )
                .andExpect(
                        method(HttpMethod.GET)
                )
                .andRespond(
                        withStatus(
                                HttpStatus.INTERNAL_SERVER_ERROR
                        )
                );

        assertThrows(
                DownstreamServiceUnavailableException.class,
                () ->
                        productClient
                                .getProduct(2L)
        );

        server.verify();
    }
}
