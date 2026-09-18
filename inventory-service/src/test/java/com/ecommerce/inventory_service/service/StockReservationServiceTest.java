package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.CreateStockReservationRequest;
import com.ecommerce.inventory_service.dto.ReserveStockItemRequest;
import com.ecommerce.inventory_service.entity.Inventory;
import com.ecommerce.inventory_service.entity.ReservationStatus;
import com.ecommerce.inventory_service.entity.StockReservation;
import com.ecommerce.inventory_service.exception.InsufficientStockException;
import com.ecommerce.inventory_service.repository.InventoryRepository;
import com.ecommerce.inventory_service.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class StockReservationServiceTest {

    private InventoryRepository inventoryRepository;
    private StockReservationRepository reservationRepository;
    private StockReservationService service;

    @BeforeEach
    void setUp() {
        inventoryRepository =
                mock(InventoryRepository.class);

        reservationRepository =
                mock(StockReservationRepository.class);

        service = new StockReservationService(
                inventoryRepository,
                reservationRepository
        );
    }

    @Test
    void shouldReserveStockForOrder() {
        Inventory inventory = Inventory.builder()
                .id(1L)
                .productId(2L)
                .quantity(100)
                .reservedQuantity(0)
                .version(0L)
                .build();

        when(
                reservationRepository
                        .existsByOrderIdAndProductId(
                                10L,
                                2L
                        )
        ).thenReturn(false);

        when(
                inventoryRepository
                        .findByProductIdForUpdate(2L)
        ).thenReturn(
                Optional.of(inventory)
        );

        when(
                reservationRepository
                        .saveAll(anyList())
        ).thenAnswer(invocation -> {
            List<StockReservation> reservations =
                    invocation.getArgument(0);

            reservations.getFirst().setId(1L);
            reservations.getFirst().setVersion(0L);

            return reservations;
        });

        var response = service.reserve(
                new CreateStockReservationRequest(
                        10L,
                        List.of(
                                new ReserveStockItemRequest(
                                        2L,
                                        3
                                )
                        )
                )
        );

        assertEquals(
                3,
                inventory.getReservedQuantity()
        );

        assertEquals(
                ReservationStatus.RESERVED,
                response.getFirst().status()
        );
    }

    @Test
    void shouldRejectReservationWhenStockIsInsufficient() {
        Inventory inventory = Inventory.builder()
                .id(1L)
                .productId(2L)
                .quantity(5)
                .reservedQuantity(4)
                .version(0L)
                .build();

        when(
                inventoryRepository
                        .findByProductIdForUpdate(2L)
        ).thenReturn(
                Optional.of(inventory)
        );

        assertThrows(
                InsufficientStockException.class,
                () -> service.reserve(
                        new CreateStockReservationRequest(
                                10L,
                                List.of(
                                        new ReserveStockItemRequest(
                                                2L,
                                                2
                                        )
                                )
                        )
                )
        );
    }
}
