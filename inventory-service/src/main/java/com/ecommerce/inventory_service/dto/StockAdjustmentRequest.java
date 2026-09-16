package com.ecommerce.inventory_service.dto;

import jakarta.validation.constraints.Min;

public record StockAdjustmentRequest(
        @Min(1) int quantity
) {
}
