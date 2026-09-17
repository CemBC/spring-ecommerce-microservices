package com.ecommerce.order_service.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateOrderItemRequest(
        @NotNull
        Long productId,

        @NotBlank
        @Size(max = 200)
        String productName,

        @NotNull
        @Positive
        Integer quantity,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal unitPrice
) {
}
