package com.anil.banking.controller;

import com.anil.banking.dto.*;
import com.anil.banking.security.AccessGuard;
import com.anil.banking.service.AccountService;
import com.anil.banking.service.StatementService;
import com.anil.banking.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final StatementService statementService;
    private final AccessGuard accessGuard;

    public AccountController(AccountService accountService, TransactionService transactionService,
                             StatementService statementService, AccessGuard accessGuard) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.statementService = statementService;
        this.accessGuard = accessGuard;
    }

    /** Admin only (see SecurityConfig). Customers open accounts via POST /api/me/accounts. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.open(request);
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse get(@PathVariable String accountNumber) {
        accessGuard.requireAccountAccess(accountNumber);
        return accountService.get(accountNumber);
    }

    /** Any logged-in user: confirms who will receive a transfer (name is masked). */
    @GetMapping("/{accountNumber}/holder")
    public AccountHolderResponse holder(@PathVariable String accountNumber) {
        return accountService.holder(accountNumber);
    }

    /** Admin only (see SecurityConfig): freeze, unfreeze or close. */
    @PatchMapping("/{accountNumber}/status")
    public AccountResponse updateStatus(@PathVariable String accountNumber,
                                        @Valid @RequestBody UpdateStatusRequest request) {
        return accountService.updateStatus(accountNumber, request.status());
    }

    @PostMapping("/{accountNumber}/deposit")
    public TransactionResponse deposit(@PathVariable String accountNumber, @Valid @RequestBody AmountRequest request) {
        accessGuard.requireAccountAccess(accountNumber);
        return transactionService.deposit(accountNumber, request);
    }

    @PostMapping("/{accountNumber}/withdraw")
    public TransactionResponse withdraw(@PathVariable String accountNumber, @Valid @RequestBody AmountRequest request) {
        accessGuard.requireAccountAccess(accountNumber);
        return transactionService.withdraw(accountNumber, request);
    }

    /** Latest activity, newest first. Example: GET /api/accounts/100000000001/transactions?limit=10 */
    @GetMapping("/{accountNumber}/transactions")
    public List<StatementResponse.Line> recent(@PathVariable String accountNumber,
                                               @RequestParam(defaultValue = "10") int limit) {
        accessGuard.requireAccountAccess(accountNumber);
        return statementService.recent(accountNumber, limit);
    }

    /** Example: GET /api/accounts/100000000001/statement?from=2026-10-01&to=2026-10-31 */
    @GetMapping("/{accountNumber}/statement")
    public StatementResponse statement(@PathVariable String accountNumber,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        accessGuard.requireAccountAccess(accountNumber);
        return statementService.statement(accountNumber, from, to);
    }

    @GetMapping("/{accountNumber}/summary")
    public List<MonthlySummaryResponse> monthlySummary(@PathVariable String accountNumber) {
        accessGuard.requireAccountAccess(accountNumber);
        return statementService.monthlySummary(accountNumber);
    }
}
