package com.anil.banking.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String accountNumber, BigDecimal balance, BigDecimal requested) {
        super("Insufficient funds in account " + accountNumber
                + ": balance " + balance + ", requested " + requested);
    }
}
