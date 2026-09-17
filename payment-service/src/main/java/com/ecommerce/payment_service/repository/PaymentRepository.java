package com.ecommerce.payment_service.repository;

import com.ecommerce.payment_service.entity.Payment;
import com.ecommerce.payment_service.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long>,
                JpaSpecificationExecutor<Payment> {

    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

    boolean existsByOrderIdAndStatusIn(
            Long orderId,
            Collection<PaymentStatus> statuses
    );

    boolean existsByTransactionReference(String transactionReference);
}
