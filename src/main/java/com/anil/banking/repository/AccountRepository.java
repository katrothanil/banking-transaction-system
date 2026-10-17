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

    /** Customer id of the account's owner; used by AccessGuard for ownership checks. */
    @Query("SELECT a.customer.id FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Long> findOwnerCustomerId(@Param("accountNumber") String accountNumber);

    @Query("SELECT SUM(a.balance) FROM Account a")
    java.math.BigDecimal totalBalance();

    long countByStatus(com.anil.banking.entity.AccountStatus status);
}
