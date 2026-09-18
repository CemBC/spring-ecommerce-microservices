package com.ecommerce.order_service.client;

import com.ecommerce.order_service.client.dto.ProductSnapshotResponse;
import com.ecommerce.order_service.exception.DownstreamServiceUnavailableException;
import com.ecommerce.order_service.exception.ResourceNotFoundException;
import com.ecommerce.order_service.resilience.ResilienceExecutor;
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

    public ProductSnapshotResponse getProduct(Long productId) {
        return resilienceExecutor.executeRead(
                CIRCUIT,
                "Product Service is unavailable",
                () -> getProductOnce(productId)
        );
    }

    private ProductSnapshotResponse getProductOnce(Long productId) {
        try {
            ProductSnapshotResponse product =
                    restClient.get()
                            .uri(
                                    "/api/products/{productId}",
                                    productId
                            )
                            .retrieve()
                            .body(ProductSnapshotResponse.class);

            if (product == null) {
                throw new DownstreamServiceUnavailableException(
                        "Product Service returned an empty response"
                );
            }

            return product;

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException(
                        "Product not found with id: " + productId
                );
            }

            throw new DownstreamServiceUnavailableException(
                    "Product Service rejected the product request"
            );

        } catch (RestClientException ex) {
            throw new DownstreamServiceUnavailableException(
                    "Product Service is unavailable"
            );
        }
    }
}
