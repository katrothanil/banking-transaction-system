package com.anil.banking.security;

import com.anil.banking.entity.AppUser;
import com.anil.banking.entity.Role;

/** The logged-in user, stored in Spring Security's context for the duration of a request. */
public record UserPrincipal(Long userId, String username, Role role, Long customerId, String fullName) {

    public static UserPrincipal from(AppUser user) {
        Long customerId = user.getCustomer() == null ? null : user.getCustomer().getId();
        String name = user.getCustomer() == null ? "Bank Admin" : user.getCustomer().getFullName();
        return new UserPrincipal(user.getId(), user.getUsername(), user.getRole(), customerId, name);
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
