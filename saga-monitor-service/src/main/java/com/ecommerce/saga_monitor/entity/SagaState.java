package com.ecommerce.saga_monitor.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saga_states")
public class SagaState {

    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_created", nullable = false)
    private boolean orderCreated;

    @Column(name = "inventory_reserved", nullable = false)
    private boolean inventoryReserved;

    @Column(name = "inventory_confirmed", nullable = false)
    private boolean inventoryConfirmed;

    @Column(name = "order_confirmed", nullable = false)
    private boolean orderConfirmed;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "payment_status", length = 40)
    private String paymentStatus;

    @Column(name = "terminal_status", nullable = false, length = 40)
    private String terminalStatus;

    @Column(name = "last_event_type", length = 120)
    private String lastEventType;

    @Column(name = "last_event_at", nullable = false)
    private LocalDateTime lastEventAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (terminalStatus == null) {
            terminalStatus = "IN_PROGRESS";
        }

        if (lastEventAt == null) {
            lastEventAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { orderId = value; }
    public boolean isOrderCreated() { return orderCreated; }
    public void setOrderCreated(boolean value) { orderCreated = value; }
    public boolean isInventoryReserved() { return inventoryReserved; }
    public void setInventoryReserved(boolean value) { inventoryReserved = value; }
    public boolean isInventoryConfirmed() { return inventoryConfirmed; }
    public void setInventoryConfirmed(boolean value) { inventoryConfirmed = value; }
    public boolean isOrderConfirmed() { return orderConfirmed; }
    public void setOrderConfirmed(boolean value) { orderConfirmed = value; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long value) { paymentId = value; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String value) { paymentStatus = value; }
    public String getTerminalStatus() { return terminalStatus; }
    public void setTerminalStatus(String value) { terminalStatus = value; }
    public String getLastEventType() { return lastEventType; }
    public void setLastEventType(String value) { lastEventType = value; }
    public LocalDateTime getLastEventAt() { return lastEventAt; }
    public void setLastEventAt(LocalDateTime value) { lastEventAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
