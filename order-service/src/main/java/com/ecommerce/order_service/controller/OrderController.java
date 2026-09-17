package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.entity.OrderStatus;
import com.ecommerce.order_service.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.create(request));
    }

    @GetMapping("/{id}")
    public OrderResponse getById(
            @PathVariable Long id
    ) {
        return orderService.getById(id);
    }

    @GetMapping
    public Page<OrderSummaryResponse> getAll(
            @RequestParam(required = false)
            Long userId,

            @RequestParam(required = false)
            OrderStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdTo,

            @PageableDefault(
                    size = 10,
                    sort = "id"
            )
            Pageable pageable
    ) {
        return orderService.getAll(
                userId,
                status,
                createdFrom,
                createdTo,
                pageable
        );
    }

    @GetMapping("/user/{userId}")
    public Page<OrderSummaryResponse> getByUserId(
            @PathVariable Long userId,
            @PageableDefault(
                    size = 10,
                    sort = "id"
            )
            Pageable pageable
    ) {
        return orderService.getByUserId(
                userId,
                pageable
        );
    }

    @PatchMapping("/{id}/confirm")
    public OrderResponse confirm(
            @PathVariable Long id
    ) {
        return orderService.confirm(id);
    }

    @PatchMapping("/{id}/complete")
    public OrderResponse complete(
            @PathVariable Long id
    ) {
        return orderService.complete(id);
    }

    @PatchMapping("/{id}/cancel")
    public OrderResponse cancel(
            @PathVariable Long id
    ) {
        return orderService.cancel(id);
    }
}
