CREATE TABLE orders (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    public_id  VARCHAR(36)  NOT NULL,
    sku        VARCHAR(64)  NOT NULL,
    quantity   INT          NOT NULL,
    amount     DECIMAL(12,2) NOT NULL,
    currency   VARCHAR(3)   NOT NULL,
    status     VARCHAR(32)  NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_orders_public_id UNIQUE (public_id)
);

CREATE INDEX idx_orders_status ON orders (status);
