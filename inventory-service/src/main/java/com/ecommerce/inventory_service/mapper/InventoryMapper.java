package com.ecommerce.inventory_service.mapper;

import com.ecommerce.inventory_service.dto.InventoryResponse;
import com.ecommerce.inventory_service.entity.Inventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {
    public InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getVersion(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}
