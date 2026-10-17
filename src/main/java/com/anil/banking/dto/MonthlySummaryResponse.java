package com.anil.banking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlySummaryResponse(LocalDate month, BigDecimal totalCredits,
                                     BigDecimal totalDebits, long entryCount) {
}
