package com.ecommerce.api_gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter =
            new RequestLoggingFilter();

    @Test
    void shouldGenerateRequestIdWhenMissing() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/products")
                                .build()
                );

        AtomicReference<String> forwardedRequestId =
                new AtomicReference<>();

        GatewayFilterChain chain = currentExchange -> {
            forwardedRequestId.set(
                    currentExchange
                            .getRequest()
                            .getHeaders()
                            .getFirst(
                                    RequestLoggingFilter.REQUEST_ID_HEADER
                            )
            );

            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertNotNull(forwardedRequestId.get());
        assertFalse(forwardedRequestId.get().isBlank());

        assertEquals(
                forwardedRequestId.get(),
                exchange.getResponse()
                        .getHeaders()
                        .getFirst(
                                RequestLoggingFilter.REQUEST_ID_HEADER
                        )
        );
    }

    @Test
    void shouldPreserveExistingRequestId() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/orders")
                                .header(
                                        RequestLoggingFilter.REQUEST_ID_HEADER,
                                        "request-123"
                                )
                                .build()
                );

        AtomicReference<String> forwardedRequestId =
                new AtomicReference<>();

        GatewayFilterChain chain = currentExchange -> {
            forwardedRequestId.set(
                    currentExchange
                            .getRequest()
                            .getHeaders()
                            .getFirst(
                                    RequestLoggingFilter.REQUEST_ID_HEADER
                            )
            );

            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals(
                "request-123",
                forwardedRequestId.get()
        );
    }
}
