package com.anil.banking.controller;

import com.anil.banking.dto.AuthResponse;
import com.anil.banking.dto.LoginRequest;
import com.anil.banking.dto.ProfileResponse;
import com.anil.banking.dto.RegisterRequest;
import com.anil.banking.security.AccessGuard;
import com.anil.banking.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AccessGuard accessGuard;

    public AuthController(AuthService authService, AccessGuard accessGuard) {
        this.authService = authService;
        this.accessGuard = accessGuard;
    }

    /** Sign up: creates customer + login + a first savings account, and logs the user in. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Who am I? Returns the logged-in user's profile and accounts. */
    @GetMapping("/me")
    public ProfileResponse me() {
        return authService.profile(accessGuard.currentUser());
    }
}
