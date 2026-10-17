package com.anil.banking.entity;

import com.anil.banking.exception.InsufficientFundsException;
import com.anil.banking.exception.InvalidOperationException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 12)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 10)
    private AccountType accountType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Maintained by the database trigger trg_accounts_updated_at
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Account() {
        // required by JPA
    }

    public Account(String accountNumber, Customer customer, AccountType accountType) {
        this.accountNumber = accountNumber;
        this.customer = customer;
        this.accountType = accountType;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** Takes money out. Caller must hold a row lock on this account. */
    public void debit(BigDecimal amount) {
        requireActive();
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountNumber, balance, amount);
        }
        balance = balance.subtract(amount);
    }

    /** Adds money. Caller must hold a row lock on this account. */
    public void credit(BigDecimal amount) {
        requireActive();
        balance = balance.add(amount);
    }

    public void changeStatus(AccountStatus newStatus) {
        if (status == AccountStatus.CLOSED) {
            throw new InvalidOperationException("Account " + accountNumber + " is closed and cannot be changed");
        }
        if (newStatus == AccountStatus.CLOSED && balance.signum() != 0) {
            throw new InvalidOperationException("Account " + accountNumber
                    + " must have zero balance before closing (current: " + balance + ")");
        }
        this.status = newStatus;
    }

    private void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new InvalidOperationException("Account " + accountNumber + " is " + status);
        }
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public Customer getCustomer() { return customer; }
    public AccountType getAccountType() { return accountType; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
