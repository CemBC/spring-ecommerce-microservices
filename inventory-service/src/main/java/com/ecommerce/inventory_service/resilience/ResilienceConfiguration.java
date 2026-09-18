package com.ecommerce.inventory_service.resilience;

import com.ecommerce.inventory_service.exception.DownstreamServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfiguration {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(
            @Value("${resilience.circuit-breaker.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${resilience.circuit-breaker.sliding-window-size:10}") int slidingWindowSize,
            @Value("${resilience.circuit-breaker.minimum-number-of-calls:5}") int minimumNumberOfCalls,
            @Value("${resilience.circuit-breaker.wait-duration-open-ms:10000}") long waitDurationOpenMs,
            @Value("${resilience.circuit-breaker.half-open-calls:2}") int halfOpenCalls
    ) {
        CircuitBreakerConfig config =
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(failureRateThreshold)
                        .slidingWindowType(
                                CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
                        )
                        .slidingWindowSize(slidingWindowSize)
                        .minimumNumberOfCalls(minimumNumberOfCalls)
                        .waitDurationInOpenState(
                                Duration.ofMillis(waitDurationOpenMs)
                        )
                        .permittedNumberOfCallsInHalfOpenState(halfOpenCalls)
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        .recordException(throwable ->
                                throwable instanceof DownstreamServiceUnavailableException
                        )
                        .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public RetryRegistry retryRegistry(
            @Value("${resilience.retry.max-attempts:2}") int maxAttempts,
            @Value("${resilience.retry.wait-duration-ms:250}") long waitDurationMs
    ) {
        RetryConfig config =
                RetryConfig.custom()
                        .maxAttempts(maxAttempts)
                        .waitDuration(
                                Duration.ofMillis(waitDurationMs)
                        )
                        .retryOnException(throwable ->
                                throwable instanceof DownstreamServiceUnavailableException
                        )
                        .build();

        return RetryRegistry.of(config);
    }
}
