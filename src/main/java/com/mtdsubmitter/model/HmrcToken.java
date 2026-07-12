package com.mtdsubmitter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "hmrc_tokens")
public class HmrcToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Convert(converter = com.mtdsubmitter.security.EncryptionConverter.class)
    @Column(nullable = false)
    private String accessTokenEncrypted;

    @Convert(converter = com.mtdsubmitter.security.EncryptionConverter.class)
    @Column(nullable = false)
    private String refreshTokenEncrypted;

    private String tokenType;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private String scope;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
