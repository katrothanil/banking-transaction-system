package com.anil.banking.controller;

import com.anil.banking.dto.AdminCustomerResponse;
import com.anil.banking.dto.AdminStatsResponse;
import com.anil.banking.dto.TransactionResponse;
import com.anil.banking.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Bank staff views. Access is restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return adminService.stats();
    }

    @GetMapping("/customers")
    public List<AdminCustomerResponse> customers() {
        return adminService.customers();
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> recentTransactions(@RequestParam(defaultValue = "50") int limit) {
        return adminService.recentTransactions(limit);
    }
}
