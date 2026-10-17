package com.anil.banking.dto;

import com.anil.banking.entity.EntryType;
import com.anil.banking.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record StatementResponse(String accountNumber, LocalDate from, LocalDate to,
                                BigDecimal openingBalance, BigDecimal closingBalance,
                                BigDecimal totalCredits, BigDecimal totalDebits,
                                List<Line> entries) {

    public record Line(LocalDateTime date, String referenceNo, TransactionType transactionType,
                       EntryType entryType, BigDecimal amount, BigDecimal balanceAfter, String description) {
    }
}
