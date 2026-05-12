package com.ukti.education.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "activity_engagement_session",
        indexes = {
                @Index(name = "idx_aes_school_class_roll_started", columnList = "school_id,class_id,roll_number,started_at"),
                @Index(name = "idx_aes_unit_activity_started", columnList = "unit_slug,activity_slug,started_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityEngagementSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_session_id", nullable = false, unique = true)
    private UUID clientSessionId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "roll_number", length = 64)
    private String rollNumber;

    @Column(name = "run_id")
    private UUID runId;

    @Column(name = "section_id", length = 200)
    private String sectionId;

    @Column(name = "unit_slug", nullable = false, length = 200)
    private String unitSlug;

    @Column(name = "activity_slug", nullable = false, length = 200)
    private String activitySlug;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "video_complete", nullable = false)
    @Builder.Default
    private int videoComplete = 0;

    @Column(name = "video_skip", nullable = false)
    @Builder.Default
    private int videoSkip = 0;

    @Column(name = "video_replay", nullable = false)
    @Builder.Default
    private int videoReplay = 0;

    @Column(name = "video_error", nullable = false)
    @Builder.Default
    private int videoError = 0;

    @Column(name = "vision_attempts", nullable = false)
    @Builder.Default
    private int visionAttempts = 0;

    @Column(name = "vision_passes", nullable = false)
    @Builder.Default
    private int visionPasses = 0;

    @Column(name = "vision_failures", nullable = false)
    @Builder.Default
    private int visionFailures = 0;

    @Column(name = "stt_listen_starts", nullable = false)
    @Builder.Default
    private int sttListenStarts = 0;

    @Column(name = "pron_pass", nullable = false)
    @Builder.Default
    private int pronPass = 0;

    @Column(name = "pron_fail", nullable = false)
    @Builder.Default
    private int pronFail = 0;

    @Column(name = "skip_audio", nullable = false)
    @Builder.Default
    private int skipAudio = 0;

    @Column(name = "stt_empty_cycles", nullable = false)
    @Builder.Default
    private int sttEmptyCycles = 0;

    @Column(name = "client_error", nullable = false)
    @Builder.Default
    private int clientError = 0;

    @Column(name = "payload_version", nullable = false)
    @Builder.Default
    private int payloadVersion = 1;

    @Column(name = "raw_events", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode rawEvents;

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
