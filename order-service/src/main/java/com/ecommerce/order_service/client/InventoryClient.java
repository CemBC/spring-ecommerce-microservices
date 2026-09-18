package com.ecommerce.order_service.client;

import com.ecommerce.order_service.client.dto.InventoryReservationItemRequest;
import com.ecommerce.order_service.client.dto.InventoryReservationRequest;
import com.ecommerce.order_service.dto.CreateOrderItemRequest;
import com.ecommerce.order_service.exception.ConflictException;
import com.ecommerce.order_service.exception.DownstreamServiceUnavailableException;
import com.ecommerce.order_service.resilience.ResilienceExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class InventoryClient {

    private static final String CIRCUIT = "inventoryService";

    private final RestClient restClient;
    private final ResilienceExecutor resilienceExecutor;

    @Autowired
    public InventoryClient(
            @Qualifier("inventoryRestClient") RestClient restClient,
            ResilienceExecutor resilienceExecutor
    ) {
        this.restClient = restClient;
        this.resilienceExecutor = resilienceExecutor;
    }

    InventoryClient(RestClient restClient) {
        this(
                restClient,
                ResilienceExecutor.noop()
        );
    }

    public void reserveOrder(
            Long orderId,
            List<CreateOrderItemRequest> items
    ) {
        InventoryReservationRequest request =
                new InventoryReservationRequest(
                        orderId,
                        items.stream()
                                .map(item ->
                                        new InventoryReservationItemRequest(
                                                item.productId(),
                                                item.quantity()
                                        )
                                )
                                .toList()
                );

        // POST: protected by circuit breaker, intentionally no automatic retry.
        resilienceExecutor.executeWriteVoid(
                CIRCUIT,
                "Inventory Service is unavailable",
                () -> reserveOnce(request)
        );
    }

    public void releaseOrder(Long orderId) {
        // POST: no retry to avoid duplicate side effects.
        resilienceExecutor.executeWriteVoid(
                CIRCUIT,
                "Inventory Service is unavailable",
                () -> releaseOnce(orderId)
        );
    }

    public void confirmOrder(Long orderId) {
        // POST: no retry to avoid duplicate stock consumption.
        resilienceExecutor.executeWriteVoid(
                CIRCUIT,
                "Inventory Service is unavailable",
                () -> confirmOnce(orderId)
        );
    }

    private void reserveOnce(
            InventoryReservationRequest request
    ) {
        try {
            restClient.post()
                    .uri("/api/inventory/reservations")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ConflictException(
                        "Inventory is not configured for one or more products"
                );
            }

            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                throw new ConflictException(
                        "Inventory reservation failed"
                );
            }

            throw new DownstreamServiceUnavailableException(
                    "Inventory Service rejected the reservation request"
            );

        } catch (RestClientException ex) {
            throw new DownstreamServiceUnavailableException(
                    "Inventory Service is unavailable"
            );
        }
    }

    private void releaseOnce(Long orderId) {
        try {
            restClient.post()
                    .uri(
                            "/api/inventory/reservations/{orderId}/release",
                            orderId
                    )
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                throw new ConflictException(
                        "Inventory reservation release failed"
                );
            }

            throw new DownstreamServiceUnavailableException(
                    "Inventory Service rejected the release request"
            );

        } catch (RestClientException ex) {
            throw new DownstreamServiceUnavailableException(
                    "Inventory Service is unavailable"
            );
        }
    }

    private void confirmOnce(Long orderId) {
        try {
            restClient.post()
                    .uri(
                            "/api/inventory/reservations/{orderId}/confirm",
                            orderId
                    )
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ConflictException(
                        "Inventory reservation was not found for order id: "
                                + orderId
                );
            }

            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                throw new ConflictException(
                        "Inventory reservation confirmation failed"
                );
            }

            throw new DownstreamServiceUnavailableException(
                    "Inventory Service rejected the confirmation request"
            );

        } catch (RestClientException ex) {
            throw new DownstreamServiceUnavailableException(
                    "Inventory Service is unavailable"
            );
        }
    }
}
