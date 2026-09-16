package com.ecommerce.inventory_service.controller;

import com.ecommerce.inventory_service.dto.CreateInventoryRequest;
import com.ecommerce.inventory_service.dto.InventoryResponse;
import com.ecommerce.inventory_service.dto.StockAdjustmentRequest;
import com.ecommerce.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public Page<InventoryResponse> getAll(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer minQuantity,
            @RequestParam(required = false) Integer maxQuantity,
            @RequestParam(required = false) Integer minAvailableQuantity,
            @RequestParam(required = false) Integer maxAvailableQuantity,
            @RequestParam(required = false) Boolean hasReservedStock,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return inventoryService.getAll(
                productId,
                minQuantity,
                maxQuantity,
                minAvailableQuantity,
                maxAvailableQuantity,
                hasReservedStock,
                pageable
        );
    }

    @GetMapping("/{productId}")
    public InventoryResponse getByProductId(@PathVariable Long productId) {
        return inventoryService.getByProductId(productId);
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> create(
            @Valid @RequestBody CreateInventoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.create(request));
    }

    @PostMapping("/{productId}/increase")
    public InventoryResponse increase(
            @PathVariable Long productId,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        return inventoryService.increase(productId, request);
    }

    @PostMapping("/{productId}/decrease")
    public InventoryResponse decrease(
            @PathVariable Long productId,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        return inventoryService.decrease(productId, request);
    }

    @PostMapping("/{productId}/reserve")
    public InventoryResponse reserve(
            @PathVariable Long productId,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        return inventoryService.reserve(productId, request);
    }

    @PostMapping("/{productId}/release")
    public InventoryResponse release(
            @PathVariable Long productId,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        return inventoryService.release(productId, request);
    }
}
