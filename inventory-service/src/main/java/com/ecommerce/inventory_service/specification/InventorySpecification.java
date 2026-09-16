package com.ecommerce.inventory_service.specification;

import com.ecommerce.inventory_service.entity.Inventory;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

public final class InventorySpecification {

    private InventorySpecification() {
    }

    public static Specification<Inventory> hasProductId(Long productId) {
        return (root, query, cb) ->
                productId == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("productId"),
                        productId
                );
    }

    public static Specification<Inventory> minQuantity(Integer minQuantity) {
        return (root, query, cb) ->
                minQuantity == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                        root.get("quantity"),
                        minQuantity
                );
    }

    public static Specification<Inventory> maxQuantity(Integer maxQuantity) {
        return (root, query, cb) ->
                maxQuantity == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                        root.get("quantity"),
                        maxQuantity
                );
    }

    public static Specification<Inventory> minAvailableQuantity(
            Integer minAvailableQuantity
    ) {
        return (root, query, cb) -> {

            if (minAvailableQuantity == null) {
                return cb.conjunction();
            }

            Expression<Integer> availableQuantity =
                    cb.diff(
                            root.<Integer>get("quantity"),
                            root.<Integer>get("reservedQuantity")
                    );

            return cb.greaterThanOrEqualTo(
                    availableQuantity,
                    minAvailableQuantity
            );
        };
    }

    public static Specification<Inventory> maxAvailableQuantity(
            Integer maxAvailableQuantity
    ) {
        return (root, query, cb) -> {

            if (maxAvailableQuantity == null) {
                return cb.conjunction();
            }

            Expression<Integer> availableQuantity =
                    cb.diff(
                            root.<Integer>get("quantity"),
                            root.<Integer>get("reservedQuantity")
                    );

            return cb.lessThanOrEqualTo(
                    availableQuantity,
                    maxAvailableQuantity
            );
        };
    }

    public static Specification<Inventory> hasReservedStock(
            Boolean hasReservedStock
    ) {
        return (root, query, cb) -> {

            if (hasReservedStock == null) {
                return cb.conjunction();
            }

            return hasReservedStock
                    ? cb.greaterThan(
                    root.get("reservedQuantity"),
                    0
            )
                    : cb.equal(
                    root.get("reservedQuantity"),
                    0
            );
        };
    }
}