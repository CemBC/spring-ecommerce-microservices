CREATE TABLE saga_states (
    order_id BIGINT PRIMARY KEY,
    order_created BOOLEAN NOT NULL DEFAULT FALSE,
    inventory_reserved BOOLEAN NOT NULL DEFAULT FALSE,
    inventory_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    order_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    payment_id BIGINT,
    payment_status VARCHAR(40),
    terminal_status VARCHAR(40) NOT NULL,
    last_event_type VARCHAR(120),
    last_event_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_saga_terminal_updated
    ON saga_states(terminal_status, updated_at);
