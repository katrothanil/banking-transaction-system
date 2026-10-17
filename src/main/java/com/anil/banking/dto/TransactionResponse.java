package com.anil.banking.dto;

import com.anil.banking.entity.Account;
import com.anil.banking.entity.BankTransaction;
import com.anil.banking.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(String referenceNo, TransactionType type, BigDecimal amount,
                                  String fromAccountNumber, String toAccountNumber,
                                  String description, LocalDateTime createdAt) {

    public static TransactionResponse from(BankTransaction t) {
        return new TransactionResponse(t.getReferenceNo(), t.getType(), t.getAmount(),
                numberOf(t.getFromAccount()), numberOf(t.getToAccount()),
                t.getDescription(), t.getCreatedAt());
    }

    private static String numberOf(Account account) {
        return account == null ? null : account.getAccountNumber();
    }
}
