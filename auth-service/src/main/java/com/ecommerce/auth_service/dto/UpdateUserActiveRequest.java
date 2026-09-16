package com.ecommerce.auth_service.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserActiveRequest(
        @NotNull
        Boolean active
) {
}
