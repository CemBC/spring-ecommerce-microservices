package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.CreateStockReservationRequest;
import com.ecommerce.inventory_service.dto.ReserveStockItemRequest;
import com.ecommerce.inventory_service.dto.StockReservationResponse;
import com.ecommerce.inventory_service.entity.Inventory;
import com.ecommerce.inventory_service.entity.ReservationStatus;
import com.ecommerce.inventory_service.entity.StockReservation;
import com.ecommerce.inventory_service.exception.ConflictException;
import com.ecommerce.inventory_service.exception.InsufficientStockException;
import com.ecommerce.inventory_service.exception.ResourceNotFoundException;
import com.ecommerce.inventory_service.outbox.OutboxService;
import com.ecommerce.inventory_service.repository.InventoryRepository;
import com.ecommerce.inventory_service.repository.StockReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StockReservationService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;

    private OutboxService outboxService;

    public StockReservationService(
            InventoryRepository inventoryRepository,
            StockReservationRepository reservationRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    @Autowired(required = false)
    void setOutboxService(
            OutboxService outboxService
    ) {
        this.outboxService = outboxService;
    }

    @Transactional
    public List<StockReservationResponse> reserve(
            CreateStockReservationRequest request
    ) {
        validateNoDuplicateProducts(request.items());

        List<ReserveStockItemRequest> orderedItems =
                request.items()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        ReserveStockItemRequest::productId
                                )
                        )
                        .toList();

        List<StockReservation> created =
                new ArrayList<>();

        for (ReserveStockItemRequest item : orderedItems) {
            if (reservationRepository
                    .existsByOrderIdAndProductId(
                            request.orderId(),
                            item.productId()
                    )) {
                throw new ConflictException(
                        "Stock reservation already exists for order "
                                + request.orderId()
                                + " and product "
                                + item.productId()
                );
            }

            Inventory inventory =
                    inventoryRepository
                            .findByProductIdForUpdate(
                                    item.productId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Inventory not found for product id: "
                                                    + item.productId()
                                    )
                            );

            if (inventory.getAvailableQuantity()
                    < item.quantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product id: "
                                + item.productId()
                );
            }

            inventory.setReservedQuantity(
                    inventory.getReservedQuantity()
                            + item.quantity()
            );

            created.add(
                    StockReservation.builder()
                            .orderId(request.orderId())
                            .productId(item.productId())
                            .quantity(item.quantity())
                            .status(ReservationStatus.RESERVED)
                            .build()
            );
        }

        List<StockReservation> saved =
                reservationRepository.saveAll(created);

        emit(
                request.orderId(),
                "INVENTORY_RESERVED",
                saved
        );

        return saved.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<StockReservationResponse> releaseByOrderId(
            Long orderId
    ) {
        List<StockReservation> reservations =
                reservationRepository
                        .findByOrderIdAndStatusOrderByProductIdAsc(
                                orderId,
                                ReservationStatus.RESERVED
                        );

        for (StockReservation reservation : reservations) {
            Inventory inventory =
                    inventoryRepository
                            .findByProductIdForUpdate(
                                    reservation.getProductId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Inventory not found for product id: "
                                                    + reservation.getProductId()
                                    )
                            );

            if (inventory.getReservedQuantity()
                    < reservation.getQuantity()) {
                throw new ConflictException(
                        "Reserved stock is inconsistent for product id: "
                                + reservation.getProductId()
                );
            }

            inventory.setReservedQuantity(
                    inventory.getReservedQuantity()
                            - reservation.getQuantity()
            );

            reservation.setStatus(
                    ReservationStatus.RELEASED
            );
        }

        if (!reservations.isEmpty()) {
            emit(
                    orderId,
                    "INVENTORY_RELEASED",
                    reservations
            );
        }

        return reservations.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<StockReservationResponse> confirmByOrderId(
            Long orderId
    ) {
        List<StockReservation> reservations =
                reservationRepository
                        .findByOrderIdAndStatusOrderByProductIdAsc(
                                orderId,
                                ReservationStatus.RESERVED
                        );

        for (StockReservation reservation : reservations) {
            Inventory inventory =
                    inventoryRepository
                            .findByProductIdForUpdate(
                                    reservation.getProductId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Inventory not found for product id: "
                                                    + reservation.getProductId()
                                    )
                            );

            if (inventory.getReservedQuantity()
                    < reservation.getQuantity()
                    || inventory.getQuantity()
                    < reservation.getQuantity()) {
                throw new ConflictException(
                        "Inventory is inconsistent for product id: "
                                + reservation.getProductId()
                );
            }

            inventory.setReservedQuantity(
                    inventory.getReservedQuantity()
                            - reservation.getQuantity()
            );

            inventory.setQuantity(
                    inventory.getQuantity()
                            - reservation.getQuantity()
            );

            reservation.setStatus(
                    ReservationStatus.CONFIRMED
            );
        }

        if (!reservations.isEmpty()) {
            emit(
                    orderId,
                    "INVENTORY_CONFIRMED",
                    reservations
            );
        }

        return reservations.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<StockReservationResponse> compensateByOrderId(
            Long orderId
    ) {
        List<StockReservation> reservations =
                reservationRepository
                        .findByOrderIdOrderByProductIdAsc(
                                orderId
                        );

        List<StockReservation> compensated =
                new ArrayList<>();

        for (StockReservation reservation : reservations) {
            if (reservation.getStatus()
                    == ReservationStatus.COMPENSATED
                    || reservation.getStatus()
                    == ReservationStatus.RELEASED) {
                continue;
            }

            Inventory inventory =
                    inventoryRepository
                            .findByProductIdForUpdate(
                                    reservation.getProductId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Inventory not found for product id: "
                                                    + reservation.getProductId()
                                    )
                            );

            if (reservation.getStatus()
                    == ReservationStatus.RESERVED) {

                if (inventory.getReservedQuantity()
                        < reservation.getQuantity()) {
                    throw new ConflictException(
                            "Reserved stock is inconsistent for product id: "
                                    + reservation.getProductId()
                    );
                }

                inventory.setReservedQuantity(
                        inventory.getReservedQuantity()
                                - reservation.getQuantity()
                );

            } else if (reservation.getStatus()
                    == ReservationStatus.CONFIRMED) {

                inventory.setQuantity(
                        inventory.getQuantity()
                                + reservation.getQuantity()
                );
            }

            reservation.setStatus(
                    ReservationStatus.COMPENSATED
            );

            compensated.add(reservation);
        }

        if (!compensated.isEmpty()) {
            emit(
                    orderId,
                    "INVENTORY_COMPENSATED",
                    compensated
            );
        }

        return compensated.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockReservationResponse> getByOrderId(
            Long orderId
    ) {
        return reservationRepository
                .findByOrderIdOrderByProductIdAsc(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void emit(
            Long orderId,
            String eventType,
            List<StockReservation> reservations
    ) {
        if (outboxService == null) {
            return;
        }

        List<Map<String, Object>> items =
                reservations.stream()
                        .map(reservation -> {
                            Map<String, Object> item =
                                    new LinkedHashMap<>();

                            item.put(
                                    "productId",
                                    reservation.getProductId()
                            );
                            item.put(
                                    "quantity",
                                    reservation.getQuantity()
                            );
                            item.put(
                                    "status",
                                    reservation
                                            .getStatus()
                                            .name()
                            );

                            return item;
                        })
                        .toList();

        Map<String, Object> payload =
                new LinkedHashMap<>();

        payload.put("orderId", orderId);
        payload.put("items", items);

        outboxService.enqueue(
                "inventory.events",
                "StockReservation",
                orderId,
                eventType,
                payload
        );
    }

    private void validateNoDuplicateProducts(
            List<ReserveStockItemRequest> items
    ) {
        Set<Long> productIds = new HashSet<>();

        for (ReserveStockItemRequest item : items) {
            if (!productIds.add(item.productId())) {
                throw new ConflictException(
                        "Duplicate productId in stock reservation request: "
                                + item.productId()
                );
            }
        }
    }

    private StockReservationResponse toResponse(
            StockReservation reservation
    ) {
        return new StockReservationResponse(
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getProductId(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getVersion(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
