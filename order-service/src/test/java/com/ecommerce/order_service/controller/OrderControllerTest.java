package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.entity.OrderStatus;
import com.ecommerce.order_service.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldCreateOrder() throws Exception {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        5L,
                        List.of(
                                new CreateOrderItemRequest(
                                        1L,
                                        2
                                )
                        )
                );

        OrderResponse response =
                new OrderResponse(
                        1L,
                        5L,
                        OrderStatus.PENDING,
                        new BigDecimal("90000.00"),
                        List.of(
                                new OrderItemResponse(
                                        10L,
                                        1L,
                                        "MacBook Air M3",
                                        2,
                                        new BigDecimal("45000.00"),
                                        new BigDecimal("90000.00")
                                )
                        ),
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        when(
                orderService.create(
                        any(CreateOrderRequest.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request
                                                )
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING")
                )
                .andExpect(
                        jsonPath("$.totalAmount")
                                .value(90000.00)
                );
    }

    @Test
    void shouldRejectEmptyItems() throws Exception {
        String body = """
                {
                  "userId": 5,
                  "items": []
                }
                """;

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(body)
                )
                .andExpect(
                        status().isBadRequest()
                );
    }

    @Test
    void shouldRejectInvalidQuantity() throws Exception {
        String body = """
                {
                  "userId": 5,
                  "items": [
                    {
                      "productId": 1,
                      "quantity": 0
                    }
                  ]
                }
                """;

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(body)
                )
                .andExpect(
                        status().isBadRequest()
                );
    }
}
