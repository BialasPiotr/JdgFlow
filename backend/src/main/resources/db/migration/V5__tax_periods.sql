-- Persisted snapshot of tax computations per (user, year, month).
-- Acts as a memo/cache: once an obligation is "paid" it must not change retroactively
-- when revenue/cost numbers shift. Recompute is allowed only for periods with no paid obligations.

CREATE TABLE tax_periods (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    year                INT  NOT NULL,
    month               INT  NOT NULL,

    revenue             NUMERIC(15, 2) NOT NULL,
    costs               NUMERIC(15, 2) NOT NULL,
    income              NUMERIC(15, 2) NOT NULL,            -- revenue - costs (dochód)

    zus_social_total    NUMERIC(15, 2) NOT NULL,            -- suma składek społecznych (do odliczenia od podstawy PIT)
    health_amount       NUMERIC(15, 2) NOT NULL,
    pit_advance         NUMERIC(15, 2) NOT NULL,
    vat_due             NUMERIC(15, 2) NOT NULL DEFAULT 0,

    -- Full breakdown (each calculator step) for UI display "Przychód − ZUS = … × stawka = …"
    breakdown           JSONB NOT NULL,

    computed_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_tax_period_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT uk_tax_periods_user_year_month UNIQUE (user_id, year, month)
);

CREATE INDEX idx_tax_periods_user_year ON tax_periods (user_id, year DESC, month DESC);

CREATE TABLE tax_obligations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    period_id   UUID NOT NULL REFERENCES tax_periods(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    type        VARCHAR(32)    NOT NULL,                    -- ZUS_SOCIAL, ZUS_HEALTH, PIT_ADVANCE, VAT
    amount      NUMERIC(15, 2) NOT NULL,
    deadline    DATE           NOT NULL,
    paid_date   DATE,
    paid_amount NUMERIC(15, 2),

    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_obligation_amount CHECK (amount >= 0)
);

CREATE INDEX idx_tax_obligations_user_deadline ON tax_obligations (user_id, deadline) WHERE paid_date IS NULL;
CREATE INDEX idx_tax_obligations_period ON tax_obligations (period_id);
