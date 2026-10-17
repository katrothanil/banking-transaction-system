package com.anil.banking.repository;

import com.anil.banking.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerIdOrderByIdAsc(Long customerId);

    /**
     * Loads an account with SELECT ... FOR UPDATE. Any other transaction trying to lock the
     * same row waits until this transaction commits or rolls back, so two operations can never
     * read the same balance and overwrite each other's update (the "lost update" problem).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    @Query(value = "SELECT nextval('account_number_seq')", nativeQuery = true)
    Long nextAccountNumber();
}
