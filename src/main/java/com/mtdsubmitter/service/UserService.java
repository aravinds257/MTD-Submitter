package com.mtdsubmitter.service;

import com.mtdsubmitter.model.User;
import com.mtdsubmitter.model.enums.SubscriptionStatus;
import com.mtdsubmitter.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for user registration, profile management, and lookup.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /**
     * Register a new user with a 14-day trial.
     */
    @Transactional
    public User register(String email, String password, String fullName) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        User user = User.builder()
                .email(email.toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName.trim())
                .subscriptionStatus(SubscriptionStatus.TRIAL)
                .trialEndDate(LocalDateTime.now().plusDays(14))
                .emailVerified(true) // Simplified for MVP — skip email verification
                .build();

        user = userRepository.save(user);
        auditService.log(user.getId(), "User", user.getId(), "CREATE", null, null);
        return user;
    }

    /**
     * Find user by email address.
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    /**
     * Find user by ID.
     */
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    /**
     * Update user's NINO and UTR (for HMRC connection).
     */
    @Transactional
    public User updateTaxDetails(UUID userId, String nino, String utr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setNinoEncrypted(nino); // Will be encrypted via JPA converter
        user.setUtrEncrypted(utr);
        return userRepository.save(user);
    }

    /**
     * Check if user has an active subscription or is within trial period.
     */
    public boolean hasActiveAccess(User user) {
        if (user.getSubscriptionStatus() == SubscriptionStatus.ACTIVE) {
            return true;
        }
        if (user.getSubscriptionStatus() == SubscriptionStatus.TRIAL
                && user.getTrialEndDate() != null
                && user.getTrialEndDate().isAfter(LocalDateTime.now())) {
            return true;
        }
        return false;
    }
}
