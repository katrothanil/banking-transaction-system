-- =====================================================================
-- V1: Core banking tables
-- Money is stored as NUMERIC(15,2) (never FLOAT) to avoid rounding errors.
-- =====================================================================

CREATE TABLE customers (
    id          BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    phone       VARCHAR(15)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Account numbers come from a sequence so they are unique and readable.
CREATE SEQUENCE account_number_seq START WITH 100000000001;

CREATE TABLE accounts (
    id              BIGSERIAL PRIMARY KEY,
    account_number  VARCHAR(12)   NOT NULL UNIQUE,
    customer_id     BIGINT        NOT NULL REFERENCES customers(id),
    account_type    VARCHAR(10)   NOT NULL CHECK (account_type IN ('SAVINGS', 'CURRENT')),
    balance         NUMERIC(15,2) NOT NULL DEFAULT 0,
    status          VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE'
                                  CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),

    -- Last line of defence: the database itself refuses a negative balance,
    -- even if a bug in the application tries to overdraw an account.
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_accounts_customer ON accounts(customer_id);

-- One row per business operation (deposit, withdrawal or transfer).
-- reference_no is UNIQUE, which makes every operation idempotent:
-- if a client retries the same request, the second insert is rejected
-- and the money moves only once.
CREATE TABLE transactions (
    id               BIGSERIAL PRIMARY KEY,
    reference_no     VARCHAR(64)   NOT NULL UNIQUE,
    transaction_type VARCHAR(10)   NOT NULL CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    amount           NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    from_account_id  BIGINT        REFERENCES accounts(id),
    to_account_id    BIGINT        REFERENCES accounts(id),
    description      VARCHAR(255),
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),

    -- Each type must have exactly the accounts it needs.
    CONSTRAINT chk_transaction_accounts CHECK (
        (transaction_type = 'DEPOSIT'    AND from_account_id IS NULL     AND to_account_id IS NOT NULL) OR
        (transaction_type = 'WITHDRAWAL' AND from_account_id IS NOT NULL AND to_account_id IS NULL) OR
        (transaction_type = 'TRANSFER'   AND from_account_id IS NOT NULL AND to_account_id IS NOT NULL
                                         AND from_account_id <> to_account_id)
    )
);

CREATE INDEX idx_transactions_created ON transactions(created_at);

-- Double-entry ledger: every money movement is written as DEBIT/CREDIT rows.
-- A transfer creates one DEBIT (sender) and one CREDIT (receiver) of equal amount.
-- balance_after keeps a running balance, which makes statements cheap to build.
CREATE TABLE ledger_entries (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT        NOT NULL REFERENCES transactions(id),
    account_id      BIGINT        NOT NULL REFERENCES accounts(id),
    entry_type      VARCHAR(6)    NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount          NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    balance_after   NUMERIC(15,2) NOT NULL CHECK (balance_after >= 0),
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Statements filter by account and date range, so index exactly that.
CREATE INDEX idx_ledger_account_created ON ledger_entries(account_id, created_at);
CREATE INDEX idx_ledger_transaction ON ledger_entries(transaction_id);
