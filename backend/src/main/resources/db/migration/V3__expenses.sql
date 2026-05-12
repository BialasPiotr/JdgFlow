CREATE TABLE expense_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(128) NOT NULL,
    icon        VARCHAR(32),
    color       VARCHAR(16),
    deductible  BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  SMALLINT     NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO expense_categories (code, name, icon, color, deductible, sort_order) VALUES
('SOFTWARE',  'Software i subskrypcje', 'cloud',          '#3b82f6', TRUE, 10),
('HARDWARE',  'Sprzęt komputerowy',     'monitor',        '#8b5cf6', TRUE, 20),
('OFFICE',    'Biuro',                  'briefcase',      '#10b981', TRUE, 30),
('TELECOM',   'Telefon i internet',     'phone',          '#06b6d4', TRUE, 40),
('FUEL',      'Paliwo',                 'fuel',           '#ef4444', TRUE, 50),
('TRAVEL',    'Podróże służbowe',       'plane',          '#f97316', TRUE, 60),
('TRAINING',  'Szkolenia i kursy',      'book-open',      '#a855f7', TRUE, 70),
('HOSTING',   'Hosting i domeny',       'server',         '#84cc16', TRUE, 80),
('OTHER',     'Inne',                   'more-horizontal','#64748b', TRUE, 999);

CREATE TABLE expenses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id     UUID NOT NULL REFERENCES expense_categories(id),

    amount          NUMERIC(15, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'PLN',
    exchange_rate   NUMERIC(12, 6),

    expense_date    DATE NOT NULL,
    description     VARCHAR(500) NOT NULL,
    vendor          VARCHAR(255),

    vat_deductible  BOOLEAN NOT NULL DEFAULT FALSE,
    vat_amount      NUMERIC(15, 2),

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_expenses_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_expenses_user_date ON expenses (user_id, expense_date DESC);
CREATE INDEX idx_expenses_user_category ON expenses (user_id, category_id);
