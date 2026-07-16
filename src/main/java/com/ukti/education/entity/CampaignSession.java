package com.ukti.education.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "campaign_sessions",
        indexes = {
                @Index(name = "idx_campaign_sessions_created", columnList = "created_at"),
                @Index(name = "idx_campaign_sessions_cognito", columnList = "cognito_sub")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "utm_source", length = 120)
    private String utmSource;

    @Column(name = "utm_medium", length = 120)
    private String utmMedium;

    @Column(name = "utm_campaign", length = 120)
    private String utmCampaign;

    @Column(name = "pack_id", length = 80)
    private String packId;

    @Column(name = "cognito_sub", length = 128)
    private String cognitoSub;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "free_completed_at")
    private Instant freeCompletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (startedAt == null) startedAt = now;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
