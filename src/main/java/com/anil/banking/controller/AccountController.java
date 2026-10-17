package com.anil.banking.controller;

import com.anil.banking.dto.*;
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

    public AccountController(AccountService accountService, TransactionService transactionService,
                             StatementService statementService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.statementService = statementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.open(request);
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse get(@PathVariable String accountNumber) {
        return accountService.get(accountNumber);
    }

    @PatchMapping("/{accountNumber}/status")
    public AccountResponse updateStatus(@PathVariable String accountNumber,
                                        @Valid @RequestBody UpdateStatusRequest request) {
        return accountService.updateStatus(accountNumber, request.status());
    }

    @PostMapping("/{accountNumber}/deposit")
    public TransactionResponse deposit(@PathVariable String accountNumber, @Valid @RequestBody AmountRequest request) {
        return transactionService.deposit(accountNumber, request);
    }

    @PostMapping("/{accountNumber}/withdraw")
    public TransactionResponse withdraw(@PathVariable String accountNumber, @Valid @RequestBody AmountRequest request) {
        return transactionService.withdraw(accountNumber, request);
    }

    /** Example: GET /api/accounts/100000000001/statement?from=2026-10-01&to=2026-10-31 */
    @GetMapping("/{accountNumber}/statement")
    public StatementResponse statement(@PathVariable String accountNumber,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statementService.statement(accountNumber, from, to);
    }

    @GetMapping("/{accountNumber}/summary")
    public List<MonthlySummaryResponse> monthlySummary(@PathVariable String accountNumber) {
        return statementService.monthlySummary(accountNumber);
    }
}
