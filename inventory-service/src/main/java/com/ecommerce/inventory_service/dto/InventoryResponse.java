package com.ecommerce.inventory_service.dto;

import java.time.LocalDateTime;

public record InventoryResponse(
        Long id,
        Long productId,
        int quantity,
        int reservedQuantity,
        int availableQuantity,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
