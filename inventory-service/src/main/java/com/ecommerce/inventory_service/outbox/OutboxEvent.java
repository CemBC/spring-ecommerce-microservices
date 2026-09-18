package com.ecommerce.inventory_service.outbox;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "outbox_events",
        indexes = @Index(
                name = "idx_outbox_pending",
                columnList = "published, created_at"
        )
)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(nullable = false, length = 120)
    private String topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String value) { aggregateType = value; }
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String value) { aggregateId = value; }
    public String getEventType() { return eventType; }
    public void setEventType(String value) { eventType = value; }
    public String getTopic() { return topic; }
    public void setTopic(String value) { topic = value; }
    public String getPayload() { return payload; }
    public void setPayload(String value) { payload = value; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean value) { published = value; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int value) { attempts = value; }
    public String getLastError() { return lastError; }
    public void setLastError(String value) { lastError = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime value) { publishedAt = value; }
}
