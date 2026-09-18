package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.client.OrderClient;
import com.ecommerce.payment_service.client.dto.OrderSnapshotResponse;
import com.ecommerce.payment_service.dto.CreatePaymentRequest;
import com.ecommerce.payment_service.dto.FailPaymentRequest;
import com.ecommerce.payment_service.dto.PaymentResponse;
import com.ecommerce.payment_service.entity.Payment;
import com.ecommerce.payment_service.entity.PaymentStatus;
import com.ecommerce.payment_service.exception.BadRequestException;
import com.ecommerce.payment_service.exception.ConflictException;
import com.ecommerce.payment_service.exception.DownstreamServiceUnavailableException;
import com.ecommerce.payment_service.exception.ResourceNotFoundException;
import com.ecommerce.payment_service.mapper.PaymentMapper;
import com.ecommerce.payment_service.outbox.OutboxService;
import com.ecommerce.payment_service.repository.PaymentRepository;
import com.ecommerce.payment_service.specification.PaymentSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PaymentService {

    private static final String MOCK_PROVIDER = "MOCK";
    private static final String PAYABLE_ORDER_STATUS = "PENDING";

    private static final Set<PaymentStatus>
    NON_RETRYABLE_STATUSES =
            EnumSet.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.PROCESSING,
                    PaymentStatus.SUCCEEDED,
                    PaymentStatus.REFUNDED
            );

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderClient orderClient;

    private OutboxService outboxService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentMapper paymentMapper,
            OrderClient orderClient
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.orderClient = orderClient;
    }

    @Autowired(required = false)
    void setOutboxService(
            OutboxService outboxService
    ) {
        this.outboxService = outboxService;
    }

    @Transactional
    public PaymentResponse create(
            CreatePaymentRequest request
    ) {
        if (paymentRepository
                .existsByOrderIdAndStatusIn(
                        request.orderId(),
                        NON_RETRYABLE_STATUSES
                )) {
            throw new ConflictException(
                    "A non-retryable payment already exists for order id: "
                            + request.orderId()
            );
        }

        OrderSnapshotResponse order =
                orderClient.getOrder(
                        request.orderId()
                );

        if (!PAYABLE_ORDER_STATUS.equals(
                order.status()
        )) {
            throw new ConflictException(
                    "Only PENDING orders can be paid"
            );
        }

        if (order.userId() == null
                || order.totalAmount() == null
                || order.totalAmount()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw new DownstreamServiceUnavailableException(
                    "Order Service returned invalid order data for id: "
                            + request.orderId()
            );
        }

        Payment payment = Payment.builder()
                .orderId(order.id())
                .userId(order.userId())
                .amount(
                        money(order.totalAmount())
                )
                .currency(
                        request.currency()
                                .trim()
                                .toUpperCase()
                )
                .status(PaymentStatus.PENDING)
                .provider(MOCK_PROVIDER)
                .build();

        Payment saved =
                paymentRepository.save(payment);

        emit(saved, "PAYMENT_CREATED");

        return paymentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return paymentMapper.toResponse(
                findPayment(id)
        );
    }

    @Transactional(readOnly = true)
    public PaymentResponse getLatestByOrderId(
            Long orderId
    ) {
        Payment payment =
                paymentRepository
                        .findTopByOrderIdOrderByCreatedAtDesc(
                                orderId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found for order id: "
                                                + orderId
                                )
                        );

        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAll(
            Long orderId,
            Long userId,
            PaymentStatus status,
            String currency,
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

        Specification<Payment> specification =
                PaymentSpecification
                        .hasOrderId(orderId)
                        .and(
                                PaymentSpecification
                                        .hasUserId(userId)
                        )
                        .and(
                                PaymentSpecification
                                        .hasStatus(status)
                        )
                        .and(
                                PaymentSpecification
                                        .hasCurrency(currency)
                        )
                        .and(
                                PaymentSpecification
                                        .createdFrom(createdFrom)
                        )
                        .and(
                                PaymentSpecification
                                        .createdTo(createdTo)
                        );

        return paymentRepository
                .findAll(
                        specification,
                        pageable
                )
                .map(
                        paymentMapper::toResponse
                );
    }

    @Transactional
    public PaymentResponse process(Long id) {
        Payment payment = findPayment(id);

        requireStatus(
                payment,
                PaymentStatus.PENDING,
                "Only PENDING payments can be processed"
        );

        payment.setStatus(
                PaymentStatus.PROCESSING
        );

        payment.setFailureReason(null);

        if (payment.getTransactionReference()
                == null) {
            payment.setTransactionReference(
                    generateTransactionReference()
            );
        }

        emit(payment, "PAYMENT_PROCESSING");

        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse succeed(Long id) {
        Payment payment = findPayment(id);

        requireStatus(
                payment,
                PaymentStatus.PROCESSING,
                "Only PROCESSING payments can succeed"
        );

        orderClient.confirmOrder(
                payment.getOrderId()
        );

        payment.setStatus(
                PaymentStatus.SUCCEEDED
        );

        payment.setFailureReason(null);

        emit(payment, "PAYMENT_SUCCEEDED");

        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse fail(
            Long id,
            FailPaymentRequest request
    ) {
        Payment payment = findPayment(id);

        requireStatus(
                payment,
                PaymentStatus.PROCESSING,
                "Only PROCESSING payments can fail"
        );

        payment.setStatus(
                PaymentStatus.FAILED
        );

        payment.setFailureReason(
                request.failureReason().trim()
        );

        emit(payment, "PAYMENT_FAILED");

        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse cancel(Long id) {
        Payment payment = findPayment(id);

        requireStatus(
                payment,
                PaymentStatus.PENDING,
                "Only PENDING payments can be cancelled"
        );

        payment.setStatus(
                PaymentStatus.CANCELLED
        );

        emit(payment, "PAYMENT_CANCELLED");

        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse refund(Long id) {
        Payment payment = findPayment(id);

        requireStatus(
                payment,
                PaymentStatus.SUCCEEDED,
                "Only SUCCEEDED payments can be refunded"
        );

        payment.setStatus(
                PaymentStatus.REFUNDED
        );

        emit(payment, "PAYMENT_REFUNDED");

        return paymentMapper.toResponse(payment);
    }

    private void emit(
            Payment payment,
            String eventType
    ) {
        if (outboxService == null) {
            return;
        }

        Map<String, Object> payload =
                new LinkedHashMap<>();

        payload.put("paymentId", payment.getId());
        payload.put("orderId", payment.getOrderId());
        payload.put("userId", payment.getUserId());
        payload.put(
                "status",
                payment.getStatus().name()
        );
        payload.put("amount", payment.getAmount());
        payload.put("currency", payment.getCurrency());

        outboxService.enqueue(
                "payment.events",
                "Payment",
                payment.getId(),
                eventType,
                payload
        );
    }

    private Payment findPayment(Long id) {
        return paymentRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found with id: "
                                        + id
                        )
                );
    }

    private void requireStatus(
            Payment payment,
            PaymentStatus expected,
            String message
    ) {
        if (payment.getStatus()
                != expected) {
            throw new ConflictException(message);
        }
    }

    private String generateTransactionReference() {
        String reference;

        do {
            reference =
                    "PAY-" + UUID.randomUUID();

        } while (
                paymentRepository
                        .existsByTransactionReference(
                                reference
                        )
        );

        return reference;
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
