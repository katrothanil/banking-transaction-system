-- =====================================================================
-- V3: Login accounts. A user is either a CUSTOMER (linked to exactly one
-- customer record) or an ADMIN (bank staff, not linked to a customer).
-- Passwords are stored only as BCrypt hashes, never in plain text.
-- =====================================================================

CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(30)  NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(10)  NOT NULL CHECK (role IN ('CUSTOMER', 'ADMIN')),
    customer_id    BIGINT       UNIQUE REFERENCES customers(id),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_user_role_customer CHECK (
        (role = 'CUSTOMER' AND customer_id IS NOT NULL) OR
        (role = 'ADMIN'    AND customer_id IS NULL)
    )
);
