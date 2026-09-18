package com.ecommerce.payment_service.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(
        name = "outbox.enabled",
        havingValue = "true"
)
public class OutboxService {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(
            OutboxRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(
            String topic,
            String aggregateType,
            Object aggregateId,
            String eventType,
            Object payload
    ) {
        try {
            OutboxEvent event =
                    new OutboxEvent();

            event.setTopic(topic);
            event.setAggregateType(aggregateType);
            event.setAggregateId(String.valueOf(aggregateId));
            event.setEventType(eventType);
            event.setPayload(
                    objectMapper.writeValueAsString(payload)
            );

            repository.save(event);
        }
        catch (Exception ex) {
            throw new IllegalStateException(
                    "Could not serialize outbox event " + eventType,
                    ex
            );
        }
    }
}
