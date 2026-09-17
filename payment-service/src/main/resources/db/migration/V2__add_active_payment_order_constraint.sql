CREATE UNIQUE INDEX uq_payments_order_non_retryable
    ON payments(order_id)
    WHERE status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'REFUNDED');
