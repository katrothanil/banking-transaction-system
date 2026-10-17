-- =====================================================================
-- V2: Audit log (trigger), immutable ledger, and reporting view
-- =====================================================================

-- Append-only audit log. Every INSERT/UPDATE on accounts is recorded here
-- automatically by the database, so no code path can skip it.
CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    table_name  VARCHAR(50) NOT NULL,
    record_id   BIGINT      NOT NULL,
    action      VARCHAR(10) NOT NULL,
    old_data    JSONB,
    new_data    JSONB,
    changed_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_record ON audit_log(table_name, record_id);

CREATE OR REPLACE FUNCTION fn_audit_accounts()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO audit_log (table_name, record_id, action, new_data)
        VALUES ('accounts', NEW.id, 'INSERT',
                jsonb_build_object('balance', NEW.balance, 'status', NEW.status));
    ELSIF TG_OP = 'UPDATE' THEN
        -- Only log real changes to balance or status
        IF NEW.balance IS DISTINCT FROM OLD.balance OR NEW.status IS DISTINCT FROM OLD.status THEN
            INSERT INTO audit_log (table_name, record_id, action, old_data, new_data)
            VALUES ('accounts', NEW.id, 'UPDATE',
                    jsonb_build_object('balance', OLD.balance, 'status', OLD.status),
                    jsonb_build_object('balance', NEW.balance, 'status', NEW.status));
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_accounts
AFTER INSERT OR UPDATE ON accounts
FOR EACH ROW EXECUTE FUNCTION fn_audit_accounts();

-- Keep updated_at current on every account change.
CREATE OR REPLACE FUNCTION fn_touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_accounts_updated_at
BEFORE UPDATE ON accounts
FOR EACH ROW EXECUTE FUNCTION fn_touch_updated_at();

-- Ledger and audit rows are history: they may never be edited or deleted.
CREATE OR REPLACE FUNCTION fn_block_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION '% is append-only: % is not allowed', TG_TABLE_NAME, TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_immutable
BEFORE UPDATE OR DELETE ON ledger_entries
FOR EACH ROW EXECUTE FUNCTION fn_block_modification();

CREATE TRIGGER trg_audit_immutable
BEFORE UPDATE OR DELETE ON audit_log
FOR EACH ROW EXECUTE FUNCTION fn_block_modification();

-- Monthly summary per account, used by the /summary report endpoint.
CREATE VIEW v_monthly_account_summary AS
SELECT
    account_id,
    DATE_TRUNC('month', created_at)::DATE                              AS month,
    COALESCE(SUM(amount) FILTER (WHERE entry_type = 'CREDIT'), 0)     AS total_credits,
    COALESCE(SUM(amount) FILTER (WHERE entry_type = 'DEBIT'),  0)     AS total_debits,
    COUNT(*)                                                           AS entry_count
FROM ledger_entries
GROUP BY account_id, DATE_TRUNC('month', created_at);
