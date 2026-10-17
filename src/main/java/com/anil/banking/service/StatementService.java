package com.anil.banking.service;

import com.anil.banking.dto.MonthlySummaryResponse;
import com.anil.banking.dto.StatementResponse;
import com.anil.banking.entity.Account;
import com.anil.banking.entity.EntryType;
import com.anil.banking.entity.LedgerEntry;
import com.anil.banking.exception.InvalidOperationException;
import com.anil.banking.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StatementService {

    private final AccountService accountService;
    private final LedgerEntryRepository ledgerEntryRepository;

    public StatementService(AccountService accountService, LedgerEntryRepository ledgerEntryRepository) {
        this.accountService = accountService;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /** Statement for [from, to] inclusive, built from the ledger's running balances. */
    @Transactional(readOnly = true)
    public StatementResponse statement(String accountNumber, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidOperationException("'from' date must not be after 'to' date");
        }
        Account account = accountService.find(accountNumber);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        BigDecimal opening = ledgerEntryRepository
                .findFirstByAccountIdAndCreatedAtBeforeOrderByIdDesc(account.getId(), start)
                .map(LedgerEntry::getBalanceAfter)
                .orElse(BigDecimal.ZERO.setScale(2));

        List<LedgerEntry> entries = ledgerEntryRepository.findForStatement(account.getId(), start, end);

        BigDecimal credits = sum(entries, EntryType.CREDIT);
        BigDecimal debits = sum(entries, EntryType.DEBIT);
        BigDecimal closing = entries.isEmpty() ? opening : entries.get(entries.size() - 1).getBalanceAfter();

        List<StatementResponse.Line> lines = entries.stream()
                .map(e -> new StatementResponse.Line(e.getCreatedAt(), e.getTransaction().getReferenceNo(),
                        e.getTransaction().getType(), e.getEntryType(), e.getAmount(), e.getBalanceAfter(),
                        e.getTransaction().getDescription()))
                .toList();

        return new StatementResponse(accountNumber, from, to, opening, closing, credits, debits, lines);
    }

    @Transactional(readOnly = true)
    public List<MonthlySummaryResponse> monthlySummary(String accountNumber) {
        Account account = accountService.find(accountNumber);
        return ledgerEntryRepository.findMonthlySummary(account.getId()).stream()
                .map(v -> new MonthlySummaryResponse(v.getMonth(), v.getTotalCredits(), v.getTotalDebits(), v.getEntryCount()))
                .toList();
    }

    private static BigDecimal sum(List<LedgerEntry> entries, EntryType type) {
        return entries.stream()
                .filter(e -> e.getEntryType() == type)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}
