package com.ecommerce.order_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(
        @NotNull
        Long productId,

        @NotNull
        @Positive
        Integer quantity
) {
}
