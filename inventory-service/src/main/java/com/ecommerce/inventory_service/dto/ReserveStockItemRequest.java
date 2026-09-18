package com.ecommerce.inventory_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReserveStockItemRequest(
        @NotNull Long productId,
        @NotNull @Positive Integer quantity
) {
}
