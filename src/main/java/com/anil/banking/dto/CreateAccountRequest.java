package com.anil.banking.dto;

import com.anil.banking.entity.AccountType;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotNull Long customerId,
        @NotNull AccountType accountType
) {
}
