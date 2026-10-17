package com.anil.banking.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Empties all tables between tests. TRUNCATE does not fire the row-level triggers that
 * protect the ledger and audit log, so it is the only way to reset them.
 */
@Component
public class DatabaseCleaner {

    private final JdbcTemplate jdbc;

    public DatabaseCleaner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void clean() {
        jdbc.execute("TRUNCATE ledger_entries, transactions, audit_log, users, accounts, customers RESTART IDENTITY CASCADE");
    }
}
