package com.ukti.education.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Marks a {@code batchId} as applied for a session so retries do not double-apply counter deltas.
 */
@Entity
@Table(
        name = "activity_engagement_processed_batch",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_aes_batch_session_batch",
                columnNames = {"session_id", "batch_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityEngagementProcessedBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
