package com.ecommerce.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateInventoryRequest(
        @NotNull Long productId,
        @Min(0) int quantity
) {
}
