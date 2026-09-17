package com.ecommerce.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FailPaymentRequest(
        @NotBlank @Size(max = 500) String failureReason
) {
}
