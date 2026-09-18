package com.ecommerce.inventory_service.client;

import com.ecommerce.inventory_service.exception.DownstreamServiceUnavailableException;
import com.ecommerce.inventory_service.exception.ResourceNotFoundException;
import com.ecommerce.inventory_service.resilience.ResilienceExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ProductClient {

    private static final String CIRCUIT = "productService";

    private final RestClient restClient;
    private final ResilienceExecutor resilienceExecutor;

    @Autowired
    public ProductClient(
            @Qualifier("productRestClient") RestClient restClient,
            ResilienceExecutor resilienceExecutor
    ) {
        this.restClient = restClient;
        this.resilienceExecutor = resilienceExecutor;
    }

    ProductClient(RestClient restClient) {
        this(
                restClient,
                ResilienceExecutor.noop()
        );
    }

    public void requireProductExists(Long productId) {
        resilienceExecutor.executeReadVoid(
                CIRCUIT,
                "Product Service is unavailable",
                () -> requireProductExistsOnce(productId)
        );
    }

    private void requireProductExistsOnce(Long productId) {
        try {
            restClient.get()
                    .uri(
                            "/api/products/{productId}",
                            productId
                    )
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
