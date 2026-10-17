package com.anil.banking.security;

import com.anil.banking.exception.ResourceNotFoundException;
import com.anil.banking.repository.AccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Ownership rules: a customer may only see and move money from their OWN accounts.
 * Admins may access every account. Called by controllers before the service layer runs.
 */
@Component
public class AccessGuard {

    private final AccountRepository accountRepository;

    public AccessGuard(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("Not logged in");
        }
        return principal;
    }

    public void requireAccountAccess(String accountNumber) {
        UserPrincipal user = currentUser();
        if (user.isAdmin()) {
            return;
        }
        if (!ownsAccount(user, accountNumber)) {
            throw new AccessDeniedException("Account " + accountNumber + " does not belong to you");
        }
    }

    /** True if the current user is an admin or owns at least one of the given accounts. */
    public boolean canSeeEither(String accountNumberA, String accountNumberB) {
        UserPrincipal user = currentUser();
        return user.isAdmin()
                || (accountNumberA != null && ownsAccount(user, accountNumberA))
                || (accountNumberB != null && ownsAccount(user, accountNumberB));
    }

    private boolean ownsAccount(UserPrincipal user, String accountNumber) {
        Long ownerId = accountRepository.findOwnerCustomerId(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account " + accountNumber + " not found"));
        return Objects.equals(ownerId, user.customerId());
    }
}
