package com.ecommerce.payment_service.integration;

import com.ecommerce.payment_service.client.OrderClient;
import com.ecommerce.payment_service.client.dto.OrderSnapshotResponse;
import com.ecommerce.payment_service.entity.Payment;
import com.ecommerce.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PaymentFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("payment_flow_test_db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void properties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private OrderClient orderClient;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();

        when(
                orderClient.getOrder(anyLong())
        ).thenAnswer(invocation -> {
            Long orderId =
                    invocation.getArgument(0);

            return switch (orderId.intValue()) {
                case 42 -> order(
                        42L,
                        5L,
                        "93500.00"
                );

                case 50 -> order(
                        50L,
                        5L,
                        "100.00"
                );

                case 70 -> order(
                        70L,
                        7L,
                        "500.00"
                );

                case 100 -> order(
                        100L,
                        1L,
                        "100.00"
                );

                case 101 -> order(
                        101L,
                        2L,
                        "200.00"
                );

                default -> order(
                        orderId,
                        1L,
                        "100.00"
                );
            };
        });

        doNothing()
                .when(orderClient)
                .confirmOrder(anyLong());
    }

    @Test
    void shouldCreateProcessSucceedAndRefundPayment()
            throws Exception {

        createPayment(
                42L,
                "TRY"
        );

        Payment payment =
                paymentRepository
                        .findAll()
                        .getFirst();

        mockMvc.perform(
                        patch(
                                "/api/payments/{id}/process",
                                payment.getId()
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PROCESSING")
                )
                .andExpect(
                        jsonPath("$.transactionReference")
                                .isNotEmpty()
                );

        mockMvc.perform(
                        patch(
                                "/api/payments/{id}/succeed",
                                payment.getId()
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("SUCCEEDED")
                );

        mockMvc.perform(
                        patch(
                                "/api/payments/{id}/refund",
                                payment.getId()
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("REFUNDED")
                );

        mockMvc.perform(
                        patch(
                                "/api/payments/{id}/refund",
                                payment.getId()
                        )
                )
                .andExpect(
                        status().isConflict()
                );
    }

    @Test
    void shouldCreateProcessAndFailPaymentThenAllowRetry()
            throws Exception {

        createPayment(
                50L,
                "USD"
        );

        Payment first =
                paymentRepository
                        .findAll()
                        .getFirst();

        mockMvc.perform(
                        patch(
                                "/api/payments/{id}/process",
                                first.getId()
                        )
                )
                .andExpect(
                        status().isOk()
                );

        mockMvc.perform(
                        patch(
                                "/api/payments/{id}/fail",
                                first.getId()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "failureReason": "Card declined"
                                        }
                                        """)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("FAILED")
                );

        createPayment(
                50L,
                "USD"
        );

        mockMvc.perform(
                        get("/api/payments")
                                .param(
                                        "orderId",
                                        "50"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(2)
                );
    }

    @Test
    void shouldRejectDuplicatePendingPaymentForSameOrder()
            throws Exception {

        createPayment(
                70L,
                "EUR"
        );

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "orderId": 70,
                                          "currency": "EUR"
                                        }
                                        """)
                )
                .andExpect(
                        status().isConflict()
                );
    }

    @Test
    void shouldFilterPayments()
            throws Exception {

        createPayment(
                100L,
                "TRY"
        );

        createPayment(
                101L,
                "USD"
        );

        mockMvc.perform(
                        get("/api/payments")
                                .param(
                                        "userId",
                                        "2"
                                )
                                .param(
                                        "currency",
                                        "usd"
                                )
                                .param(
                                        "status",
                                        "PENDING"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].orderId")
                                .value(101)
                )
                .andExpect(
                        jsonPath("$.content[0].currency")
                                .value("USD")
                );
    }

    private void createPayment(
            Long orderId,
            String currency
    ) throws Exception {

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "orderId": %d,
                                          "currency": "%s"
                                        }
                                        """.formatted(
                                        orderId,
                                        currency
                                ))
                )
                .andExpect(
                        status().isCreated()
                );
    }

    private OrderSnapshotResponse order(
            Long orderId,
            Long userId,
            String totalAmount
    ) {
        return new OrderSnapshotResponse(
                orderId,
                userId,
                "PENDING",
                new BigDecimal(totalAmount),
                null,
                null,
                null
        );
    }
}
