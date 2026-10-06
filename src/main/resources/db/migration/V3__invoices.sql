-- An invoice is a frozen snapshot of a closed billing period: names, prices and amounts are copied in,
-- so later price changes or renamed endpoints never change an issued invoice.
CREATE TABLE invoice (
    id                 BIGSERIAL PRIMARY KEY,
    account_id         BIGINT         NOT NULL REFERENCES account (id),
    billing_period_id  BIGINT         NOT NULL UNIQUE REFERENCES billing_period (id),  -- one invoice per period
    usage_total        NUMERIC(19, 8) NOT NULL,
    plan_included      NUMERIC(19, 6) NOT NULL,
    amount_due         NUMERIC(19, 2) NOT NULL,
    issued_at          TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE invoice_line (
    id                 BIGSERIAL PRIMARY KEY,
    invoice_id         BIGINT         NOT NULL REFERENCES invoice (id),
    service_name       VARCHAR(200)   NOT NULL,
    endpoint_name      VARCHAR(200)   NOT NULL,
    metric             VARCHAR(30)    NOT NULL,
    quantity           BIGINT         NOT NULL,
    unit_price         NUMERIC(19, 8) NOT NULL,
    amount             NUMERIC(19, 8) NOT NULL
);

CREATE INDEX idx_invoice_line_invoice ON invoice_line (invoice_id);
CREATE INDEX idx_invoice_account ON invoice (account_id);

-- Immutability enforced by the DATABASE, not just by Java: any UPDATE or DELETE is rejected,
-- even from a SQL console or a buggy future version of the app.
CREATE FUNCTION forbid_invoice_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION '% is immutable: % is not allowed', TG_TABLE_NAME, TG_OP;
END;
$$;

CREATE TRIGGER invoice_immutable
    BEFORE UPDATE OR DELETE ON invoice
    FOR EACH ROW EXECUTE FUNCTION forbid_invoice_change();

CREATE TRIGGER invoice_line_immutable
    BEFORE UPDATE OR DELETE ON invoice_line
    FOR EACH ROW EXECUTE FUNCTION forbid_invoice_change();
