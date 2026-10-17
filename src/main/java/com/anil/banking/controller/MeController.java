package com.anil.banking.controller;

import com.anil.banking.dto.AccountResponse;
import com.anil.banking.dto.CreateAccountRequest;
import com.anil.banking.dto.OpenAccountRequest;
import com.anil.banking.exception.InvalidOperationException;
import com.anil.banking.security.AccessGuard;
import com.anil.banking.security.UserPrincipal;
import com.anil.banking.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints for the logged-in customer's own data. */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final AccountService accountService;
    private final AccessGuard accessGuard;

    public MeController(AccountService accountService, AccessGuard accessGuard) {
        this.accountService = accountService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/accounts")
    public List<AccountResponse> myAccounts() {
        return accountService.listForCustomer(customerId());
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse openAccount(@Valid @RequestBody OpenAccountRequest request) {
        return accountService.open(new CreateAccountRequest(customerId(), request.accountType()));
    }

    private Long customerId() {
        UserPrincipal user = accessGuard.currentUser();
        if (user.customerId() == null) {
            throw new InvalidOperationException("Admin users do not have personal accounts");
        }
        return user.customerId();
    }
}
