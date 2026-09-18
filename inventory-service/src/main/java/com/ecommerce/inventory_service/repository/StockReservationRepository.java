package com.ecommerce.inventory_service.repository;

import com.ecommerce.inventory_service.entity.ReservationStatus;
import com.ecommerce.inventory_service.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockReservationRepository
        extends JpaRepository<StockReservation, Long> {

    boolean existsByOrderIdAndProductId(
            Long orderId,
            Long productId
    );

    List<StockReservation> findByOrderIdOrderByProductIdAsc(
            Long orderId
    );

    List<StockReservation> findByOrderIdAndStatusOrderByProductIdAsc(
            Long orderId,
            ReservationStatus status
    );
}
