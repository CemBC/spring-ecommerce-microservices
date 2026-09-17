package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.CreateOrderItemRequest;
import com.ecommerce.order_service.dto.CreateOrderRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderStatus;
import com.ecommerce.order_service.exception.BadRequestException;
import com.ecommerce.order_service.exception.ConflictException;
import com.ecommerce.order_service.exception.ResourceNotFoundException;
import com.ecommerce.order_service.mapper.OrderMapper;
import com.ecommerce.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);

        orderService = new OrderService(
                orderRepository,
                new OrderMapper()
        );
    }

    @Test
    void shouldCreateOrderAndCalculateTotalsOnServer() {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        5L,
                        List.of(
                                new CreateOrderItemRequest(
                                        1L,
                                        "MacBook Air M3",
                                        2,
                                        new BigDecimal("45000")
                                ),
                                new CreateOrderItemRequest(
                                        2L,
                                        "Magic Mouse",
                                        1,
                                        new BigDecimal("3500")
                                )
                        )
                );

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(1L);
                    return order;
                });

        OrderResponse response =
                orderService.create(request);

        assertEquals(1L, response.id());
        assertEquals(5L, response.userId());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(
                new BigDecimal("93500.00"),
                response.totalAmount()
        );
        assertEquals(2, response.items().size());
        assertEquals(
                new BigDecimal("90000.00"),
                response.items().get(0).subtotal()
        );
    }

    @Test
    void shouldRejectDuplicateProductIds() {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        5L,
                        List.of(
                                new CreateOrderItemRequest(
                                        1L,
                                        "MacBook Air M3",
                                        1,
                                        new BigDecimal("45000")
                                ),
                                new CreateOrderItemRequest(
                                        1L,
                                        "MacBook Air M3",
                                        2,
                                        new BigDecimal("45000")
                                )
                        )
                );

        assertThrows(
                BadRequestException.class,
                () -> orderService.create(request)
        );

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void shouldConfirmPendingOrder() {
        Order order = orderWithStatus(
                OrderStatus.PENDING
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        OrderResponse response =
                orderService.confirm(1L);

        assertEquals(
                OrderStatus.CONFIRMED,
                response.status()
        );
    }

    @Test
    void shouldCompleteConfirmedOrder() {
        Order order = orderWithStatus(
                OrderStatus.CONFIRMED
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        OrderResponse response =
                orderService.complete(1L);

        assertEquals(
                OrderStatus.COMPLETED,
                response.status()
        );
    }

    @Test
    void shouldRejectCompletingPendingOrder() {
        Order order = orderWithStatus(
                OrderStatus.PENDING
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThrows(
                ConflictException.class,
                () -> orderService.complete(1L)
        );
    }

    @Test
    void shouldCancelConfirmedOrder() {
        Order order = orderWithStatus(
                OrderStatus.CONFIRMED
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        OrderResponse response =
                orderService.cancel(1L);

        assertEquals(
                OrderStatus.CANCELLED,
                response.status()
        );
    }

    @Test
    void shouldRejectCancellingCompletedOrder() {
        Order order = orderWithStatus(
                OrderStatus.COMPLETED
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThrows(
                ConflictException.class,
                () -> orderService.cancel(1L)
        );
    }

    @Test
    void shouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getById(999L)
        );
    }

    private Order orderWithStatus(
            OrderStatus status
    ) {
        return Order.builder()
                .id(1L)
                .userId(5L)
                .status(status)
                .totalAmount(
                        new BigDecimal("100.00")
                )
                .build();
    }
}
