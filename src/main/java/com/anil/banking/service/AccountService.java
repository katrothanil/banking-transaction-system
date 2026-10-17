package com.anil.banking.service;

import com.anil.banking.dto.AccountResponse;
import com.anil.banking.dto.CreateAccountRequest;
import com.anil.banking.entity.Account;
import com.anil.banking.entity.AccountStatus;
import com.anil.banking.entity.Customer;
import com.anil.banking.exception.ResourceNotFoundException;
import com.anil.banking.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerService customerService;

    public AccountService(AccountRepository accountRepository, CustomerService customerService) {
        this.accountRepository = accountRepository;
        this.customerService = customerService;
    }

    @Transactional
    public AccountResponse open(CreateAccountRequest request) {
        Customer customer = customerService.findCustomer(request.customerId());
        String accountNumber = String.valueOf(accountRepository.nextAccountNumber());
        Account account = accountRepository.save(new Account(accountNumber, customer, request.accountType()));
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse get(String accountNumber) {
        return AccountResponse.from(find(accountNumber));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listForCustomer(Long customerId) {
        customerService.findCustomer(customerId); // 404 if the customer does not exist
        return accountRepository.findByCustomerIdOrderByIdAsc(customerId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    /** Freeze, unfreeze or close. Locks the row so it cannot race with a transfer. */
    @Transactional
    public AccountResponse updateStatus(String accountNumber, AccountStatus status) {
        Account account = lock(accountNumber);
        account.changeStatus(status);
        return AccountResponse.from(account);
    }

    Account find(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> notFound(accountNumber));
    }

    Account lock(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> notFound(accountNumber));
    }

    private static ResourceNotFoundException notFound(String accountNumber) {
        return new ResourceNotFoundException("Account " + accountNumber + " not found");
    }
}
