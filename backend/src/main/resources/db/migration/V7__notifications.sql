-- Notifications log: each row = one email we tried to send. Idempotency via the unique partial
-- index on (obligation_id, days_before) — the scheduler can run repeatedly without spamming the user.

CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    type            VARCHAR(64) NOT NULL,         -- OBLIGATION_REMINDER, TEST
    obligation_id   UUID REFERENCES tax_obligations(id) ON DELETE SET NULL,
    days_before     INTEGER,                      -- reminder window (e.g. 3 days before deadline)

    recipient       VARCHAR(255) NOT NULL,
    subject         VARCHAR(500) NOT NULL,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    delivered       BOOLEAN     NOT NULL DEFAULT TRUE,
    error_message   TEXT,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Prevent the scheduler from re-sending the same reminder for the same obligation+window.
-- Only successful deliveries count — failed ones can be retried.
CREATE UNIQUE INDEX uq_notifications_obligation_window
    ON notifications (obligation_id, days_before)
    WHERE obligation_id IS NOT NULL AND delivered = TRUE;

CREATE INDEX idx_notifications_user_sent ON notifications (user_id, sent_at DESC);
