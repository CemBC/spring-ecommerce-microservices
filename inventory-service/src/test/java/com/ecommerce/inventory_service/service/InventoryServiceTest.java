package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.CreateInventoryRequest;
import com.ecommerce.inventory_service.dto.InventoryResponse;
import com.ecommerce.inventory_service.dto.StockAdjustmentRequest;
import com.ecommerce.inventory_service.entity.Inventory;
import com.ecommerce.inventory_service.exception.ConflictException;
import com.ecommerce.inventory_service.exception.InsufficientStockException;
import com.ecommerce.inventory_service.mapper.InventoryMapper;
import com.ecommerce.inventory_service.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryServiceTest {

    private InventoryRepository repository;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        repository = mock(InventoryRepository.class);
        service = new InventoryService(repository, new InventoryMapper());
    }

    @Test
    void shouldCreateInventory() {
        when(repository.existsByProductId(1L)).thenReturn(false);
        when(repository.save(any(Inventory.class))).thenAnswer(invocation -> {
            Inventory inventory = invocation.getArgument(0);
            inventory.setId(1L);
            inventory.setVersion(0L);
            return inventory;
        });

        InventoryResponse response = service.create(new CreateInventoryRequest(1L, 100));

        assertEquals(1L, response.productId());
        assertEquals(100, response.quantity());
        assertEquals(100, response.availableQuantity());
    }

    @Test
    void shouldRejectDuplicateInventory() {
        when(repository.existsByProductId(1L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(new CreateInventoryRequest(1L, 10)));
    }

    @Test
    void shouldReserveStock() {
        Inventory inventory = Inventory.builder()
                .id(1L).productId(1L).quantity(10).reservedQuantity(2).version(0L).build();
        when(repository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        InventoryResponse response = service.reserve(1L, new StockAdjustmentRequest(3));

        assertEquals(5, response.reservedQuantity());
        assertEquals(5, response.availableQuantity());
    }

    @Test
    void shouldRejectReservationWhenStockIsInsufficient() {
        Inventory inventory = Inventory.builder()
                .id(1L).productId(1L).quantity(5).reservedQuantity(4).version(0L).build();
        when(repository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientStockException.class,
                () -> service.reserve(1L, new StockAdjustmentRequest(2)));
    }
}
