CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT chk_inventory_quantity_non_negative
        CHECK (quantity >= 0),
    CONSTRAINT chk_inventory_reserved_non_negative
        CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_inventory_reserved_not_above_quantity
        CHECK (reserved_quantity <= quantity)
);

CREATE UNIQUE INDEX idx_inventory_product_id
    ON inventory(product_id);
