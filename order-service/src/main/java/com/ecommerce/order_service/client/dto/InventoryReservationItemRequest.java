package com.ecommerce.order_service.client.dto;

public record InventoryReservationItemRequest(
        Long productId,
        Integer quantity
) {
}
