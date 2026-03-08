package com.ukti.education.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cognito_sub", unique = true)
    private String cognitoSub;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String username;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "school_id")
    private String schoolId;

    @Column(name = "password_hash")
    private String passwordHash;  // Nullable for Cognito users; used if migrating from Firebase

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
