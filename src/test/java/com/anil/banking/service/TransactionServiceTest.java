package com.anil.banking.service;

import com.anil.banking.dto.*;
import com.anil.banking.entity.AccountStatus;
import com.anil.banking.entity.AccountType;
import com.anil.banking.entity.EntryType;
import com.anil.banking.exception.DuplicateResourceException;
import com.anil.banking.exception.InsufficientFundsException;
import com.anil.banking.exception.InvalidOperationException;
import com.anil.banking.support.DatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TransactionServiceTest {

    @Autowired private CustomerService customerService;
    @Autowired private AccountService accountService;
    @Autowired private TransactionService transactionService;
    @Autowired private StatementService statementService;
    @Autowired private DatabaseCleaner cleaner;
    @Autowired private JdbcTemplate jdbc;

    private String alice;
    private String bob;

    @BeforeEach
    void setUp() {
        cleaner.clean();
        Long customerId = customerService.create(
                new CreateCustomerRequest("Test User", "test.user@example.com", "9876543210")).id();
        alice = accountService.open(new CreateAccountRequest(customerId, AccountType.SAVINGS)).accountNumber();
        bob = accountService.open(new CreateAccountRequest(customerId, AccountType.CURRENT)).accountNumber();
        transactionService.deposit(alice, new AmountRequest(money("1000.00"), null, null));
    }

    @Test
    void depositIncreasesBalanceAndWritesCreditEntry() {
        transactionService.deposit(bob, new AmountRequest(money("250.50"), null, "Salary"));

        assertThat(balance(bob)).isEqualByComparingTo("250.50");
        assertThat(ledgerCount(bob, EntryType.CREDIT)).isEqualTo(1);
    }

    @Test
    void withdrawalMoreThanBalanceIsRejectedAndChangesNothing() {
        assertThatThrownBy(() -> transactionService.withdraw(alice, new AmountRequest(money("1000.01"), null, null)))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(balance(alice)).isEqualByComparingTo("1000.00");
        assertThat(count("transactions")).isEqualTo(1); // only the setup deposit
    }

    @Test
    void transferMovesMoneyAndWritesBalancedLedger() {
        TransactionResponse txn = transactionService.transfer(
                new TransferRequest(alice, bob, money("300.00"), null, null));

        assertThat(balance(alice)).isEqualByComparingTo("700.00");
        assertThat(balance(bob)).isEqualByComparingTo("300.00");
        assertThat(txn.fromAccountNumber()).isEqualTo(alice);
        assertThat(txn.toAccountNumber()).isEqualTo(bob);

        BigDecimal net = jdbc.queryForObject("""
                SELECT SUM(CASE WHEN l.entry_type = 'CREDIT' THEN l.amount ELSE -l.amount END)
                FROM ledger_entries l JOIN transactions t ON t.id = l.transaction_id
                WHERE t.reference_no = ?""", BigDecimal.class, txn.referenceNo());
        assertThat(net).isEqualByComparingTo("0"); // debit and credit cancel out
    }

    @Test
    void failedTransferRollsBackBothSides() {
        assertThatThrownBy(() -> transactionService.transfer(
                new TransferRequest(alice, bob, money("5000.00"), null, null)))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(balance(alice)).isEqualByComparingTo("1000.00");
        assertThat(balance(bob)).isEqualByComparingTo("0.00");
    }

    @Test
    void retryWithSameReferenceMovesMoneyOnlyOnce() {
        TransferRequest request = new TransferRequest(alice, bob, money("100.00"), "PAY-001", null);

        TransactionResponse first = transactionService.transfer(request);
        TransactionResponse retry = transactionService.transfer(request);

        assertThat(retry.referenceNo()).isEqualTo(first.referenceNo());
        assertThat(balance(alice)).isEqualByComparingTo("900.00");
        assertThat(balance(bob)).isEqualByComparingTo("100.00");
    }

    @Test
    void reusingReferenceForDifferentRequestIsRejected() {
        transactionService.transfer(new TransferRequest(alice, bob, money("100.00"), "PAY-002", null));

        assertThatThrownBy(() -> transactionService.transfer(
                new TransferRequest(alice, bob, money("999.00"), "PAY-002", null)))
                .isInstanceOf(DuplicateResourceException.class);
        assertThat(balance(alice)).isEqualByComparingTo("900.00");
    }

    @Test
    void transferToSameAccountIsRejected() {
        assertThatThrownBy(() -> transactionService.transfer(
                new TransferRequest(alice, alice, money("10.00"), null, null)))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void frozenAccountCannotSendOrReceive() {
        accountService.updateStatus(bob, AccountStatus.FROZEN);

        assertThatThrownBy(() -> transactionService.transfer(
                new TransferRequest(alice, bob, money("10.00"), null, null)))
                .isInstanceOf(InvalidOperationException.class);
        assertThat(balance(alice)).isEqualByComparingTo("1000.00");
    }

    @Test
    void accountWithMoneyCannotBeClosed() {
        assertThatThrownBy(() -> accountService.updateStatus(alice, AccountStatus.CLOSED))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void balanceChangesAreWrittenToAuditLogByTrigger() {
        transactionService.withdraw(alice, new AmountRequest(money("50.00"), null, null));

        Integer updates = jdbc.queryForObject("""
                SELECT COUNT(*) FROM audit_log a JOIN accounts acc ON acc.id = a.record_id
                WHERE a.table_name = 'accounts' AND a.action = 'UPDATE' AND acc.account_number = ?""",
                Integer.class, alice);
        assertThat(updates).isEqualTo(2); // setup deposit + this withdrawal
    }

    @Test
    void statementShowsOpeningClosingAndTotals() {
        transactionService.transfer(new TransferRequest(alice, bob, money("200.00"), null, null));
        transactionService.withdraw(alice, new AmountRequest(money("50.00"), null, null));

        LocalDate today = LocalDate.now();
        StatementResponse statement = statementService.statement(alice, today, today);

        assertThat(statement.openingBalance()).isEqualByComparingTo("0.00");
        assertThat(statement.totalCredits()).isEqualByComparingTo("1000.00");
        assertThat(statement.totalDebits()).isEqualByComparingTo("250.00");
        assertThat(statement.closingBalance()).isEqualByComparingTo("750.00");
        assertThat(statement.entries()).hasSize(3);
    }

    private BigDecimal balance(String accountNumber) {
        return accountService.get(accountNumber).balance();
    }

    private int ledgerCount(String accountNumber, EntryType type) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ledger_entries l JOIN accounts a ON a.id = l.account_id
                WHERE a.account_number = ? AND l.entry_type = ?""", Integer.class, accountNumber, type.name());
        return n == null ? 0 : n;
    }

    private int count(String table) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return n == null ? 0 : n;
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
