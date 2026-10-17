package com.anil.banking.repository;

import com.anil.banking.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<BankTransaction, Long> {

    @Query("""
            SELECT t FROM BankTransaction t
            LEFT JOIN FETCH t.fromAccount
            LEFT JOIN FETCH t.toAccount
            WHERE t.referenceNo = :referenceNo
            """)
    Optional<BankTransaction> findByReferenceNo(@Param("referenceNo") String referenceNo);

    @Query("""
            SELECT t FROM BankTransaction t
            LEFT JOIN FETCH t.fromAccount
            LEFT JOIN FETCH t.toAccount
            ORDER BY t.id DESC
            """)
    java.util.List<BankTransaction> findRecent(org.springframework.data.domain.Pageable pageable);

    long countByCreatedAtGreaterThanEqual(java.time.LocalDateTime since);
}
