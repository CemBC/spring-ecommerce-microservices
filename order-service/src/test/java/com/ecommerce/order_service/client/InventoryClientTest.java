package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.CreateOrderItemRequest;
import com.ecommerce.order_service.exception.ConflictException;
import com.ecommerce.order_service.exception.DownstreamServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class InventoryClientTest {

    private MockRestServiceServer server;
    private InventoryClient inventoryClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(
                                "http://inventory-service"
                        );

        server =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        inventoryClient =
                new InventoryClient(
                        builder.build()
                );
    }

    @Test
    void shouldReserveOrderStock() {
        server.expect(
                        once(),
                        requestTo(
                                "http://inventory-service/api/inventory/reservations"
                        )
                )
                .andExpect(
                        method(HttpMethod.POST)
                )
                .andRespond(
                        withStatus(HttpStatus.CREATED)
                );

        assertDoesNotThrow(
                () ->
                        inventoryClient.reserveOrder(
                                1L,
                                List.of(
                                        new CreateOrderItemRequest(
                                                2L,
                                                3
                                        )
                                )
                        )
        );

        server.verify();
    }

    @Test
    void shouldReleaseOrderStock() {
        server.expect(
                        once(),
                        requestTo(
                                "http://inventory-service/api/inventory/reservations/1/release"
                        )
                )
                .andExpect(
                        method(HttpMethod.POST)
                )
                .andRespond(
                        withStatus(HttpStatus.OK)
                );

        assertDoesNotThrow(
                () ->
                        inventoryClient.releaseOrder(1L)
        );

        server.verify();
    }

    @Test
    void shouldConfirmOrderStock() {
        server.expect(
                        once(),
                        requestTo(
                                "http://inventory-service/api/inventory/reservations/1/confirm"
                        )
                )
                .andExpect(
                        method(HttpMethod.POST)
                )
                .andRespond(
                        withStatus(HttpStatus.OK)
                );

        assertDoesNotThrow(
                () ->
                        inventoryClient.confirmOrder(1L)
        );

        server.verify();
    }

    @Test
    void shouldMapInventoryReservationConflict() {
        server.expect(
                        once(),
                        requestTo(
                                "http://inventory-service/api/inventory/reservations"
                        )
                )
                .andExpect(
                        method(HttpMethod.POST)
                )
                .andRespond(
                        withStatus(HttpStatus.CONFLICT)
                );

        assertThrows(
                ConflictException.class,
                () ->
                        inventoryClient.reserveOrder(
                                1L,
                                List.of(
                                        new CreateOrderItemRequest(
                                                2L,
                                                3
                                        )
                                )
                        )
        );

        server.verify();
    }

    @Test
    void shouldMapInventoryConfirmationConflict() {
        server.expect(
                        once(),
                        requestTo(
                                "http://inventory-service/api/inventory/reservations/1/confirm"
                        )
                )
                .andExpect(
                        method(HttpMethod.POST)
                )
                .andRespond(
                        withStatus(HttpStatus.CONFLICT)
                );

        assertThrows(
                ConflictException.class,
                () ->
                        inventoryClient.confirmOrder(1L)
        );

        server.verify();
    }

    @Test
    void shouldReturnUnavailableWhenInventoryServiceFails() {
        server.expect(
                        once(),
                        requestTo(
                                "http://inventory-service/api/inventory/reservations/1/confirm"
                        )
                )
                .andExpect(
                        method(HttpMethod.POST)
                )
                .andRespond(
                        withStatus(
                                HttpStatus.INTERNAL_SERVER_ERROR
                        )
                );

        assertThrows(
                DownstreamServiceUnavailableException.class,
                () ->
                        inventoryClient.confirmOrder(1L)
        );

        server.verify();
    }
}
