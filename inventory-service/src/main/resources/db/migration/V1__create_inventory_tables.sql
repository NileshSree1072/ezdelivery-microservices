CREATE TABLE inventory_items (
    product_id          VARCHAR(64) PRIMARY KEY,
    available_quantity  INTEGER     NOT NULL CHECK (available_quantity >= 0),
    reserved_quantity   INTEGER     NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    updated_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version             BIGINT      NOT NULL DEFAULT 0
);

-- One row per order: the UNIQUE order_id makes reservation idempotent.
CREATE TABLE stock_reservations (
    id          BIGSERIAL PRIMARY KEY,
    order_id    UUID        NOT NULL UNIQUE,
    status      VARCHAR(20) NOT NULL,
    reason      VARCHAR(255),
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE reservation_lines (
    id              BIGSERIAL PRIMARY KEY,
    reservation_id  BIGINT      NOT NULL REFERENCES stock_reservations (id) ON DELETE CASCADE,
    product_id      VARCHAR(64) NOT NULL,
    quantity        INTEGER     NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_reservation_lines_reservation ON reservation_lines (reservation_id);
