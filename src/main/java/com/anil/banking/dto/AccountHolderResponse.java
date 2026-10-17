package com.anil.banking.dto;

/** Shown before a transfer so the sender can confirm the receiver. The name is partly masked for privacy. */
public record AccountHolderResponse(String accountNumber, String holderName) {
}
