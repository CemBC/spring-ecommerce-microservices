package com.ecommerce.payment_service.dto;

import com.ecommerce.payment_service.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        Long userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String provider,
        String transactionReference,
        String failureReason,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
