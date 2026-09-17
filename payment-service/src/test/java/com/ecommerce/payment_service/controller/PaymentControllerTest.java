package com.ecommerce.payment_service.controller;

import com.ecommerce.payment_service.dto.CreatePaymentRequest;
import com.ecommerce.payment_service.dto.PaymentResponse;
import com.ecommerce.payment_service.entity.PaymentStatus;
import com.ecommerce.payment_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void shouldCreatePayment() throws Exception {
        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        42L,
                        5L,
                        new BigDecimal("93500.00"),
                        "TRY"
                );

        PaymentResponse response =
                new PaymentResponse(
                        1L,
                        42L,
                        5L,
                        new BigDecimal("93500.00"),
                        "TRY",
                        PaymentStatus.PENDING,
                        "MOCK",
                        null,
                        null,
                        0L,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        when(paymentService.create(any(CreatePaymentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(42))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.currency").value("TRY"));
    }

    @Test
    void shouldRejectZeroAmount() throws Exception {
        String body = """
                {
                  "orderId": 42,
                  "userId": 5,
                  "amount": 0,
                  "currency": "TRY"
                }
                """;

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidCurrency() throws Exception {
        String body = """
                {
                  "orderId": 42,
                  "userId": 5,
                  "amount": 100,
                  "currency": "TL"
                }
                """;

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());
    }
}
