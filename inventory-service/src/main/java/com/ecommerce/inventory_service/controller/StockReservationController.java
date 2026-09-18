package com.ecommerce.inventory_service.controller;

import com.ecommerce.inventory_service.dto.CreateStockReservationRequest;
import com.ecommerce.inventory_service.dto.StockReservationResponse;
import com.ecommerce.inventory_service.service.StockReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/reservations")
public class StockReservationController {

    private final StockReservationService reservationService;

    public StockReservationController(
            StockReservationService reservationService
    ) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<List<StockReservationResponse>> reserve(
            @Valid @RequestBody CreateStockReservationRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        reservationService.reserve(request)
                );
    }

    @PostMapping("/{orderId}/release")
    public List<StockReservationResponse> release(
            @PathVariable Long orderId
    ) {
        return reservationService
                .releaseByOrderId(orderId);
    }

    @PostMapping("/{orderId}/confirm")
    public List<StockReservationResponse> confirm(
            @PathVariable Long orderId
    ) {
        return reservationService
                .confirmByOrderId(orderId);
    }

    @PostMapping("/{orderId}/compensate")
    public List<StockReservationResponse> compensate(
            @PathVariable Long orderId
    ) {
        return reservationService
                .compensateByOrderId(orderId);
    }

    @GetMapping("/order/{orderId}")
    public List<StockReservationResponse> getByOrderId(
            @PathVariable Long orderId
    ) {
        return reservationService
                .getByOrderId(orderId);
    }
}
