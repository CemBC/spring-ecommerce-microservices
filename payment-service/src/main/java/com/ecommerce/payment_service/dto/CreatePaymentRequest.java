package com.ecommerce.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreatePaymentRequest(
        @NotNull Long orderId,

        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{3}$")
        String currency
) {
}
