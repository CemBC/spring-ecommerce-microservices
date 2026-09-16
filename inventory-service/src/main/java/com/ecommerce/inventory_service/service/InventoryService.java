package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.CreateInventoryRequest;
import com.ecommerce.inventory_service.dto.InventoryResponse;
import com.ecommerce.inventory_service.dto.StockAdjustmentRequest;
import com.ecommerce.inventory_service.entity.Inventory;
import com.ecommerce.inventory_service.exception.ConflictException;
import com.ecommerce.inventory_service.exception.InsufficientStockException;
import com.ecommerce.inventory_service.exception.ResourceNotFoundException;
import com.ecommerce.inventory_service.mapper.InventoryMapper;
import com.ecommerce.inventory_service.repository.InventoryRepository;
import com.ecommerce.inventory_service.specification.InventorySpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    public InventoryService(InventoryRepository inventoryRepository, InventoryMapper inventoryMapper) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryMapper = inventoryMapper;
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> getAll(
            Long productId,
            Integer minQuantity,
            Integer maxQuantity,
            Integer minAvailableQuantity,
            Integer maxAvailableQuantity,
            Boolean hasReservedStock,
            Pageable pageable
    ) {

        Specification<Inventory> specification =
                InventorySpecification
                        .hasProductId(productId)
                        .and(
                                InventorySpecification
                                        .minQuantity(minQuantity)
                        )
                        .and(
                                InventorySpecification
                                        .maxQuantity(maxQuantity)
                        )
                        .and(
                                InventorySpecification
                                        .minAvailableQuantity(
                                                minAvailableQuantity
                                        )
                        )
                        .and(
                                InventorySpecification
                                        .maxAvailableQuantity(
                                                maxAvailableQuantity
                                        )
                        )
                        .and(
                                InventorySpecification
                                        .hasReservedStock(
                                                hasReservedStock
                                        )
                        );

        return inventoryRepository
                .findAll(specification, pageable)
                .map(inventoryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(Long productId) {
        return inventoryMapper.toResponse(findByProductId(productId));
    }

    @Transactional
    public InventoryResponse create(CreateInventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new ConflictException("Inventory already exists for product id: " + request.productId());
        }

        Inventory inventory = Inventory.builder()
                .productId(request.productId())
                .quantity(request.quantity())
                .reservedQuantity(0)
                .build();

        return inventoryMapper.toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse increase(Long productId, StockAdjustmentRequest request) {
        Inventory inventory = findByProductId(productId);
        inventory.setQuantity(inventory.getQuantity() + request.quantity());
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse decrease(Long productId, StockAdjustmentRequest request) {
        Inventory inventory = findByProductId(productId);

        if (inventory.getAvailableQuantity() < request.quantity()) {
            throw new InsufficientStockException("Not enough available stock to decrease product id: " + productId);
        }

        inventory.setQuantity(inventory.getQuantity() - request.quantity());
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse reserve(Long productId, StockAdjustmentRequest request) {
        Inventory inventory = findByProductId(productId);

        if (inventory.getAvailableQuantity() < request.quantity()) {
            throw new InsufficientStockException("Insufficient stock for product id: " + productId);
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse release(Long productId, StockAdjustmentRequest request) {
        Inventory inventory = findByProductId(productId);

        if (inventory.getReservedQuantity() < request.quantity()) {
            throw new ConflictException("Cannot release more stock than currently reserved for product id: " + productId);
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() - request.quantity());
        return inventoryMapper.toResponse(inventory);
    }

    private Inventory findByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product id: " + productId
                ));
    }
}
