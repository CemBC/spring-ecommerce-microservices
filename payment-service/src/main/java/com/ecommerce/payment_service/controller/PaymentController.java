package com.ecommerce.payment_service.controller;

import com.ecommerce.payment_service.dto.*;
import com.ecommerce.payment_service.entity.PaymentStatus;
import com.ecommerce.payment_service.service.PaymentService;
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
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.create(request));
    }

    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable Long id) {
        return paymentService.getById(id);
    }

    @GetMapping("/order/{orderId}")
    public PaymentResponse getLatestByOrderId(
            @PathVariable Long orderId
    ) {
        return paymentService.getLatestByOrderId(orderId);
    }

    @GetMapping
    public Page<PaymentResponse> getAll(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdTo,
            @PageableDefault(size = 10, sort = "id")
            Pageable pageable
    ) {
        return paymentService.getAll(
                orderId,
                userId,
                status,
                currency,
                createdFrom,
                createdTo,
                pageable
        );
    }

    @PatchMapping("/{id}/process")
    public PaymentResponse process(@PathVariable Long id) {
        return paymentService.process(id);
    }

    @PatchMapping("/{id}/succeed")
    public PaymentResponse succeed(@PathVariable Long id) {
        return paymentService.succeed(id);
    }

    @PatchMapping("/{id}/fail")
    public PaymentResponse fail(
            @PathVariable Long id,
            @Valid @RequestBody FailPaymentRequest request
    ) {
        return paymentService.fail(id, request);
    }

    @PatchMapping("/{id}/cancel")
    public PaymentResponse cancel(@PathVariable Long id) {
        return paymentService.cancel(id);
    }

    @PatchMapping("/{id}/refund")
    public PaymentResponse refund(@PathVariable Long id) {
        return paymentService.refund(id);
    }
}
