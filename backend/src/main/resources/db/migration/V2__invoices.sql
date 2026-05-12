CREATE TABLE invoices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    fakturownia_id  BIGINT NOT NULL,
    invoice_number  VARCHAR(64) NOT NULL,
    kind            VARCHAR(32) NOT NULL,
    status          VARCHAR(32) NOT NULL,

    net_amount      NUMERIC(15, 2) NOT NULL,
    vat_amount      NUMERIC(15, 2) NOT NULL,
    gross_amount    NUMERIC(15, 2) NOT NULL,
    currency        VARCHAR(3)     NOT NULL,
    exchange_rate   NUMERIC(12, 6),

    issue_date      DATE NOT NULL,
    sale_date       DATE NOT NULL,
    payment_date    DATE,
    paid_date       DATE,

    buyer_name      VARCHAR(255),
    buyer_nip       VARCHAR(20),
    buyer_email     VARCHAR(255),

    pdf_s3_key      VARCHAR(512),

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_invoices_user_fakturownia UNIQUE (user_id, fakturownia_id)
);

CREATE INDEX idx_invoices_user_payment ON invoices (user_id, payment_date);
CREATE INDEX idx_invoices_user_issue ON invoices (user_id, issue_date DESC);
CREATE INDEX idx_invoices_user_status ON invoices (user_id, status);
