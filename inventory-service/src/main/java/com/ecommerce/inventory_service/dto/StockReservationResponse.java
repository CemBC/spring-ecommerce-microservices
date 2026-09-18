package com.ecommerce.inventory_service.dto;

import com.ecommerce.inventory_service.entity.ReservationStatus;

import java.time.LocalDateTime;

public record StockReservationResponse(
        Long id,
        Long orderId,
        Long productId,
        Integer quantity,
        ReservationStatus status,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
