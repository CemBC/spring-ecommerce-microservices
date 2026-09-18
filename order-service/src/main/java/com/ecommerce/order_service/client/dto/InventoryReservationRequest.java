package com.ecommerce.order_service.client.dto;

import java.util.List;

public record InventoryReservationRequest(
        Long orderId,
        List<InventoryReservationItemRequest> items
) {
}
