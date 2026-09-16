package com.ecommerce.inventory_service.repository;

import com.ecommerce.inventory_service.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    Optional<Inventory> findByProductId(Long productId);

    boolean existsByProductId(Long productId);
}