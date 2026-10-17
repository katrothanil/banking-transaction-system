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
}
