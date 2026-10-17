package com.anil.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body for deposit and withdrawal.
 * referenceNo is optional: send the same value when retrying so the money moves only once.
 */
public record AmountRequest(
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @Size(max = 64) String referenceNo,
        @Size(max = 255) String description
) {
}
