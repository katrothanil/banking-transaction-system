package com.anil.banking.controller;

import com.anil.banking.dto.TransactionResponse;
import com.anil.banking.dto.TransferRequest;
import com.anil.banking.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfers")
    public TransactionResponse transfer(@Valid @RequestBody TransferRequest request) {
        return transactionService.transfer(request);
    }

    @GetMapping("/transactions/{referenceNo}")
    public TransactionResponse get(@PathVariable String referenceNo) {
        return transactionService.getByReference(referenceNo);
    }
}
