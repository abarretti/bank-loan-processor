-- V1__init_trade_finance_loan.sql
-- Flyway migration for Postgres 16

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- Enums
-- =========================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'party_role') THEN
CREATE TYPE party_role AS ENUM ('DEBTOR', 'CREDITOR');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'loan_state') THEN
CREATE TYPE loan_state AS ENUM ('CREATED', 'DISBURSED', 'REPAID', 'CLOSED');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'collateral_status') THEN
CREATE TYPE collateral_status AS ENUM ('NONE', 'RESERVED', 'PLEDGED', 'RELEASED');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'loan_event_type') THEN
CREATE TYPE loan_event_type AS ENUM (
      'CREATE',
      'DISBURSE',
      'REPAY',
      'RESERVE',
      'PLEDGE',
      'RELEASE',
      'CLOSE'
    );
END IF;
END$$;

-- =========================
-- party
-- =========================
CREATE TABLE IF NOT EXISTS party (
    party_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role            party_role NOT NULL,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_party_role ON party(role);

-- =========================
-- loan (snapshot/current state)
-- =========================
CREATE TABLE IF NOT EXISTS loan (
    loan_id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debtor_party_id     UUID NOT NULL REFERENCES party(party_id),
    creditor_party_id   UUID NOT NULL REFERENCES party(party_id),

    state               loan_state NOT NULL DEFAULT 'CREATED',

    -- Financials (snapshot)
    principal_amount    NUMERIC(19,4) NOT NULL CHECK (principal_amount >= 0),
    amount_disbursed    NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (amount_disbursed >= 0),
    amount_paid         NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (amount_paid >= 0),

    -- Useful for optimistic concurrency / event-sourcing sequencing
    version             BIGINT NOT NULL DEFAULT 0,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Optional sanity constraints
    CONSTRAINT chk_disbursed_le_principal CHECK (amount_disbursed <= principal_amount),
    CONSTRAINT chk_paid_le_disbursed CHECK (amount_paid <= amount_disbursed)
    );

CREATE INDEX IF NOT EXISTS idx_loan_debtor ON loan(debtor_party_id);
CREATE INDEX IF NOT EXISTS idx_loan_creditor ON loan(creditor_party_id);
CREATE INDEX IF NOT EXISTS idx_loan_state ON loan(state);

-- =========================
-- collateral
-- =========================
CREATE TABLE IF NOT EXISTS collateral (
    collateral_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id          UUID NOT NULL REFERENCES loan(loan_id) ON DELETE CASCADE,

    status           collateral_status NOT NULL DEFAULT 'NONE',
    metadata         JSONB NOT NULL DEFAULT '{}'::jsonb,

    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_collateral_loan ON collateral(loan_id);
CREATE INDEX IF NOT EXISTS idx_collateral_status ON collateral(status);

-- =========================
-- loan_event (event store)
-- =========================
CREATE TABLE IF NOT EXISTS loan_event (
    event_id          UUID PRIMARY KEY, -- unique index on eventId (your choice)
    loan_id           UUID NOT NULL REFERENCES loan(loan_id) ON DELETE CASCADE,

    -- who initiated (optional but helpful)
    party_id          UUID NULL REFERENCES party(party_id),

    type              loan_event_type NOT NULL,

    -- Optional fields based on event type
    amount            NUMERIC(19,4) NULL CHECK (amount IS NULL OR amount >= 0),
    collateral_id     UUID NULL REFERENCES collateral(collateral_id),

    -- Client-provided retry key (optional; you can also enforce uniqueness on it)
    idempotency_key   TEXT NULL,

    occurred_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Optional: enforce that collateral_id (if set) belongs to the same loan
    -- This is hard to do with pure FK; would require a trigger. Keeping it simple.
    CONSTRAINT chk_collateral_requires_type CHECK (
        collateral_id IS NULL OR type IN ('RESERVE', 'PLEDGE', 'RELEASE')
    ),
    CONSTRAINT chk_amount_requires_type CHECK (
        amount IS NULL OR type IN ('DISBURSE', 'REPAY')
    )
    );

CREATE INDEX IF NOT EXISTS idx_loan_event_loan_occurred ON loan_event(loan_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_loan_event_loan_type ON loan_event(loan_id, type);

-- =========================
-- updated_at trigger helper
-- =========================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_party_updated_at') THEN
CREATE TRIGGER trg_party_updated_at
    BEFORE UPDATE ON party
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_loan_updated_at') THEN
CREATE TRIGGER trg_loan_updated_at
    BEFORE UPDATE ON loan
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_collateral_updated_at') THEN
CREATE TRIGGER trg_collateral_updated_at
    BEFORE UPDATE ON collateral
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
END IF;
END$$;
