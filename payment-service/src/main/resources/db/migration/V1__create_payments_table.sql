-- UNIQUE order_id: an order can be charged at most once, even if the event is redelivered.
CREATE TABLE payments (
    id                  UUID PRIMARY KEY,
    order_id            UUID           NOT NULL UNIQUE,
    amount              NUMERIC(12, 2) NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    provider            VARCHAR(20)    NOT NULL,
    provider_reference  VARCHAR(255),
    failure_reason      VARCHAR(255),
    created_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
