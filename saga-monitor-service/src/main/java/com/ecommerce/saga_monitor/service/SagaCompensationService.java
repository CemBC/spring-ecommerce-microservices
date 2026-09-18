package com.ecommerce.saga_monitor.service;

import com.ecommerce.saga_monitor.entity.SagaState;
import com.ecommerce.saga_monitor.repository.SagaStateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class SagaCompensationService {

    private final SagaStateRepository repository;
    private final RestClient orderClient;
    private final RestClient inventoryClient;
    private final RestClient paymentClient;

    public SagaCompensationService(
            SagaStateRepository repository,
            @Qualifier("sagaOrderRestClient")
            RestClient orderClient,
            @Qualifier("sagaInventoryRestClient")
            RestClient inventoryClient,
            @Qualifier("sagaPaymentRestClient")
            RestClient paymentClient
    ) {
        this.repository = repository;
        this.orderClient = orderClient;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public SagaState compensate(Long orderId) {
        SagaState state =
                repository.findById(orderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Saga not found"
                                )
                        );

        if (!"COMPENSATION_REQUIRED".equals(
                state.getTerminalStatus()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Saga does not require compensation"
            );
        }

        Map<String, Object> order = null;
        boolean orderExists = true;

        try {
            order =
                    orderClient.get()
                            .uri(
                                    "/api/orders/{orderId}",
                                    orderId
                            )
                            .retrieve()
                            .body(Map.class);
        }
        catch (HttpClientErrorException.NotFound ex) {
            orderExists = false;
        }

        if (orderExists
                && (order == null
                || !"PENDING".equals(
                String.valueOf(
                        order.get("status")
                )))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only a PENDING or missing order can be compensated automatically"
            );
        }

        if (state.getPaymentId() != null
                && "PROCESSING".equals(
                state.getPaymentStatus()
        )) {

            paymentClient.patch()
                    .uri(
                            "/api/payments/{paymentId}/fail",
                            state.getPaymentId()
                    )
                    .body(
                            Map.of(
                                    "failureReason",
                                    "Saga compensation"
                            )
                    )
                    .retrieve()
                    .toBodilessEntity();
        }

        inventoryClient.post()
                .uri(
                        "/api/inventory/reservations/{orderId}/compensate",
                        orderId
                )
                .retrieve()
                .toBodilessEntity();

        if (orderExists) {
            orderClient.patch()
                    .uri(
                            "/api/orders/{orderId}/cancel",
                            orderId
                    )
                    .retrieve()
                    .toBodilessEntity();
        }

        state.setTerminalStatus(
                "COMPENSATED"
        );

        return state;
    }
}
