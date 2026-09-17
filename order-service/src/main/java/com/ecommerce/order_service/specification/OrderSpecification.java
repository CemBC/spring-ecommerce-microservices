package com.ecommerce.order_service.specification;

import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class OrderSpecification {

    private OrderSpecification() {
    }

    public static Specification<Order> hasUserId(Long userId) {
        return (root, query, cb) ->
                userId == null
                        ? cb.conjunction()
                        : cb.equal(root.get("userId"), userId);
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) ->
                status == null
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> createdFrom(LocalDateTime createdFrom) {
        return (root, query, cb) ->
                createdFrom == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                                root.<LocalDateTime>get("createdAt"),
                                createdFrom
                        );
    }

    public static Specification<Order> createdTo(LocalDateTime createdTo) {
        return (root, query, cb) ->
                createdTo == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                                root.<LocalDateTime>get("createdAt"),
                                createdTo
                        );
    }
}
