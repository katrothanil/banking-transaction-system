package com.anil.banking.dto;

import com.anil.banking.entity.Customer;

import java.time.LocalDateTime;

public record CustomerResponse(Long id, String fullName, String email, String phone, LocalDateTime createdAt) {

    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getFullName(), c.getEmail(), c.getPhone(), c.getCreatedAt());
    }
}
