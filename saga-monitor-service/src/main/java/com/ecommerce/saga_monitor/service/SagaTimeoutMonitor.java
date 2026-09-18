package com.ecommerce.saga_monitor.service;

import com.ecommerce.saga_monitor.entity.SagaState;
import com.ecommerce.saga_monitor.repository.SagaStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class SagaTimeoutMonitor {

    private static final Logger log =
            LoggerFactory.getLogger(
                    SagaTimeoutMonitor.class
            );

    private final SagaStateRepository repository;
    private final long timeoutMs;

    public SagaTimeoutMonitor(
            SagaStateRepository repository,
            @Value("${saga.timeout-ms:60000}")
            long timeoutMs
    ) {
        this.repository = repository;
        this.timeoutMs = timeoutMs;
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void markStuckSagas() {
        LocalDateTime cutoff =
                LocalDateTime.now()
                        .minusNanos(
                                timeoutMs * 1_000_000
                        );

        for (SagaState state
                : repository
                .findByTerminalStatusAndUpdatedAtBefore(
                        "IN_PROGRESS",
                        cutoff
                )) {

            state.setTerminalStatus(
                    "COMPENSATION_REQUIRED"
            );

            log.warn(
                    "Saga requires compensation orderId={} lastEvent={}",
                    state.getOrderId(),
                    state.getLastEventType()
            );
        }
    }
}
