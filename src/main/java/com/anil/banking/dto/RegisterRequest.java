package com.anil.banking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,14}$", message = "must be 10 to 14 digits, optionally starting with +")
        String phone,
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._]{3,30}$",
                message = "3 to 30 characters: letters, numbers, dot or underscore")
        String username,
        @NotBlank @Size(min = 8, max = 72, message = "must be 8 to 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "must contain at least one letter and one number")
        String password
) {
}
