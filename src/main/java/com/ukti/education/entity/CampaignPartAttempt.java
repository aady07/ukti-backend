package com.ukti.education.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "campaign_part_attempts",
        indexes = {
                @Index(name = "idx_campaign_parts_session", columnList = "session_id"),
                @Index(name = "idx_campaign_parts_passage_part", columnList = "passage_id,part_index")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignPartAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "pack_id", length = 80)
    private String packId;

    @Column(name = "passage_id", nullable = false, length = 120)
    private String passageId;

    @Column(name = "part_index", nullable = false)
    private int partIndex;

    @Column(name = "expected_text", columnDefinition = "text")
    private String expectedText;

    @Column(name = "spoken_transcript", columnDefinition = "text")
    private String spokenTranscript;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "pause_count")
    @Builder.Default
    private int pauseCount = 0;

    @Column(name = "wrong_count")
    @Builder.Default
    private int wrongCount = 0;

    @Column(name = "skip_count")
    @Builder.Default
    private int skipCount = 0;

    @Column(name = "words_total")
    private Integer wordsTotal;

    @Column(name = "words_matched")
    private Integer wordsMatched;

    @Column(name = "accuracy_pct")
    private Integer accuracyPct;

    @Column(name = "gemini_score_json", columnDefinition = "text")
    private String geminiScoreJson;

    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private String status = "pending_score";

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
