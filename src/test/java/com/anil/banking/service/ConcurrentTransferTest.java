package com.anil.banking.service;

import com.anil.banking.dto.AmountRequest;
import com.anil.banking.dto.CreateAccountRequest;
import com.anil.banking.dto.CreateCustomerRequest;
import com.anil.banking.dto.TransferRequest;
import com.anil.banking.entity.AccountType;
import com.anil.banking.exception.InsufficientFundsException;
import com.anil.banking.support.DatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fires hundreds of transfers at the same accounts from many threads at once, in random
 * directions (A->B and B->A at the same moment is the classic deadlock case).
 * Proves: no deadlocks, no lost updates, no negative balances, total money unchanged.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrentTransferTest {

    private static final int ACCOUNTS = 4;
    private static final int TRANSFERS = 400;
    private static final int THREADS = 20;
    private static final BigDecimal OPENING_BALANCE = new BigDecimal("1000.00");

    @Autowired private CustomerService customerService;
    @Autowired private AccountService accountService;
    @Autowired private TransactionService transactionService;
    @Autowired private DatabaseCleaner cleaner;
    @Autowired private JdbcTemplate jdbc;

    private final List<String> accounts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        cleaner.clean();
        accounts.clear();
        Long customerId = customerService.create(
                new CreateCustomerRequest("Load Test", "load.test@example.com", "9876543210")).id();
        for (int i = 0; i < ACCOUNTS; i++) {
            String number = accountService.open(new CreateAccountRequest(customerId, AccountType.SAVINGS)).accountNumber();
            transactionService.deposit(number, new AmountRequest(OPENING_BALANCE, null, null));
            accounts.add(number);
        }
    }

    @Test
    void parallelTransfersConserveMoney() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startTogether = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();
        Random random = new Random(42);
        int[] amounts = {5, 20, 75, 150};

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < TRANSFERS; i++) {
            int from = random.nextInt(ACCOUNTS);
            int to = (from + 1 + random.nextInt(ACCOUNTS - 1)) % ACCOUNTS;
            BigDecimal amount = BigDecimal.valueOf(amounts[random.nextInt(amounts.length)]);
            TransferRequest request = new TransferRequest(accounts.get(from), accounts.get(to), amount, null, null);

            futures.add(pool.submit(() -> {
                startTogether.await();
                try {
                    transactionService.transfer(request);
                    succeeded.incrementAndGet();
                } catch (InsufficientFundsException expected) {
                    insufficient.incrementAndGet(); // a legitimate business rejection
                } catch (Throwable t) {
                    unexpected.add(t);              // deadlocks or lost updates would land here
                }
                return null;
            }));
        }

        startTogether.countDown();
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(unexpected).as("unexpected errors such as deadlocks").isEmpty();
        assertThat(succeeded.get() + insufficient.get()).isEqualTo(TRANSFERS);

        BigDecimal total = jdbc.queryForObject("SELECT SUM(balance) FROM accounts", BigDecimal.class);
        assertThat(total).as("total money in the system")
                .isEqualByComparingTo(OPENING_BALANCE.multiply(BigDecimal.valueOf(ACCOUNTS)));

        BigDecimal minBalance = jdbc.queryForObject("SELECT MIN(balance) FROM accounts", BigDecimal.class);
        assertThat(minBalance).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        // Every account's balance must equal the sum of its ledger entries.
        Integer mismatches = jdbc.queryForObject("""
                SELECT COUNT(*) FROM accounts a
                WHERE a.balance <> (SELECT COALESCE(SUM(CASE WHEN l.entry_type = 'CREDIT' THEN l.amount ELSE -l.amount END), 0)
                                    FROM ledger_entries l WHERE l.account_id = a.id)""", Integer.class);
        assertThat(mismatches).isZero();

        Integer transferRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE transaction_type = 'TRANSFER'", Integer.class);
        assertThat(transferRows).isEqualTo(succeeded.get());
    }

    @Test
    void parallelRetriesOfSameReferenceMoveMoneyOnce() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startTogether = new CountDownLatch(1);
        TransferRequest request = new TransferRequest(accounts.get(0), accounts.get(1),
                new BigDecimal("100.00"), "RETRY-SAME-REF", null);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            futures.add(pool.submit(() -> {
                startTogether.await();
                return transactionService.transfer(request);
            }));
        }
        startTogether.countDown();
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        BigDecimal sender = accountService.get(accounts.get(0)).balance();
        BigDecimal receiver = accountService.get(accounts.get(1)).balance();
        assertThat(sender).isEqualByComparingTo("900.00");
        assertThat(receiver).isEqualByComparingTo("1100.00");
    }
}
