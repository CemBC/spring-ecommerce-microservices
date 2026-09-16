package com.ecommerce.inventory_service.exception;

import java.time.LocalDateTime;

public record ApiError(
        int status,
        String message,
        LocalDateTime timestamp
) {
}
