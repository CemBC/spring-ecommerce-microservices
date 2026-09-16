-- V2__create_products_table.sql

CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(150) NOT NULL,
                          description VARCHAR(1000),
                          price NUMERIC(12, 2) NOT NULL,
                          sku VARCHAR(100) NOT NULL UNIQUE,
                          active BOOLEAN NOT NULL DEFAULT TRUE,
                          category_id BIGINT NOT NULL,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL,

                          CONSTRAINT fk_products_category
                              FOREIGN KEY (category_id)
                                  REFERENCES categories(id)
);

CREATE INDEX idx_products_name
    ON products(name);

CREATE INDEX idx_products_sku
    ON products(sku);