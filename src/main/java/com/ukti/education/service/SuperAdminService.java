package com.ukti.education.service;

import com.ukti.education.entity.User;
import com.ukti.education.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * DB-only super admin (email + password). No Cognito.
 */
@Service
@Slf4j
public class SuperAdminService implements ApplicationRunner {

    public static final String USER_TYPE_SUPER_ADMIN = "super_admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SuperAdminJwtService superAdminJwtService;

    @Value("${ukti.super-admin.email:}")
    private String bootstrapEmail;

    @Value("${ukti.super-admin.password:}")
    private String bootstrapPassword;

    public SuperAdminService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SuperAdminJwtService superAdminJwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.superAdminJwtService = superAdminJwtService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureBootstrapSuperAdmin();
    }

    public void ensureBootstrapSuperAdmin() {
        if (bootstrapEmail == null || bootstrapEmail.isBlank()
                || bootstrapPassword == null || bootstrapPassword.isBlank()) {
            return;
        }
        String email = bootstrapEmail.trim().toLowerCase();
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User u = existing.get();
            u.setUserType(USER_TYPE_SUPER_ADMIN);
            u.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
            if (u.getUsername() == null) u.setUsername(email);
            if (u.getDisplayName() == null) u.setDisplayName("Super Admin");
            userRepository.save(u);
            log.info("Super admin ready (updated) {}", email);
            return;
        }
        userRepository.save(User.builder()
                .email(email)
                .username(email)
                .displayName("Super Admin")
                .userType(USER_TYPE_SUPER_ADMIN)
                .passwordHash(passwordEncoder.encode(bootstrapPassword))
                .build());
        log.info("Super admin ready (created) {}", email);
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> login(String email, String password) {
        if (email == null || password == null) return Optional.empty();
        String normalized = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalized);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(email.trim());
        }
        if (userOpt.isEmpty()) return Optional.empty();
        User user = userOpt.get();
        if (!USER_TYPE_SUPER_ADMIN.equalsIgnoreCase(user.getUserType())) {
            return Optional.empty();
        }
        String hash = user.getPasswordHash();
        if (hash == null || hash.isBlank() || "COGNITO".equals(hash) || "STUDENT".equals(hash)) {
            return Optional.empty();
        }
        if (!passwordEncoder.matches(password, hash)) {
            return Optional.empty();
        }
        String token = superAdminJwtService.createToken(user.getId(), user.getEmail());
        return Optional.of(Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.getId().toString(),
                        "email", user.getEmail(),
                        "displayName", user.getDisplayName() != null ? user.getDisplayName() : "Super Admin",
                        "userType", USER_TYPE_SUPER_ADMIN
                )
        ));
    }
}
