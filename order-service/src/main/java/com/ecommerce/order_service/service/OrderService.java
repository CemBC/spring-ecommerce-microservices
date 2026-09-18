package com.ecommerce.order_service.service;

import com.ecommerce.order_service.client.InventoryClient;
import com.ecommerce.order_service.client.ProductClient;
import com.ecommerce.order_service.client.dto.ProductSnapshotResponse;
import com.ecommerce.order_service.dto.CreateOrderItemRequest;
import com.ecommerce.order_service.dto.CreateOrderRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.dto.OrderSummaryResponse;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderItem;
import com.ecommerce.order_service.entity.OrderStatus;
import com.ecommerce.order_service.exception.BadRequestException;
import com.ecommerce.order_service.exception.ConflictException;
import com.ecommerce.order_service.exception.DownstreamServiceUnavailableException;
import com.ecommerce.order_service.exception.ResourceNotFoundException;
import com.ecommerce.order_service.mapper.OrderMapper;
import com.ecommerce.order_service.outbox.OutboxService;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.specification.OrderSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    private OutboxService outboxService;

    public OrderService(
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            ProductClient productClient,
            InventoryClient inventoryClient
    ) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
    }

    @Autowired(required = false)
    void setOutboxService(
            OutboxService outboxService
    ) {
        this.outboxService = outboxService;
    }

    @Transactional
    public OrderResponse create(
            CreateOrderRequest request
    ) {
        validateNoDuplicateProducts(request);

        Order order = Order.builder()
                .userId(request.userId())
                .status(OrderStatus.PENDING)
                .totalAmount(
                        BigDecimal.ZERO.setScale(2)
                )
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest
                : request.items()) {

            ProductSnapshotResponse product =
                    productClient.getProduct(
                            itemRequest.productId()
                    );

            if (!Boolean.TRUE.equals(
                    product.active()
            )) {
                throw new ConflictException(
                        "Product is inactive with id: "
                                + itemRequest.productId()
                );
            }

            if (product.price() == null
                    || product.price()
                    .compareTo(BigDecimal.ZERO) <= 0
                    || product.name() == null
                    || product.name().isBlank()) {

                throw new DownstreamServiceUnavailableException(
                        "Product Service returned invalid product data for id: "
                                + itemRequest.productId()
                );
            }

            BigDecimal unitPrice =
                    money(product.price());

            BigDecimal subtotal =
                    money(
                            unitPrice.multiply(
                                    BigDecimal.valueOf(
                                            itemRequest.quantity()
                                    )
                            )
                    );

            OrderItem item = OrderItem.builder()
                    .productId(product.id())
                    .productName(
                            product.name().trim()
                    )
                    .quantity(
                            itemRequest.quantity()
                    )
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            order.addItem(item);

            total = total.add(subtotal);
        }

        order.setTotalAmount(
                money(total)
        );

        Order saved =
                orderRepository.saveAndFlush(order);

        inventoryClient.reserveOrder(
                saved.getId(),
                request.items()
        );

        emit(
                saved,
                "ORDER_CREATED"
        );

        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return orderMapper.toResponse(
                findOrder(id)
        );
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
                        .and(
                                OrderSpecification
                                        .hasStatus(status)
                        )
                        .and(
                                OrderSpecification
                                        .createdFrom(createdFrom)
                        )
                        .and(
                                OrderSpecification
                                        .createdTo(createdTo)
                        );

        return orderRepository
                .findAll(
                        specification,
                        pageable
                )
                .map(
                        orderMapper::toSummaryResponse
                );
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

        if (order.getStatus()
                != OrderStatus.PENDING) {
            throw new ConflictException(
                    "Only PENDING orders can be confirmed"
            );
        }

        inventoryClient.confirmOrder(
                order.getId()
        );

        order.setStatus(
                OrderStatus.CONFIRMED
        );

        emit(
                order,
                "ORDER_CONFIRMED"
        );

        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse complete(Long id) {
        Order order = findOrder(id);

        if (order.getStatus()
                != OrderStatus.CONFIRMED) {
            throw new ConflictException(
                    "Only CONFIRMED orders can be completed"
            );
        }

        order.setStatus(
                OrderStatus.COMPLETED
        );

        emit(
                order,
                "ORDER_COMPLETED"
        );

        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        Order order = findOrder(id);

        if (order.getStatus()
                == OrderStatus.CANCELLED) {
            throw new ConflictException(
                    "Order is already cancelled"
            );
        }

        if (order.getStatus()
                == OrderStatus.COMPLETED) {
            throw new ConflictException(
                    "Completed orders cannot be cancelled"
            );
        }

        if (order.getStatus()
                != OrderStatus.PENDING) {
            throw new ConflictException(
                    "Only PENDING orders can be cancelled"
            );
        }

        inventoryClient.releaseOrder(
                order.getId()
        );

        order.setStatus(
                OrderStatus.CANCELLED
        );

        emit(
                order,
                "ORDER_CANCELLED"
        );

        return orderMapper.toResponse(order);
    }

    private void emit(
            Order order,
            String eventType
    ) {
        if (outboxService == null) {
            return;
        }

        Map<String, Object> payload =
                new LinkedHashMap<>();

        payload.put(
                "orderId",
                order.getId()
        );
        payload.put(
                "userId",
                order.getUserId()
        );
        payload.put(
                "status",
                order.getStatus().name()
        );
        payload.put(
                "totalAmount",
                order.getTotalAmount()
        );

        outboxService.enqueue(
                "order.events",
                "Order",
                order.getId(),
                eventType,
                payload
        );
    }

    private void validateNoDuplicateProducts(
            CreateOrderRequest request
    ) {
        Set<Long> productIds =
                new HashSet<>();

        for (CreateOrderItemRequest item
                : request.items()) {

            if (!productIds.add(
                    item.productId()
            )) {
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
                                "Order not found with id: "
                                        + id
                        )
                );
    }

    private BigDecimal money(
            BigDecimal value
    ) {
        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }
}
