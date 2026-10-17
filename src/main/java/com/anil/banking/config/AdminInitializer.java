package com.anil.banking.config;

import com.anil.banking.entity.AppUser;
import com.anil.banking.entity.Role;
import com.anil.banking.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the bank admin login on first start-up if it does not exist yet. */
@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
                            @Value("${app.admin.username:admin}") String username,
                            @Value("${app.admin.password:Admin@123}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(new AppUser(username, passwordEncoder.encode(password), Role.ADMIN, null));
            log.info("Created admin login '{}' (change app.admin.password for real use)", username);
        }
    }
}
