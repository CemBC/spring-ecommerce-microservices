package com.ecommerce.payment_service.resilience;

import com.ecommerce.payment_service.exception.DownstreamServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ResilienceExecutor {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final boolean enabled;

    public ResilienceExecutor(
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry
    ) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.enabled = true;
    }

    private ResilienceExecutor() {
        this.circuitBreakerRegistry = null;
        this.retryRegistry = null;
        this.enabled = false;
    }

    /**
     * Used only by isolated client unit tests so existing MockRestServiceServer
     * tests can exercise HTTP/error mapping without the resilience layer.
     */
    public static ResilienceExecutor noop() {
        return new ResilienceExecutor();
    }

    /**
     * Safe read call: retry first, then count the final outcome once in the
     * circuit breaker. This prevents one request with retries from inflating
     * the circuit-breaker failure count.
     */
    public <T> T executeRead(
            String name,
            String unavailableMessage,
            Supplier<T> supplier
    ) {
        if (!enabled) {
            return supplier.get();
        }

        Retry retry = retryRegistry.retry(name);
        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker(name);

        Supplier<T> retried =
                Retry.decorateSupplier(retry, supplier);

        Supplier<T> protectedCall =
                CircuitBreaker.decorateSupplier(
                        circuitBreaker,
                        retried
                );

        try {
            return protectedCall.get();
        } catch (CallNotPermittedException ex) {
            throw new DownstreamServiceUnavailableException(
                    unavailableMessage + " (circuit breaker open)"
            );
        }
    }

    public void executeReadVoid(
            String name,
            String unavailableMessage,
            Runnable runnable
    ) {
        executeRead(
                name,
                unavailableMessage,
                () -> {
                    runnable.run();
                    return null;
                }
        );
    }

    /**
     * Write calls are circuit-breaker protected but intentionally NOT retried.
     * Automatic retry of POST/PATCH operations can duplicate side effects.
     */
    public <T> T executeWrite(
            String name,
            String unavailableMessage,
            Supplier<T> supplier
    ) {
        if (!enabled) {
            return supplier.get();
        }

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker(name);

        Supplier<T> protectedCall =
                CircuitBreaker.decorateSupplier(
                        circuitBreaker,
                        supplier
                );

        try {
            return protectedCall.get();
        } catch (CallNotPermittedException ex) {
            throw new DownstreamServiceUnavailableException(
                    unavailableMessage + " (circuit breaker open)"
            );
        }
    }

    public void executeWriteVoid(
            String name,
            String unavailableMessage,
            Runnable runnable
    ) {
        executeWrite(
                name,
                unavailableMessage,
                () -> {
                    runnable.run();
                    return null;
                }
        );
    }
}
