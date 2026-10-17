package com.anil.banking.repository;

import com.anil.banking.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    /** Entries in [from, to), with their transaction loaded in the same query (no N+1). */
    @Query("""
            SELECT e FROM LedgerEntry e
            JOIN FETCH e.transaction
            WHERE e.account.id = :accountId
              AND e.createdAt >= :from AND e.createdAt < :to
            ORDER BY e.id
            """)
    List<LedgerEntry> findForStatement(@Param("accountId") Long accountId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /** Running balance just before a date = opening balance of a statement. */
    Optional<LedgerEntry> findFirstByAccountIdAndCreatedAtBeforeOrderByIdDesc(Long accountId, LocalDateTime before);

    /** Reads the v_monthly_account_summary database view. */
    @Query(value = """
            SELECT month AS "month", total_credits AS "totalCredits",
                   total_debits AS "totalDebits", entry_count AS "entryCount"
            FROM v_monthly_account_summary
            WHERE account_id = :accountId
            ORDER BY month DESC
            """, nativeQuery = true)
    List<MonthlySummaryView> findMonthlySummary(@Param("accountId") Long accountId);

    interface MonthlySummaryView {
        LocalDate getMonth();
        BigDecimal getTotalCredits();
        BigDecimal getTotalDebits();
        Long getEntryCount();
    }
}
