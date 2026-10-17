package com.anil.banking.service;

import com.anil.banking.dto.AmountRequest;
import com.anil.banking.dto.TransactionResponse;
import com.anil.banking.dto.TransferRequest;
import com.anil.banking.entity.Account;
import com.anil.banking.entity.BankTransaction;
import com.anil.banking.entity.EntryType;
import com.anil.banking.entity.LedgerEntry;
import com.anil.banking.entity.TransactionType;
import com.anil.banking.exception.DuplicateResourceException;
import com.anil.banking.exception.InvalidOperationException;
import com.anil.banking.exception.ResourceNotFoundException;
import com.anil.banking.repository.LedgerEntryRepository;
import com.anil.banking.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Moves money. Every public method runs in ONE database transaction, so either all of its
 * changes (balances, transaction row, ledger rows, audit rows) are committed, or none are.
 *
 * Safety rules applied in every operation:
 *  1. Lock the affected account rows (SELECT ... FOR UPDATE) before reading balances.
 *  2. When two accounts are involved, lock them in a fixed order (by account number)
 *     so two opposite transfers can never wait on each other forever (deadlock).
 *  3. Check the reference number only AFTER taking the locks, so a retried request
 *     waits for the first one to finish and then sees it, instead of running twice.
 */
@Service
public class TransactionService {

    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public TransactionService(AccountService accountService,
                              TransactionRepository transactionRepository,
                              LedgerEntryRepository ledgerEntryRepository) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public TransactionResponse deposit(String accountNumber, AmountRequest request) {
        BigDecimal amount = normalize(request.amount());
        Account account = accountService.lock(accountNumber);

        String referenceNo = referenceOrNew(request.referenceNo());
        Optional<TransactionResponse> replay = replayIfExists(referenceNo, TransactionType.DEPOSIT, amount, null, accountNumber);
        if (replay.isPresent()) {
            return replay.get();
        }

        account.credit(amount);
        BankTransaction txn = transactionRepository.save(new BankTransaction(referenceNo, TransactionType.DEPOSIT,
                amount, null, account, descriptionOr(request.description(), "Cash deposit")));
        ledgerEntryRepository.save(new LedgerEntry(txn, account, EntryType.CREDIT, amount));
        return TransactionResponse.from(txn);
    }

    @Transactional
    public TransactionResponse withdraw(String accountNumber, AmountRequest request) {
        BigDecimal amount = normalize(request.amount());
        Account account = accountService.lock(accountNumber);

        String referenceNo = referenceOrNew(request.referenceNo());
        Optional<TransactionResponse> replay = replayIfExists(referenceNo, TransactionType.WITHDRAWAL, amount, accountNumber, null);
        if (replay.isPresent()) {
            return replay.get();
        }

        account.debit(amount); // throws InsufficientFundsException -> whole transaction rolls back
        BankTransaction txn = transactionRepository.save(new BankTransaction(referenceNo, TransactionType.WITHDRAWAL,
                amount, account, null, descriptionOr(request.description(), "Cash withdrawal")));
        ledgerEntryRepository.save(new LedgerEntry(txn, account, EntryType.DEBIT, amount));
        return TransactionResponse.from(txn);
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        String fromNumber = request.fromAccountNumber().trim();
        String toNumber = request.toAccountNumber().trim();
        if (fromNumber.equals(toNumber)) {
            throw new InvalidOperationException("Cannot transfer to the same account");
        }
        BigDecimal amount = normalize(request.amount());

        // Deadlock prevention: always lock the smaller account number first.
        // Transfer A->B and transfer B->A both lock A then B, so they queue instead of deadlocking.
        boolean fromFirst = fromNumber.compareTo(toNumber) < 0;
        Account first = accountService.lock(fromFirst ? fromNumber : toNumber);
        Account second = accountService.lock(fromFirst ? toNumber : fromNumber);
        Account source = fromFirst ? first : second;
        Account target = fromFirst ? second : first;

        String referenceNo = referenceOrNew(request.referenceNo());
        Optional<TransactionResponse> replay = replayIfExists(referenceNo, TransactionType.TRANSFER, amount, fromNumber, toNumber);
        if (replay.isPresent()) {
            return replay.get();
        }

        source.debit(amount);  // fails before anything is written if funds are short
        target.credit(amount);

        BankTransaction txn = transactionRepository.save(new BankTransaction(referenceNo, TransactionType.TRANSFER,
                amount, source, target, descriptionOr(request.description(), "Transfer to " + toNumber)));
        // Double entry: the debit and the credit always have the same amount, so money is conserved.
        ledgerEntryRepository.save(new LedgerEntry(txn, source, EntryType.DEBIT, amount));
        ledgerEntryRepository.save(new LedgerEntry(txn, target, EntryType.CREDIT, amount));
        return TransactionResponse.from(txn);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByReference(String referenceNo) {
        return transactionRepository.findByReferenceNo(referenceNo)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction " + referenceNo + " not found"));
    }

    /**
     * Idempotency: if this reference was already processed, return the original result
     * (a safe retry). If the same reference is reused for a DIFFERENT request, reject it.
     */
    private Optional<TransactionResponse> replayIfExists(String referenceNo, TransactionType type, BigDecimal amount,
                                                         String fromNumber, String toNumber) {
        return transactionRepository.findByReferenceNo(referenceNo).map(existing -> {
            TransactionResponse previous = TransactionResponse.from(existing);
            boolean sameRequest = previous.type() == type
                    && previous.amount().compareTo(amount) == 0
                    && Objects.equals(previous.fromAccountNumber(), fromNumber)
                    && Objects.equals(previous.toAccountNumber(), toNumber);
            if (!sameRequest) {
                throw new DuplicateResourceException("Reference " + referenceNo + " was already used for a different transaction");
            }
            return previous;
        });
    }

    private static BigDecimal normalize(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidOperationException("Amount must be greater than zero");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static String referenceOrNew(String referenceNo) {
        return (referenceNo == null || referenceNo.isBlank())
                ? "TXN-" + UUID.randomUUID()
                : referenceNo.trim();
    }

    private static String descriptionOr(String description, String fallback) {
        return (description == null || description.isBlank()) ? fallback : description.trim();
    }
}
