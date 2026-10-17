package com.anil.banking.service;

import com.anil.banking.dto.AccountResponse;
import com.anil.banking.dto.AdminCustomerResponse;
import com.anil.banking.dto.AdminStatsResponse;
import com.anil.banking.dto.TransactionResponse;
import com.anil.banking.entity.AccountStatus;
import com.anil.banking.entity.AppUser;
import com.anil.banking.repository.AccountRepository;
import com.anil.banking.repository.AppUserRepository;
import com.anil.banking.repository.CustomerRepository;
import com.anil.banking.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Read-only views for bank staff. */
@Service
public class AdminService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final AppUserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public AdminService(CustomerRepository customerRepository, AccountRepository accountRepository,
                        AppUserRepository userRepository, TransactionRepository transactionRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        return new AdminStatsResponse(
                customerRepository.count(),
                accountRepository.count(),
                accountRepository.countByStatus(AccountStatus.FROZEN),
                Objects.requireNonNullElse(accountRepository.totalBalance(), BigDecimal.ZERO),
                transactionRepository.countByCreatedAtGreaterThanEqual(LocalDate.now().atStartOfDay()));
    }

    @Transactional(readOnly = true)
    public List<AdminCustomerResponse> customers() {
        Map<Long, List<AccountResponse>> accountsByCustomer = accountRepository.findAll(Sort.by("id")).stream()
                .collect(Collectors.groupingBy(a -> a.getCustomer().getId(),
                        Collectors.mapping(AccountResponse::from, Collectors.toList())));
        Map<Long, String> usernameByCustomer = userRepository.findAll().stream()
                .filter(u -> u.getCustomer() != null)
                .collect(Collectors.toMap(u -> u.getCustomer().getId(), AppUser::getUsername));

        return customerRepository.findAll(Sort.by("id")).stream()
                .map(c -> new AdminCustomerResponse(c.getId(), c.getFullName(), c.getEmail(), c.getPhone(),
                        usernameByCustomer.get(c.getId()), c.getCreatedAt(),
                        accountsByCustomer.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> recentTransactions(int limit) {
        int size = Math.max(1, Math.min(limit, 200));
        return transactionRepository.findRecent(PageRequest.of(0, size)).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
