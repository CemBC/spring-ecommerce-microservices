package com.ecommerce.inventory_service.client;

import com.ecommerce.inventory_service.exception.DownstreamServiceUnavailableException;
import com.ecommerce.inventory_service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(
            @Qualifier("productRestClient")
            RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public void requireProductExists(Long productId) {
        try {
            restClient.get()
                    .uri("/api/products/{productId}", productId)
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException(
                        "Product not found with id: " + productId
                );
            }

            throw new DownstreamServiceUnavailableException(
                    "Product Service rejected the validation request"
            );

        } catch (RestClientException ex) {
            throw new DownstreamServiceUnavailableException(
                    "Product Service is unavailable"
            );
        }
    }
}
