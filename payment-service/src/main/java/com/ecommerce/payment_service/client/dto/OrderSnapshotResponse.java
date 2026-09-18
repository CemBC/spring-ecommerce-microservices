package com.ecommerce.payment_service.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderSnapshotResponse(
        Long id,
        Long userId,
        String status,
        BigDecimal totalAmount,
        List<?> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
