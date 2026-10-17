package com.anil.banking.dto;

import com.anil.banking.entity.AccountType;
import jakarta.validation.constraints.NotNull;

/** A logged-in customer opening another account for themselves. */
public record OpenAccountRequest(@NotNull AccountType accountType) {
}
