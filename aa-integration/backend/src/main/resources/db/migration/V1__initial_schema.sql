-- Account Aggregator Integration Schema
-- V1: Initial schema creation

-- ============================================================
-- Customers table (minimal PII)
-- ============================================================
CREATE TABLE customers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mobile_hash     VARCHAR(64) NOT NULL UNIQUE,   -- SHA-256 of mobile number
    email_hash      VARCHAR(64),                   -- SHA-256 of email
    aa_handle       VARCHAR(128),                  -- e.g. user@finvu
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_mobile_hash ON customers(mobile_hash);

-- ============================================================
-- Consent requests
-- ============================================================


CREATE TABLE consent_requests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id         UUID NOT NULL REFERENCES customers(id),
    digio_consent_id    VARCHAR(128) UNIQUE,         -- ID returned by Digio
    template_id         VARCHAR(64) NOT NULL,
    status              VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    purpose_code        VARCHAR(32) NOT NULL,
    purpose_text        TEXT,
    fi_types            TEXT[],                      -- e.g. {DEPOSIT, MUTUAL_FUNDS}
    date_range_from     DATE,
    date_range_to       DATE,
    consent_expiry      TIMESTAMPTZ,
    redirect_url        TEXT,
    digio_redirect_url  TEXT,                        -- URL to redirect user for consent
    raw_request         JSONB,                       -- full payload sent to Digio
    raw_response        JSONB,                       -- full response from Digio
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consent_customer ON consent_requests(customer_id);
CREATE INDEX idx_consent_digio_id ON consent_requests(digio_consent_id);
CREATE INDEX idx_consent_status   ON consent_requests(status);

-- ============================================================
-- Data fetch requests
-- ============================================================


CREATE TABLE fetch_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consent_id      UUID NOT NULL REFERENCES consent_requests(id),
    digio_fetch_id  VARCHAR(128) UNIQUE,
    status          VARCHAR(32) NOT NULL DEFAULT 'INITIATED',
    from_date       DATE NOT NULL,
    to_date         DATE NOT NULL,
    fi_types        TEXT[],
    initiated_by    VARCHAR(64),                     -- service or user initiating
    raw_request     JSONB,
    raw_response    JSONB,
    error_message   TEXT,
    retry_count     INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fetch_consent    ON fetch_requests(consent_id);
CREATE INDEX idx_fetch_digio_id   ON fetch_requests(digio_fetch_id);
CREATE INDEX idx_fetch_status     ON fetch_requests(status);

-- ============================================================
-- Normalized financial data - Accounts
-- ============================================================
CREATE TABLE financial_accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fetch_id        UUID NOT NULL REFERENCES fetch_requests(id),
    customer_id     UUID NOT NULL REFERENCES customers(id),
    fip_id          VARCHAR(64),
    account_type    VARCHAR(32),                     -- SAVINGS, CURRENT, DEPOSIT, etc.
    masked_account  VARCHAR(32),                     -- last 4 digits only
    ifsc_code       VARCHAR(12),
    currency        VARCHAR(3) DEFAULT 'INR',
    balance         NUMERIC(18,2),
    balance_date    DATE,
    raw_data        JSONB,                           -- encrypted original payload
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fin_account_fetch    ON financial_accounts(fetch_id);
CREATE INDEX idx_fin_account_customer ON financial_accounts(customer_id);

-- ============================================================
-- Normalized financial data - Transactions
-- ============================================================
CREATE TABLE financial_transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id          UUID NOT NULL REFERENCES financial_accounts(id),
    fetch_id            UUID NOT NULL REFERENCES fetch_requests(id),
    transaction_date    DATE NOT NULL,
    amount              NUMERIC(18,2) NOT NULL,
    tx_type             VARCHAR(16),                 -- CREDIT, DEBIT
    narration           TEXT,
    reference           VARCHAR(128),
    mode                VARCHAR(32),                 -- NEFT, IMPS, UPI, etc.
    category            VARCHAR(64),
    raw_data            JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tx_account  ON financial_transactions(account_id);
CREATE INDEX idx_tx_fetch    ON financial_transactions(fetch_id);
CREATE INDEX idx_tx_date     ON financial_transactions(transaction_date);

-- ============================================================
-- Normalized financial data - Loans
-- ============================================================
CREATE TABLE financial_loans (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fetch_id        UUID NOT NULL REFERENCES fetch_requests(id),
    customer_id     UUID NOT NULL REFERENCES customers(id),
    fip_id          VARCHAR(64),
    loan_type       VARCHAR(32),
    masked_account  VARCHAR(32),
    principal       NUMERIC(18,2),
    outstanding     NUMERIC(18,2),
    emi_amount      NUMERIC(18,2),
    interest_rate   NUMERIC(6,3),
    tenure_months   INT,
    disbursement_date DATE,
    maturity_date   DATE,
    raw_data        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_loan_fetch    ON financial_loans(fetch_id);
CREATE INDEX idx_loan_customer ON financial_loans(customer_id);

-- ============================================================
-- Audit log
-- ============================================================
CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(64) NOT NULL,               -- consent_request, fetch_request, etc.
    entity_id   UUID,
    action      VARCHAR(64) NOT NULL,               -- CREATED, UPDATED, WEBHOOK_RECEIVED, etc.
    actor       VARCHAR(128),                       -- service name or user
    ip_address  VARCHAR(45),
    old_state   JSONB,
    new_state   JSONB,
    metadata    JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity   ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_created  ON audit_logs(created_at);

-- ============================================================
-- Idempotency keys (for safe retries)
-- ============================================================
CREATE TABLE idempotency_keys (
    key         VARCHAR(128) PRIMARY KEY,
    response    JSONB NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '24 hours'
);

-- Auto-cleanup: PostgreSQL doesn't auto-delete but we'll schedule this
CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);

-- ============================================================
-- Update triggers for updated_at
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_customers_updated_at
    BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_consent_updated_at
    BEFORE UPDATE ON consent_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_fetch_updated_at
    BEFORE UPDATE ON fetch_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
