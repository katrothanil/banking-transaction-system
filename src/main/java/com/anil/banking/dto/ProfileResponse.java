package com.anil.banking.dto;

import com.anil.banking.entity.Role;

import java.util.List;

/** The logged-in user's own details and accounts (accounts is empty for an admin). */
public record ProfileResponse(String username, Role role, String fullName, String email, String phone,
                              List<AccountResponse> accounts) {
}
