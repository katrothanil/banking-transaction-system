package com.anil.banking.dto;

import com.anil.banking.entity.Account;
import com.anil.banking.entity.AccountStatus;
import com.anil.banking.entity.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(String accountNumber, Long customerId, AccountType accountType,
                              BigDecimal balance, AccountStatus status, LocalDateTime createdAt) {

    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getAccountNumber(), a.getCustomer().getId(), a.getAccountType(),
                a.getBalance(), a.getStatus(), a.getCreatedAt());
    }
}
