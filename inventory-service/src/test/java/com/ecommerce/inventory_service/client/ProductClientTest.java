package com.ecommerce.inventory_service.client;

import com.ecommerce.inventory_service.exception.DownstreamServiceUnavailableException;
import com.ecommerce.inventory_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProductClientTest {

    private MockRestServiceServer server;
    private ProductClient productClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl("http://product-service");

        server = MockRestServiceServer
                .bindTo(builder)
                .build();

        productClient =
                new ProductClient(builder.build());
    }

    @Test
    void shouldAcceptExistingProduct() {
        server.expect(
                        once(),
                        requestTo(
                                "http://product-service/api/products/1"
                        )
                )
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        assertDoesNotThrow(
                () -> productClient.requireProductExists(1L)
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
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.NOT_FOUND)
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> productClient.requireProductExists(999L)
        );

        server.verify();
    }

    @Test
    void shouldReturnUnavailableWhenProductServiceFails() {
        server.expect(
                        once(),
                        requestTo(
                                "http://product-service/api/products/1"
                        )
                )
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(
                                HttpStatus.INTERNAL_SERVER_ERROR
                        )
                );

        assertThrows(
                DownstreamServiceUnavailableException.class,
                () -> productClient.requireProductExists(1L)
        );

        server.verify();
    }
}
