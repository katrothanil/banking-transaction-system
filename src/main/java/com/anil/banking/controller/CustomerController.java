package com.anil.banking.controller;

import com.anil.banking.dto.AccountResponse;
import com.anil.banking.dto.CreateCustomerRequest;
import com.anil.banking.dto.CustomerResponse;
import com.anil.banking.service.AccountService;
import com.anil.banking.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final AccountService accountService;

    public CustomerController(CustomerService customerService, AccountService accountService) {
        this.customerService = customerService;
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.create(request);
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable Long id) {
        return customerService.get(id);
    }

    @GetMapping("/{id}/accounts")
    public List<AccountResponse> accounts(@PathVariable Long id) {
        return accountService.listForCustomer(id);
    }
}
