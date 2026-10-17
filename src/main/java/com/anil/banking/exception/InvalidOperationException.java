package com.anil.banking.exception;

/** A request that is well-formed but not allowed in the current state (e.g. a frozen account). */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}
