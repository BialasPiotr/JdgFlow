-- Receipts (paragony / faktury VAT) — uploaded images parsed by Claude Vision API.
-- A receipt may exist without an expense (just-uploaded, awaiting user confirmation) or be
-- linked 1:1 to an expense. The link is bidirectional: expenses.receipt_id is the canonical FK,
-- receipts.expense_id is a denormalized convenience for "find the expense from a receipt" queries.

CREATE TABLE receipts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    s3_key              VARCHAR(512) NOT NULL UNIQUE,
    original_filename   VARCHAR(255),
    mime_type           VARCHAR(64)  NOT NULL,
    file_size_bytes     BIGINT       NOT NULL,

    status              VARCHAR(32)  NOT NULL DEFAULT 'PENDING',  -- PENDING / PROCESSED / FAILED
    ocr_result          JSONB,                                    -- parsed fields from Claude
    error_message       TEXT,

    expense_id          UUID REFERENCES expenses(id) ON DELETE SET NULL,
    processed_at        TIMESTAMPTZ,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_receipts_size CHECK (file_size_bytes > 0)
);

CREATE INDEX idx_receipts_user_created ON receipts (user_id, created_at DESC);
CREATE INDEX idx_receipts_status ON receipts (status) WHERE status <> 'PROCESSED';
CREATE INDEX idx_receipts_expense ON receipts (expense_id) WHERE expense_id IS NOT NULL;

-- Canonical FK on expenses → receipts. Nullable; an expense may be entered manually without a paragon.
ALTER TABLE expenses ADD COLUMN receipt_id UUID REFERENCES receipts(id) ON DELETE SET NULL;
CREATE INDEX idx_expenses_receipt ON expenses (receipt_id) WHERE receipt_id IS NOT NULL;
