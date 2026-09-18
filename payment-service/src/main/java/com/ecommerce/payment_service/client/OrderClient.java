package com.ecommerce.payment_service.client;

import com.ecommerce.payment_service.client.dto.OrderSnapshotResponse;
import com.ecommerce.payment_service.exception.ConflictException;
import com.ecommerce.payment_service.exception.DownstreamServiceUnavailableException;
import com.ecommerce.payment_service.exception.ResourceNotFoundException;
import com.ecommerce.payment_service.resilience.ResilienceExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OrderClient {

    private static final String CIRCUIT = "orderService";

    private final RestClient restClient;
    private final ResilienceExecutor resilienceExecutor;

    @Autowired
    public OrderClient(
            @Qualifier("orderRestClient") RestClient restClient,
            ResilienceExecutor resilienceExecutor
    ) {
        this.restClient = restClient;
        this.resilienceExecutor = resilienceExecutor;
    }

    OrderClient(RestClient restClient) {
        this(
                restClient,
                ResilienceExecutor.noop()
        );
    }

    public OrderSnapshotResponse getOrder(Long orderId) {
        // GET: transient downstream failures may be retried once.
        return resilienceExecutor.executeRead(
                CIRCUIT,
                "Order Service is unavailable",
                () -> getOrderOnce(orderId)
        );
    }

    public void confirmOrder(Long orderId) {
        // PATCH: circuit breaker only, no automatic retry.
        resilienceExecutor.executeWriteVoid(
                CIRCUIT,
                "Order Service is unavailable",
                () -> confirmOrderOnce(orderId)
        );
    }

    private OrderSnapshotResponse getOrderOnce(Long orderId) {
        try {
            OrderSnapshotResponse order =
                    restClient.get()
                            .uri(
                                    "/api/orders/{orderId}",
                                    orderId
                            )
                            .retrieve()
                            .body(OrderSnapshotResponse.class);

            if (order == null) {
                throw new DownstreamServiceUnavailableException(
                        "Order Service returned an empty response"
                );
            }

            return order;

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException(
                        "Order not found with id: " + orderId
                );
            }

            throw new DownstreamServiceUnavailableException(
                    "Order Service rejected the order request"
            );

        } catch (RestClientException ex) {
            throw new DownstreamServiceUnavailableException(
                    "Order Service is unavailable"
            );
        }
    }

    private void confirmOrderOnce(Long orderId) {
        try {
            restClient.patch()
                    .uri(
                            "/api/orders/{orderId}/confirm",
                            orderId
                    )
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException(
                        "Order not found with id: " + orderId
                );
            }

            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                throw new ConflictException(
                        "Order cannot be confirmed"
                );
            }

            throw new DownstreamServiceUnavailableException(
                    "Order Service rejected the confirmation request"
            );

        } catch (RestClientException ex) {
            throw new DownstreamServiceUnavailableException(
                    "Order Service is unavailable"
            );
        }
    }
}
