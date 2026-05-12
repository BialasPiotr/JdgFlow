-- Tax engine config — rates per year. Edit by inserting a new row, never UPDATE history.
-- All amounts in PLN, percentages stored as decimal fraction (e.g. 0.1952 for 19.52%).

CREATE TABLE tax_rates (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    year                        INT  NOT NULL UNIQUE,

    -- Bases derived from statutory minimums (refreshed each year)
    minimum_wage                NUMERIC(12, 2) NOT NULL,         -- minimalne wynagrodzenie
    average_wage_forecast       NUMERIC(12, 2) NOT NULL,         -- prognozowane przeciętne wynagrodzenie
    preferential_zus_base       NUMERIC(12, 2) NOT NULL,         -- 30% × minimum_wage
    standard_zus_base           NUMERIC(12, 2) NOT NULL,         -- 60% × average_wage_forecast

    -- ZUS contribution rates (paid by entrepreneur, applied to chosen base)
    pension_rate                NUMERIC(6, 4) NOT NULL,          -- emerytalna   19.52%
    disability_rate             NUMERIC(6, 4) NOT NULL,          -- rentowa       8.00%
    sickness_rate               NUMERIC(6, 4) NOT NULL,          -- chorobowa     2.45% (voluntary)
    accident_rate               NUMERIC(6, 4) NOT NULL,          -- wypadkowa     1.67% (default)
    labor_fund_rate             NUMERIC(6, 4) NOT NULL,          -- Fundusz Pracy 2.45%

    -- PIT — scale (skala podatkowa)
    scale_first_threshold       NUMERIC(15, 2) NOT NULL,         -- 120 000 PLN
    scale_lower_rate            NUMERIC(6, 4)  NOT NULL,         -- 12%
    scale_upper_rate            NUMERIC(6, 4)  NOT NULL,         -- 32%
    scale_tax_free_amount       NUMERIC(15, 2) NOT NULL,         -- 30 000 PLN/rok

    -- PIT — liniowy
    linear_rate                 NUMERIC(6, 4) NOT NULL,          -- 19%

    -- PIT — IP BOX
    ip_box_rate                 NUMERIC(6, 4) NOT NULL,          -- 5%

    -- Składka zdrowotna
    health_rate_scale           NUMERIC(6, 4)  NOT NULL,         -- 9% dochodu (skala)
    health_rate_linear          NUMERIC(6, 4)  NOT NULL,         -- 4.9% dochodu (liniowy)
    health_min_base             NUMERIC(12, 2) NOT NULL,         -- minimalna podstawa = minimalne wynagrodzenie
    health_linear_deduction_cap NUMERIC(12, 2) NOT NULL,         -- roczny limit odliczenia w liniowym

    -- Mały ZUS Plus — bracket parameters (revenue from previous year)
    mzp_revenue_threshold       NUMERIC(15, 2) NOT NULL,         -- 200 000 PLN — próg uprawnień
    mzp_factor                  NUMERIC(6, 4)  NOT NULL,         -- 0.5 (połowa średniego miesięcznego dochodu)

    -- Audit
    notes                       TEXT,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tax_rates_year ON tax_rates (year);

-- Seed: 2025 — values verified against kalkulatorB2B / publicly known statutory rates.
INSERT INTO tax_rates (
    year,
    minimum_wage, average_wage_forecast, preferential_zus_base, standard_zus_base,
    pension_rate, disability_rate, sickness_rate, accident_rate, labor_fund_rate,
    scale_first_threshold, scale_lower_rate, scale_upper_rate, scale_tax_free_amount,
    linear_rate, ip_box_rate,
    health_rate_scale, health_rate_linear, health_min_base, health_linear_deduction_cap,
    mzp_revenue_threshold, mzp_factor,
    notes
) VALUES (
    2025,
    4806.00, 8673.00, 1441.80, 5203.80,
    0.1952, 0.0800, 0.0245, 0.0167, 0.0245,
    120000.00, 0.12, 0.32, 30000.00,
    0.19, 0.05,
    0.09, 0.049, 4806.00, 14500.00,
    200000.00, 0.5,
    'Stawki obowiązujące od 01.01.2025. Min. wynagrodzenie 4806 zł. Preferencyjna podstawa = 30% × 4806 = 1441,80 zł.'
);

-- Seed: 2026 — placeholder until budget law confirms. Verify before reliance.
INSERT INTO tax_rates (
    year,
    minimum_wage, average_wage_forecast, preferential_zus_base, standard_zus_base,
    pension_rate, disability_rate, sickness_rate, accident_rate, labor_fund_rate,
    scale_first_threshold, scale_lower_rate, scale_upper_rate, scale_tax_free_amount,
    linear_rate, ip_box_rate,
    health_rate_scale, health_rate_linear, health_min_base, health_linear_deduction_cap,
    mzp_revenue_threshold, mzp_factor,
    notes
) VALUES (
    2026,
    5020.00, 9000.00, 1506.00, 5400.00,
    0.1952, 0.0800, 0.0245, 0.0167, 0.0245,
    120000.00, 0.12, 0.32, 30000.00,
    0.19, 0.05,
    0.09, 0.049, 5020.00, 14500.00,
    200000.00, 0.5,
    'PLACEHOLDER — wartości szacunkowe na 2026. Zweryfikuj po publikacji ustawy budżetowej.'
);

-- Extend users with tax-engine configuration. Replaces the simpler `zus_strategy` flag.
ALTER TABLE users ADD COLUMN zus_mode            VARCHAR(32);
ALTER TABLE users ADD COLUMN vat_payer           BOOLEAN     NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN accounting_method   VARCHAR(16) NOT NULL DEFAULT 'ACCRUAL';
ALTER TABLE users ADD COLUMN voluntary_sickness  BOOLEAN     NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN ip_box_eligible     BOOLEAN     NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN joint_settlement    BOOLEAN     NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN business_start_date DATE;
ALTER TABLE users ADD COLUMN previous_year_revenue NUMERIC(15, 2);   -- dla weryfikacji uprawnień MZP (≤ 200 000)
ALTER TABLE users ADD COLUMN previous_year_income  NUMERIC(15, 2);   -- dla obliczenia podstawy MZP

-- Migrate existing zus_strategy values to the new zus_mode column.
-- Old enum: ULGA_NA_START / MALY_ZUS_PLUS / PELNY_ZUS
-- New enum: ULGA_NA_START / PREFERENTIAL / MALY_ZUS_PLUS / STANDARD
UPDATE users SET zus_mode = CASE zus_strategy
    WHEN 'PELNY_ZUS' THEN 'STANDARD'
    ELSE zus_strategy
END;
ALTER TABLE users ALTER COLUMN zus_mode SET NOT NULL;
ALTER TABLE users DROP COLUMN zus_strategy;
