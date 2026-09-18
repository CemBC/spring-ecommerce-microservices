package com.ecommerce.payment_service.client;

import com.ecommerce.payment_service.client.dto.OrderSnapshotResponse;
import com.ecommerce.payment_service.exception.ConflictException;
import com.ecommerce.payment_service.exception.DownstreamServiceUnavailableException;
import com.ecommerce.payment_service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OrderClient {

    private final RestClient restClient;

    public OrderClient(
            @Qualifier("orderRestClient")
            RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public OrderSnapshotResponse getOrder(Long orderId) {
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

    public void confirmOrder(Long orderId) {
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
