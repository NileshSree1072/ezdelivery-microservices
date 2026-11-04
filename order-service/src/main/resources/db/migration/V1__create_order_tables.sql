CREATE TABLE orders (
    id              UUID PRIMARY KEY,
    customer_email  VARCHAR(255)   NOT NULL,
    customer_name   VARCHAR(120)   NOT NULL,
    street_address  VARCHAR(255),
    city            VARCHAR(100),
    postal_code     VARCHAR(20),
    country         VARCHAR(100),
    status          VARCHAR(30)    NOT NULL,
    total_amount    NUMERIC(12, 2) NOT NULL,
    failure_reason  VARCHAR(255),
    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version         BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_orders_customer_email ON orders (customer_email, created_at DESC);

CREATE TABLE order_lines (
    id             BIGSERIAL PRIMARY KEY,
    order_id       UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id     VARCHAR(64)    NOT NULL,
    product_title  VARCHAR(200)   NOT NULL,
    unit_price     NUMERIC(12, 2) NOT NULL,
    quantity       INTEGER        NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_order_lines_order ON order_lines (order_id);
