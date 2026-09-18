package com.ecommerce.payment_service.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(
        name = "outbox.enabled",
        havingValue = "true"
)
public class OutboxPublisher {

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String serviceName;
    private final long sendTimeoutMs;

    public OutboxPublisher(
            OutboxRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${spring.application.name}") String serviceName,
            @Value("${outbox.send-timeout-ms:5000}") long sendTimeoutMs
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    @Scheduled(
            fixedDelayString = "${outbox.publish-interval-ms:1000}"
    )
    @Transactional
    public void publishPending() {
        for (OutboxEvent event
                : repository.findTop50ByPublishedFalseOrderByCreatedAtAsc()) {

            try {
                Map<String, Object> envelope =
                        new LinkedHashMap<>();

                envelope.put(
                        "eventId",
                        serviceName + "-" + event.getId()
                );
                envelope.put("source", serviceName);
                envelope.put("eventType", event.getEventType());
                envelope.put("aggregateType", event.getAggregateType());
                envelope.put("aggregateId", event.getAggregateId());
                envelope.put(
                        "occurredAt",
                        event.getCreatedAt().toString()
                );
                envelope.put(
                        "payload",
                        objectMapper.readValue(
                                event.getPayload(),
                                Object.class
                        )
                );

                String message =
                        objectMapper.writeValueAsString(envelope);

                kafkaTemplate.send(
                                event.getTopic(),
                                event.getAggregateId(),
                                message
                        )
                        .get(
                                sendTimeoutMs,
                                TimeUnit.MILLISECONDS
                        );

                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
                event.setLastError(null);
            }
            catch (Exception ex) {
                event.setAttempts(event.getAttempts() + 1);

                String message = ex.getMessage();

                if (message == null) {
                    message = ex.getClass().getSimpleName();
                }

                event.setLastError(
                        message.substring(
                                0,
                                Math.min(message.length(), 2000)
                        )
                );
            }
        }
    }
}
