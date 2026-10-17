package com.anil.banking.controller;

import com.anil.banking.dto.TransactionResponse;
import com.anil.banking.dto.TransferRequest;
import com.anil.banking.exception.ResourceNotFoundException;
import com.anil.banking.security.AccessGuard;
import com.anil.banking.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccessGuard accessGuard;

    public TransactionController(TransactionService transactionService, AccessGuard accessGuard) {
        this.transactionService = transactionService;
        this.accessGuard = accessGuard;
    }

    /** Customers can send from their own accounts to ANY account number. */
    @PostMapping("/transfers")
    public TransactionResponse transfer(@Valid @RequestBody TransferRequest request) {
        accessGuard.requireAccountAccess(request.fromAccountNumber().trim());
        return transactionService.transfer(request);
    }

    @GetMapping("/transactions/{referenceNo}")
    public TransactionResponse get(@PathVariable String referenceNo) {
        TransactionResponse txn = transactionService.getByReference(referenceNo);
        if (!accessGuard.canSeeEither(txn.fromAccountNumber(), txn.toAccountNumber())) {
            // Pretend it does not exist rather than confirming someone else's reference number
            throw new ResourceNotFoundException("Transaction " + referenceNo + " not found");
        }
        return txn;
    }
}
