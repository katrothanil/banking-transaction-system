package com.anil.banking.dto;

import com.anil.banking.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull AccountStatus status) {
}
