package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.client.OrderClient;
import com.ecommerce.payment_service.client.dto.OrderSnapshotResponse;
import com.ecommerce.payment_service.dto.CreatePaymentRequest;
import com.ecommerce.payment_service.dto.FailPaymentRequest;
import com.ecommerce.payment_service.dto.PaymentResponse;
import com.ecommerce.payment_service.entity.Payment;
import com.ecommerce.payment_service.entity.PaymentStatus;
import com.ecommerce.payment_service.exception.ConflictException;
import com.ecommerce.payment_service.mapper.PaymentMapper;
import com.ecommerce.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private OrderClient orderClient;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository =
                mock(PaymentRepository.class);

        orderClient =
                mock(OrderClient.class);

        paymentService =
                new PaymentService(
                        paymentRepository,
                        new PaymentMapper(),
                        orderClient
                );
    }

    @Test
    void shouldCreatePendingPaymentUsingAuthoritativeOrderData() {
        when(
                paymentRepository
                        .existsByOrderIdAndStatusIn(
                                eq(42L),
                                anyCollection()
                        )
        ).thenReturn(false);

        when(
                orderClient.getOrder(42L)
        ).thenReturn(
                order(
                        42L,
                        5L,
                        "PENDING",
                        "93500.00"
                )
        );

        when(
                paymentRepository.save(
                        any(Payment.class)
                )
        ).thenAnswer(invocation -> {
            Payment payment =
                    invocation.getArgument(0);

            payment.setId(1L);
            payment.setVersion(0L);

            return payment;
        });

        PaymentResponse response =
                paymentService.create(
                        new CreatePaymentRequest(
                                42L,
                                "try"
                        )
                );

        assertEquals(
                1L,
                response.id()
        );

        assertEquals(
                5L,
                response.userId()
        );

        assertEquals(
                new BigDecimal("93500.00"),
                response.amount()
        );

        assertEquals(
                "TRY",
                response.currency()
        );

        assertEquals(
                PaymentStatus.PENDING,
                response.status()
        );

        assertEquals(
                "MOCK",
                response.provider()
        );

        verify(
                orderClient
        ).getOrder(42L);
    }

    @Test
    void shouldRejectPaymentForNonPendingOrder() {
        when(
                paymentRepository
                        .existsByOrderIdAndStatusIn(
                                eq(42L),
                                anyCollection()
                        )
        ).thenReturn(false);

        when(
                orderClient.getOrder(42L)
        ).thenReturn(
                order(
                        42L,
                        5L,
                        "CONFIRMED",
                        "93500.00"
                )
        );

        assertThrows(
                ConflictException.class,
                () -> paymentService.create(
                        new CreatePaymentRequest(
                                42L,
                                "TRY"
                        )
                )
        );

        verify(
                paymentRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldRejectSecondNonRetryablePaymentForSameOrder() {
        when(
                paymentRepository
                        .existsByOrderIdAndStatusIn(
                                eq(42L),
                                anyCollection()
                        )
        ).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> paymentService.create(
                        new CreatePaymentRequest(
                                42L,
                                "TRY"
                        )
                )
        );

        verifyNoInteractions(
                orderClient
        );

        verify(
                paymentRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldProcessPendingPaymentAndCreateTransactionReference() {
        Payment payment =
                payment(PaymentStatus.PENDING);

        when(
                paymentRepository.findById(1L)
        ).thenReturn(
                Optional.of(payment)
        );

        when(
                paymentRepository
                        .existsByTransactionReference(
                                anyString()
                        )
        ).thenReturn(false);

        PaymentResponse response =
                paymentService.process(1L);

        assertEquals(
                PaymentStatus.PROCESSING,
                response.status()
        );

        assertNotNull(
                response.transactionReference()
        );

        assertTrue(
                response.transactionReference()
                        .startsWith("PAY-")
        );
    }

    @Test
    void shouldConfirmOrderWhenPaymentSucceeds() {
        Payment payment =
                payment(
                        PaymentStatus.PROCESSING
                );

        when(
                paymentRepository.findById(1L)
        ).thenReturn(
                Optional.of(payment)
        );

        doNothing()
                .when(orderClient)
                .confirmOrder(42L);

        PaymentResponse response =
                paymentService.succeed(1L);

        assertEquals(
                PaymentStatus.SUCCEEDED,
                response.status()
        );

        verify(
                orderClient
        ).confirmOrder(42L);
    }

    @Test
    void shouldFailProcessingPayment() {
        Payment payment =
                payment(
                        PaymentStatus.PROCESSING
                );

        when(
                paymentRepository.findById(1L)
        ).thenReturn(
                Optional.of(payment)
        );

        PaymentResponse response =
                paymentService.fail(
                        1L,
                        new FailPaymentRequest(
                                "Card declined"
                        )
                );

        assertEquals(
                PaymentStatus.FAILED,
                response.status()
        );

        assertEquals(
                "Card declined",
                response.failureReason()
        );
    }

    @Test
    void shouldCancelPendingPayment() {
        Payment payment =
                payment(PaymentStatus.PENDING);

        when(
                paymentRepository.findById(1L)
        ).thenReturn(
                Optional.of(payment)
        );

        assertEquals(
                PaymentStatus.CANCELLED,
                paymentService.cancel(1L)
                        .status()
        );
    }

    @Test
    void shouldRefundSucceededPayment() {
        Payment payment =
                payment(
                        PaymentStatus.SUCCEEDED
                );

        when(
                paymentRepository.findById(1L)
        ).thenReturn(
                Optional.of(payment)
        );

        assertEquals(
                PaymentStatus.REFUNDED,
                paymentService.refund(1L)
                        .status()
        );
    }

    @Test
    void shouldRejectInvalidLifecycleTransition() {
        Payment payment =
                payment(PaymentStatus.FAILED);

        when(
                paymentRepository.findById(1L)
        ).thenReturn(
                Optional.of(payment)
        );

        assertThrows(
                ConflictException.class,
                () -> paymentService.succeed(1L)
        );

        verify(
                orderClient,
                never()
        ).confirmOrder(anyLong());
    }

    private Payment payment(
            PaymentStatus status
    ) {
        return Payment.builder()
                .id(1L)
                .orderId(42L)
                .userId(5L)
                .amount(
                        new BigDecimal("100.00")
                )
                .currency("TRY")
                .status(status)
                .provider("MOCK")
                .version(0L)
                .build();
    }

    private OrderSnapshotResponse order(
            Long orderId,
            Long userId,
            String status,
            String totalAmount
    ) {
        return new OrderSnapshotResponse(
                orderId,
                userId,
                status,
                new BigDecimal(totalAmount),
                null,
                null,
                null
        );
    }
}
