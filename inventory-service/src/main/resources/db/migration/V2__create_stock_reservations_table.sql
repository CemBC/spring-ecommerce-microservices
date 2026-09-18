CREATE TABLE stock_reservations (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT chk_stock_reservation_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT uq_stock_reservation_order_product
        UNIQUE (order_id, product_id),

    CONSTRAINT fk_stock_reservation_inventory_product
        FOREIGN KEY (product_id)
        REFERENCES inventory(product_id)
);

CREATE INDEX idx_stock_reservations_order_id
    ON stock_reservations(order_id);

CREATE INDEX idx_stock_reservations_product_id
    ON stock_reservations(product_id);

CREATE INDEX idx_stock_reservations_status
    ON stock_reservations(status);
