package com.ecommerce.inventory_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateStockReservationRequest(
        @NotNull Long orderId,

        @NotEmpty
        List<@Valid ReserveStockItemRequest> items
) {
}
