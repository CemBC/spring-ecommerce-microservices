package com.ecommerce.payment_service.specification;

import com.ecommerce.payment_service.entity.Payment;
import com.ecommerce.payment_service.entity.PaymentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class PaymentSpecification {

    private PaymentSpecification() {}

    public static Specification<Payment> hasOrderId(Long orderId) {
        return (root, query, cb) ->
                orderId == null ? cb.conjunction()
                        : cb.equal(root.get("orderId"), orderId);
    }

    public static Specification<Payment> hasUserId(Long userId) {
        return (root, query, cb) ->
                userId == null ? cb.conjunction()
                        : cb.equal(root.get("userId"), userId);
    }

    public static Specification<Payment> hasStatus(PaymentStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Payment> hasCurrency(String currency) {
        return (root, query, cb) ->
                currency == null || currency.isBlank()
                        ? cb.conjunction()
                        : cb.equal(
                                root.get("currency"),
                                currency.trim().toUpperCase()
                        );
    }

    public static Specification<Payment> createdFrom(LocalDateTime createdFrom) {
        return (root, query, cb) ->
                createdFrom == null ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                                root.<LocalDateTime>get("createdAt"),
                                createdFrom
                        );
    }

    public static Specification<Payment> createdTo(LocalDateTime createdTo) {
        return (root, query, cb) ->
                createdTo == null ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                                root.<LocalDateTime>get("createdAt"),
                                createdTo
                        );
    }
}
