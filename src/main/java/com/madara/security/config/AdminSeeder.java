package com.madara.security.config;

import com.madara.security.model.AuthProvider;
import com.madara.security.model.Role;
import com.madara.security.model.User;
import com.madara.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${application.admin.email:admin@pdfextractor.com}")
    private String adminEmail;

    @Value("${application.admin.password:Admin@1234}")
    private String adminPassword;

    @Value("${application.admin.name:Admin}")
    private String adminName;

    @Override
    public void run(ApplicationArguments args) {
        boolean adminExists = userRepository.findByEmail(adminEmail).isPresent();
        if (adminExists) {
            log.info("Admin account already exists — skipping seed.");
            return;
        }

        User admin = User.builder()
                .name(adminName)
                .username("admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .roles(new HashSet<>(Set.of(Role.ADMIN)))
                .authProvider(AuthProvider.LOCAL)
                .isAccountEnabled(true)
                .isAccountNonLocked(true)
                .seededAdmin(true)
                .build();

        userRepository.save(admin);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  Default admin account created");
        log.info("  Email   : {}", adminEmail);
        log.info("  Password: {}", adminPassword);
        log.info("  Change these via ADMIN_EMAIL / ADMIN_PASSWORD env vars");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
