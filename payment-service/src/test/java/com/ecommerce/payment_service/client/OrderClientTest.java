package com.ecommerce.payment_service.client;

import com.ecommerce.payment_service.client.dto.OrderSnapshotResponse;
import com.ecommerce.payment_service.exception.ConflictException;
import com.ecommerce.payment_service.exception.DownstreamServiceUnavailableException;
import com.ecommerce.payment_service.exception.ResourceNotFoundException;
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

class OrderClientTest {

    private MockRestServiceServer server;
    private OrderClient orderClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(
                                "http://order-service"
                        );

        server =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        orderClient =
                new OrderClient(
                        builder.build()
                );
    }

    @Test
    void shouldReturnOrder() {
        server.expect(
                        once(),
                        requestTo(
                                "http://order-service/api/orders/42"
                        )
                )
                .andExpect(
                        method(HttpMethod.GET)
                )
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "id": 42,
                                  "userId": 5,
                                  "status": "PENDING",
                                  "totalAmount": 93500.00,
                                  "items": [],
                                  "createdAt": "2026-09-18T15:00:00",
                                  "updatedAt": "2026-09-18T15:00:00"
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        OrderSnapshotResponse order =
                orderClient.getOrder(42L);

        assertEquals(
                42L,
                order.id()
        );

        assertEquals(
                5L,
                order.userId()
        );

        assertEquals(
                new BigDecimal("93500.00"),
                order.totalAmount()
        );

        assertEquals(
                "PENDING",
                order.status()
        );

        server.verify();
    }

    @Test
    void shouldRejectMissingOrder() {
        server.expect(
                        once(),
                        requestTo(
                                "http://order-service/api/orders/999"
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
                () -> orderClient.getOrder(999L)
        );

        server.verify();
    }

    @Test
    void shouldConfirmOrder() {
        server.expect(
                        once(),
                        requestTo(
                                "http://order-service/api/orders/42/confirm"
                        )
                )
                .andExpect(
                        method(HttpMethod.PATCH)
                )
                .andRespond(
                        withSuccess()
                );

        assertDoesNotThrow(
                () -> orderClient.confirmOrder(42L)
        );

        server.verify();
    }

    @Test
    void shouldMapOrderConfirmationConflict() {
        server.expect(
                        once(),
                        requestTo(
                                "http://order-service/api/orders/42/confirm"
                        )
                )
                .andExpect(
                        method(HttpMethod.PATCH)
                )
                .andRespond(
                        withStatus(
                                HttpStatus.CONFLICT
                        )
                );

        assertThrows(
                ConflictException.class,
                () -> orderClient.confirmOrder(42L)
        );

        server.verify();
    }

    @Test
    void shouldReturnUnavailableWhenOrderServiceFails() {
        server.expect(
                        once(),
                        requestTo(
                                "http://order-service/api/orders/42"
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
                () -> orderClient.getOrder(42L)
        );

        server.verify();
    }
}
