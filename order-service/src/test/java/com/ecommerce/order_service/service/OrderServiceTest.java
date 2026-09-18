package com.ecommerce.order_service.service;

import com.ecommerce.order_service.client.InventoryClient;
import com.ecommerce.order_service.client.ProductClient;
import com.ecommerce.order_service.client.dto.ProductSnapshotResponse;
import com.ecommerce.order_service.dto.CreateOrderItemRequest;
import com.ecommerce.order_service.dto.CreateOrderRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderStatus;
import com.ecommerce.order_service.exception.BadRequestException;
import com.ecommerce.order_service.exception.ConflictException;
import com.ecommerce.order_service.exception.DownstreamServiceUnavailableException;
import com.ecommerce.order_service.exception.ResourceNotFoundException;
import com.ecommerce.order_service.mapper.OrderMapper;
import com.ecommerce.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private ProductClient productClient;
    private InventoryClient inventoryClient;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productClient = mock(ProductClient.class);
        inventoryClient = mock(InventoryClient.class);

        orderService = new OrderService(
                orderRepository,
                new OrderMapper(),
                productClient,
                inventoryClient
        );
    }

    @Test
    void shouldCreateOrderUsingAuthoritativeProductDataAndReserveInventory() {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        5L,
                        List.of(
                                new CreateOrderItemRequest(1L, 2),
                                new CreateOrderItemRequest(2L, 1)
                        )
                );

        when(productClient.getProduct(1L))
                .thenReturn(
                        product(
                                1L,
                                "MacBook Air M3",
                                "45000.00",
                                true
                        )
                );

        when(productClient.getProduct(2L))
                .thenReturn(
                        product(
                                2L,
                                "Magic Mouse",
                                "3500.00",
                                true
                        )
                );

        when(
                orderRepository.saveAndFlush(
                        any(Order.class)
                )
        ).thenAnswer(invocation -> {
            Order order =
                    invocation.getArgument(0);

            order.setId(1L);

            return order;
        });

        OrderResponse response =
                orderService.create(request);

        assertEquals(1L, response.id());

        assertEquals(
                new BigDecimal("93500.00"),
                response.totalAmount()
        );

        verify(inventoryClient)
                .reserveOrder(
                        eq(1L),
                        eq(request.items())
                );
    }

    @Test
    void shouldRejectInactiveProduct() {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        5L,
                        List.of(
                                new CreateOrderItemRequest(
                                        1L,
                                        1
                                )
                        )
                );

        when(productClient.getProduct(1L))
                .thenReturn(
                        product(
                                1L,
                                "Inactive Product",
                                "100.00",
                                false
                        )
                );

        assertThrows(
                ConflictException.class,
                () -> orderService.create(request)
        );

        verify(
                orderRepository,
                never()
        ).saveAndFlush(any(Order.class));

        verifyNoInteractions(inventoryClient);
    }

    @Test
    void shouldRejectDuplicateProductIds() {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        5L,
                        List.of(
                                new CreateOrderItemRequest(1L, 1),
                                new CreateOrderItemRequest(1L, 2)
                        )
                );

        assertThrows(
                BadRequestException.class,
                () -> orderService.create(request)
        );

        verifyNoInteractions(productClient);
        verifyNoInteractions(inventoryClient);

        verify(
                orderRepository,
                never()
        ).saveAndFlush(any(Order.class));
    }

    @Test
    void shouldConfirmInventoryBeforeConfirmingOrder() {
        Order order =
                orderWithStatus(
                        OrderStatus.PENDING
                );

        when(orderRepository.findById(1L))
                .thenReturn(
                        Optional.of(order)
                );

        OrderResponse response =
                orderService.confirm(1L);

        verify(inventoryClient)
                .confirmOrder(1L);

        assertEquals(
                OrderStatus.CONFIRMED,
                response.status()
        );
    }

    @Test
    void shouldKeepOrderPendingWhenInventoryConfirmationFails() {
        Order order =
                orderWithStatus(
                        OrderStatus.PENDING
                );

        when(orderRepository.findById(1L))
                .thenReturn(
                        Optional.of(order)
                );

        doThrow(
                new DownstreamServiceUnavailableException(
                        "Inventory Service is unavailable"
                )
        ).when(inventoryClient)
                .confirmOrder(1L);

        assertThrows(
                DownstreamServiceUnavailableException.class,
                () -> orderService.confirm(1L)
        );

        assertEquals(
                OrderStatus.PENDING,
                order.getStatus()
        );
    }

    @Test
    void shouldCompleteConfirmedOrder() {
        Order order =
                orderWithStatus(
                        OrderStatus.CONFIRMED
                );

        when(orderRepository.findById(1L))
                .thenReturn(
                        Optional.of(order)
                );

        OrderResponse response =
                orderService.complete(1L);

        assertEquals(
                OrderStatus.COMPLETED,
                response.status()
        );
    }

    @Test
    void shouldRejectCompletingPendingOrder() {
        Order order =
                orderWithStatus(
                        OrderStatus.PENDING
                );

        when(orderRepository.findById(1L))
                .thenReturn(
                        Optional.of(order)
                );

        assertThrows(
                ConflictException.class,
                () -> orderService.complete(1L)
        );
    }

    @Test
    void shouldCancelPendingOrderAndReleaseInventory() {
        Order order =
                orderWithStatus(
                        OrderStatus.PENDING
                );

        when(orderRepository.findById(1L))
                .thenReturn(
                        Optional.of(order)
                );

        OrderResponse response =
                orderService.cancel(1L);

        verify(inventoryClient)
                .releaseOrder(1L);

        assertEquals(
                OrderStatus.CANCELLED,
                response.status()
        );
    }

    @Test
    void shouldRejectCancellingConfirmedOrder() {
        Order order =
                orderWithStatus(
                        OrderStatus.CONFIRMED
                );

        when(orderRepository.findById(1L))
                .thenReturn(
                        Optional.of(order)
                );

        assertThrows(
                ConflictException.class,
                () -> orderService.cancel(1L)
        );

        verify(
                inventoryClient,
                never()
        ).releaseOrder(anyLong());
    }

    @Test
    void shouldRejectCancellingCompletedOrder() {
        Order order =
                orderWithStatus(
                        OrderStatus.COMPLETED
                );

        when(orderRepository.findById(1L))
                .thenReturn(
                        Optional.of(order)
                );

        assertThrows(
                ConflictException.class,
                () -> orderService.cancel(1L)
        );

        verify(
                inventoryClient,
                never()
        ).releaseOrder(anyLong());
    }

    @Test
    void shouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findById(999L))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getById(999L)
        );
    }

    private ProductSnapshotResponse product(
            Long id,
            String name,
            String price,
            boolean active
    ) {
        return new ProductSnapshotResponse(
                id,
                name,
                "description",
                new BigDecimal(price),
                "SKU-" + id,
                active,
                1L,
                "Category",
                null,
                null
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
