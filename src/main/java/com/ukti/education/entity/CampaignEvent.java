package com.ukti.education.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "campaign_events",
        indexes = {
                @Index(name = "idx_campaign_events_session", columnList = "session_id"),
                @Index(name = "idx_campaign_events_type", columnList = "event_type"),
                @Index(name = "idx_campaign_events_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "path", length = 400)
    private String path;

    @Column(name = "payload_json", columnDefinition = "text")
    private String payloadJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
