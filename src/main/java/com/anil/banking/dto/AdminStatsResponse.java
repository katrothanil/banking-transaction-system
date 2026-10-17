package com.anil.banking.dto;

import java.math.BigDecimal;

public record AdminStatsResponse(long customers, long accounts, long frozenAccounts,
                                 BigDecimal totalDeposits, long transactionsToday) {
}
