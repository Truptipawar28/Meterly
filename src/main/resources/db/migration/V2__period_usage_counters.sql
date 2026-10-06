-- Running totals per (period, endpoint, metric), updated with every usage event.
-- Many workers update the same row at the same time -> protected by optimistic locking (version).
-- Events stay the source of truth; these counters are a fast summary that can be reconciled against them.
CREATE TABLE period_usage (
    id                 BIGSERIAL PRIMARY KEY,
    billing_period_id  BIGINT      NOT NULL REFERENCES billing_period (id),
    endpoint_id        BIGINT      NOT NULL REFERENCES endpoint (id),
    metric             VARCHAR(30) NOT NULL REFERENCES metric_price (metric),
    quantity           BIGINT      NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    event_count        BIGINT      NOT NULL DEFAULT 0 CHECK (event_count >= 0),
    version            BIGINT      NOT NULL DEFAULT 0,
    UNIQUE (billing_period_id, endpoint_id, metric)
);
