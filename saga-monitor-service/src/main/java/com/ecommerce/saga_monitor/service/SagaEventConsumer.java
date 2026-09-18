package com.ecommerce.saga_monitor.service;

import com.ecommerce.saga_monitor.entity.SagaState;
import com.ecommerce.saga_monitor.repository.SagaStateRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class SagaEventConsumer {

    private final SagaStateRepository repository;
    private final ObjectMapper objectMapper;

    public SagaEventConsumer(
            SagaStateRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = {
                    "order.events",
                    "inventory.events",
                    "payment.events"
            }
    )
    @Transactional
    @SuppressWarnings("unchecked")
    public void consume(String message) {
        try {
            Map<String, Object> envelope =
                    objectMapper.readValue(
                            message,
                            Map.class
                    );

            String eventType =
                    String.valueOf(
                            envelope.get("eventType")
                    );

            Map<String, Object> payload =
                    (Map<String, Object>)
                            envelope.get("payload");

            Long orderId =
                    asLong(
                            payload.get("orderId")
                    );

            SagaState state =
                    repository.findById(orderId)
                            .orElseGet(() -> {
                                SagaState created =
                                        new SagaState();

                                created.setOrderId(
                                        orderId
                                );
                                created.setTerminalStatus(
                                        "IN_PROGRESS"
                                );

                                return created;
                            });

            apply(
                    state,
                    eventType,
                    payload
            );

            state.setLastEventType(
                    eventType
            );
            state.setLastEventAt(
                    LocalDateTime.now()
            );

            evaluateTerminalStatus(
                    state
            );

            repository.save(state);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Could not process saga event",
                    ex
            );
        }
    }

    private void apply(
            SagaState state,
            String eventType,
            Map<String, Object> payload
    ) {
        switch (eventType) {
            case "ORDER_CREATED" ->
                    state.setOrderCreated(true);

            case "ORDER_CONFIRMED" ->
                    state.setOrderConfirmed(true);

            case "ORDER_CANCELLED" ->
                    state.setTerminalStatus(
                            "CANCELLED"
                    );

            case "INVENTORY_RESERVED" ->
                    state.setInventoryReserved(true);

            case "INVENTORY_RELEASED" -> {
                state.setInventoryReserved(false);

                if (!state.isOrderConfirmed()) {
                    state.setTerminalStatus(
                            "CANCELLED"
                    );
                }
            }

            case "INVENTORY_CONFIRMED" -> {
                state.setInventoryReserved(false);
                state.setInventoryConfirmed(true);
            }

            case "INVENTORY_COMPENSATED" -> {
                state.setInventoryReserved(false);
                state.setInventoryConfirmed(false);
                state.setTerminalStatus(
                        "COMPENSATED"
                );
            }

            case "PAYMENT_CREATED",
                 "PAYMENT_PROCESSING",
                 "PAYMENT_SUCCEEDED",
                 "PAYMENT_FAILED",
                 "PAYMENT_CANCELLED",
                 "PAYMENT_REFUNDED" -> {

                Object paymentId =
                        payload.get("paymentId");

                if (paymentId != null) {
                    state.setPaymentId(
                            asLong(paymentId)
                    );
                }

                Object status =
                        payload.get("status");

                if (status != null) {
                    state.setPaymentStatus(
                            String.valueOf(status)
                    );
                }

                if ("PAYMENT_FAILED".equals(
                        eventType
                )) {
                    state.setTerminalStatus(
                            "FAILED"
                    );
                }

                if ("PAYMENT_CANCELLED".equals(
                        eventType
                )) {
                    state.setTerminalStatus(
                            "CANCELLED"
                    );
                }
            }

            default -> {
                // Forward-compatible: ignore unknown future events.
            }
        }
    }

    private void evaluateTerminalStatus(
            SagaState state
    ) {
        if ("CANCELLED".equals(
                state.getTerminalStatus()
        )
                || "FAILED".equals(
                state.getTerminalStatus()
        )
                || "COMPENSATED".equals(
                state.getTerminalStatus()
        )) {
            return;
        }

        if (state.isInventoryConfirmed()
                && state.isOrderConfirmed()
                && "SUCCEEDED".equals(
                        state.getPaymentStatus()
                )) {
            state.setTerminalStatus(
                    "COMPLETED"
            );
        } else {
            state.setTerminalStatus(
                    "IN_PROGRESS"
            );
        }
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.valueOf(
                String.valueOf(value)
        );
    }
}
