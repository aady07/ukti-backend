package com.ukti.education.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "campaign_leads",
        indexes = {
                @Index(name = "idx_campaign_leads_email", columnList = "email"),
                @Index(name = "idx_campaign_leads_cognito", columnList = "cognito_sub")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignLead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "cognito_sub", length = 128)
    private String cognitoSub;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private String status = "anon";

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
