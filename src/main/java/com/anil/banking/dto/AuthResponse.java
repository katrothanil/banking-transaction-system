package com.anil.banking.dto;

import com.anil.banking.entity.Role;

/** Returned after login or sign-up. The client sends the token as "Authorization: Bearer <token>". */
public record AuthResponse(String token, String tokenType, long expiresInSeconds,
                           String username, String fullName, Role role) {
}
