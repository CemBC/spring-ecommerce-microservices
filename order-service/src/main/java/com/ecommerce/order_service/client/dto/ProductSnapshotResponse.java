package com.ecommerce.order_service.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductSnapshotResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String sku,
        Boolean active,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
