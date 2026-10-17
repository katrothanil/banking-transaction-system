package com.anil.banking.service;

import com.anil.banking.dto.*;
import com.anil.banking.entity.AccountType;
import com.anil.banking.entity.AppUser;
import com.anil.banking.entity.Customer;
import com.anil.banking.entity.Role;
import com.anil.banking.exception.DuplicateResourceException;
import com.anil.banking.repository.AppUserRepository;
import com.anil.banking.security.JwtService;
import com.anil.banking.security.UserPrincipal;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final CustomerService customerService;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepository, CustomerService customerService,
                       AccountService accountService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.customerService = customerService;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Sign-up: creates the customer, the login and a first savings account in ONE transaction,
     * so a failure (e.g. email already used) leaves nothing half-created.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username " + username + " is already taken");
        }
        CustomerResponse created = customerService.create(
                new CreateCustomerRequest(request.fullName(), request.email(), request.phone()));
        Customer customer = customerService.findCustomer(created.id());

        AppUser user = userRepository.save(new AppUser(username, passwordEncoder.encode(request.password()),
                Role.CUSTOMER, customer));
        accountService.open(new CreateAccountRequest(customer.getId(), AccountType.SAVINGS));
        return tokenFor(user, customer.getFullName());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase();
        AppUser user = userRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                // Same message for "no such user" and "wrong password", so usernames cannot be probed
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        String name = user.getCustomer() == null ? "Bank Admin" : user.getCustomer().getFullName();
        return tokenFor(user, name);
    }

    @Transactional(readOnly = true)
    public ProfileResponse profile(UserPrincipal principal) {
        if (principal.customerId() == null) {
            return new ProfileResponse(principal.username(), principal.role(), principal.fullName(),
                    null, null, List.of());
        }
        CustomerResponse customer = customerService.get(principal.customerId());
        return new ProfileResponse(principal.username(), principal.role(), customer.fullName(),
                customer.email(), customer.phone(), accountService.listForCustomer(principal.customerId()));
    }

    private AuthResponse tokenFor(AppUser user, String fullName) {
        return new AuthResponse(jwtService.createToken(user), "Bearer", jwtService.validitySeconds(),
                user.getUsername(), fullName, user.getRole());
    }
}
