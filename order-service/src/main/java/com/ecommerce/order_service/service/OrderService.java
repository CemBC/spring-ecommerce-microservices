package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderItem;
import com.ecommerce.order_service.entity.OrderStatus;
import com.ecommerce.order_service.exception.BadRequestException;
import com.ecommerce.order_service.exception.ConflictException;
import com.ecommerce.order_service.exception.ResourceNotFoundException;
import com.ecommerce.order_service.mapper.OrderMapper;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.specification.OrderSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderService(
            OrderRepository orderRepository,
            OrderMapper orderMapper
    ) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        validateNoDuplicateProducts(request);

        Order order = Order.builder()
                .userId(request.userId())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO.setScale(2))
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : request.items()) {
            BigDecimal unitPrice = money(itemRequest.unitPrice());

            BigDecimal subtotal = money(
                    unitPrice.multiply(
                            BigDecimal.valueOf(itemRequest.quantity())
                    )
            );

            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.productId())
                    .productName(itemRequest.productName().trim())
                    .quantity(itemRequest.quantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            order.addItem(item);
            total = total.add(subtotal);
        }

        order.setTotalAmount(money(total));

        Order saved = orderRepository.save(order);

        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return orderMapper.toResponse(findOrder(id));
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAll(
            Long userId,
            OrderStatus status,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            Pageable pageable
    ) {

        if (createdFrom != null
                && createdTo != null
                && createdFrom.isAfter(createdTo)) {
            throw new BadRequestException(
                    "createdFrom cannot be after createdTo"
            );
        }

        Specification<Order> specification =
                OrderSpecification
                        .hasUserId(userId)
                        .and(OrderSpecification.hasStatus(status))
                        .and(OrderSpecification.createdFrom(createdFrom))
                        .and(OrderSpecification.createdTo(createdTo));

        return orderRepository
                .findAll(specification, pageable)
                .map(orderMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getByUserId(
            Long userId,
            Pageable pageable
    ) {
        return getAll(
                userId,
                null,
                null,
                null,
                pageable
        );
    }

    @Transactional
    public OrderResponse confirm(Long id) {
        Order order = findOrder(id);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException(
                    "Only PENDING orders can be confirmed"
            );
        }

        order.setStatus(OrderStatus.CONFIRMED);

        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse complete(Long id) {
        Order order = findOrder(id);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ConflictException(
                    "Only CONFIRMED orders can be completed"
            );
        }

        order.setStatus(OrderStatus.COMPLETED);

        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        Order order = findOrder(id);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException(
                    "Order is already cancelled"
            );
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new ConflictException(
                    "Completed orders cannot be cancelled"
            );
        }

        order.setStatus(OrderStatus.CANCELLED);

        return orderMapper.toResponse(order);
    }

    private void validateNoDuplicateProducts(
            CreateOrderRequest request
    ) {
        Set<Long> productIds = new HashSet<>();

        for (CreateOrderItemRequest item : request.items()) {
            if (!productIds.add(item.productId())) {
                throw new BadRequestException(
                        "Duplicate productId in order items: "
                                + item.productId()
                );
            }
        }
    }

    private Order findOrder(Long id) {
        return orderRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with id: " + id
                        )
                );
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }
}
