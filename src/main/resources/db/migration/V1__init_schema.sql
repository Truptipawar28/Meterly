-- Customer hierarchy: account -> service -> endpoint
CREATE TABLE account (
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(200)   NOT NULL,
    plan_included      NUMERIC(19, 6) NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE service (
    id                 BIGSERIAL PRIMARY KEY,
    account_id         BIGINT       NOT NULL REFERENCES account (id),
    name               VARCHAR(200) NOT NULL,
    UNIQUE (account_id, name)
);

CREATE TABLE endpoint (
    id                 BIGSERIAL PRIMARY KEY,
    service_id         BIGINT       NOT NULL REFERENCES service (id),
    name               VARCHAR(200) NOT NULL,
    UNIQUE (service_id, name)
);

-- Price per unit for each metric we bill
CREATE TABLE metric_price (
    metric             VARCHAR(30)    PRIMARY KEY,
    unit_price         NUMERIC(19, 8) NOT NULL
);

INSERT INTO metric_price (metric, unit_price) VALUES
    ('API_CALLS', 0.00020000),
    ('AI_TOKENS', 0.00001000),
    ('STORAGE',   0.00000600),
    ('EGRESS',    0.00009000);

-- One billing period (normally a month) per account; only one OPEN at a time
CREATE TABLE billing_period (
    id                 BIGSERIAL PRIMARY KEY,
    account_id         BIGINT      NOT NULL REFERENCES account (id),
    starts_on          DATE        NOT NULL,
    ends_on            DATE        NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                       CHECK (status IN ('OPEN', 'CLOSED')),
    version            BIGINT      NOT NULL DEFAULT 0,   -- optimistic locking (Step 5)
    UNIQUE (account_id, starts_on)
);

CREATE UNIQUE INDEX one_open_period_per_account
    ON billing_period (account_id) WHERE status = 'OPEN';

-- Raw usage reported by workers; the source of truth for cost
CREATE TABLE usage_event (
    id                 BIGSERIAL PRIMARY KEY,
    endpoint_id        BIGINT       NOT NULL REFERENCES endpoint (id),
    billing_period_id  BIGINT       NOT NULL REFERENCES billing_period (id),
    metric             VARCHAR(30)  NOT NULL REFERENCES metric_price (metric),
    quantity           BIGINT       NOT NULL CHECK (quantity > 0),
    idempotency_key    VARCHAR(100) NOT NULL UNIQUE,     -- retries are counted once (Step 4)
    occurred_at        TIMESTAMPTZ  NOT NULL,
    received_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_usage_event_period ON usage_event (billing_period_id);
