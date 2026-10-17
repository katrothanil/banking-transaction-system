package com.anil.banking.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminCustomerResponse(Long id, String fullName, String email, String phone, String username,
                                    LocalDateTime createdAt, List<AccountResponse> accounts) {
}
