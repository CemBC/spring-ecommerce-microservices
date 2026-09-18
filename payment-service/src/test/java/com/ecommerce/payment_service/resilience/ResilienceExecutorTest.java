package com.ecommerce.payment_service.resilience;

import com.ecommerce.payment_service.exception.DownstreamServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResilienceExecutorTest {

    private ResilienceExecutor executor;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig circuitBreakerConfig =
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .slidingWindowSize(2)
                        .minimumNumberOfCalls(2)
                        .waitDurationInOpenState(
                                Duration.ofMinutes(1)
                        )
                        .recordException(throwable ->
                                throwable instanceof DownstreamServiceUnavailableException
                        )
                        .build();

        RetryConfig retryConfig =
                RetryConfig.custom()
                        .maxAttempts(2)
                        .waitDuration(Duration.ZERO)
                        .retryOnException(throwable ->
                                throwable instanceof DownstreamServiceUnavailableException
                        )
                        .build();

        executor = new ResilienceExecutor(
                CircuitBreakerRegistry.of(circuitBreakerConfig),
                RetryRegistry.of(retryConfig)
        );
    }

    @Test
    void shouldRetryReadOnceThenSucceed() {
        AtomicInteger calls = new AtomicInteger();

        String result = executor.executeRead(
                "read-service",
                "Downstream unavailable",
                () -> {
                    if (calls.incrementAndGet() == 1) {
                        throw new DownstreamServiceUnavailableException(
                                "temporary failure"
                        );
                    }
                    return "ok";
                }
        );

        assertEquals("ok", result);
        assertEquals(2, calls.get());
    }

    @Test
    void shouldNotRetryWriteCalls() {
        AtomicInteger calls = new AtomicInteger();

        assertThrows(
                DownstreamServiceUnavailableException.class,
                () -> executor.executeWriteVoid(
                        "write-service",
                        "Downstream unavailable",
                        () -> {
                            calls.incrementAndGet();
                            throw new DownstreamServiceUnavailableException(
                                    "failure"
                            );
                        }
                )
        );

        assertEquals(1, calls.get());
    }

    @Test
    void shouldFailFastAfterCircuitOpens() {
        AtomicInteger calls = new AtomicInteger();

        for (int i = 0; i < 2; i++) {
            assertThrows(
                    DownstreamServiceUnavailableException.class,
                    () -> executor.executeWriteVoid(
                            "open-service",
                            "Downstream unavailable",
                            () -> {
                                calls.incrementAndGet();
                                throw new DownstreamServiceUnavailableException(
                                        "failure"
                                );
                            }
                    )
            );
        }

        DownstreamServiceUnavailableException exception =
                assertThrows(
                        DownstreamServiceUnavailableException.class,
                        () -> executor.executeWriteVoid(
                                "open-service",
                                "Downstream unavailable",
                                calls::incrementAndGet
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("circuit breaker open")
        );
        assertEquals(2, calls.get());
    }
}
