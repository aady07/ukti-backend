package com.ukti.education.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "student_activity_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentActivityProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "student_user_id", nullable = false)
    private UUID studentUserId;

    @Column(name = "roll_number", nullable = false, length = 100)
    private String rollNumber;

    @Column(name = "section_id", nullable = false, length = 150)
    private String sectionId;

    @Column(name = "activity_id", nullable = false, length = 150)
    private String activityId;

    @Column(name = "challenge_index")
    private Integer challengeIndex;

    @Column(nullable = false, length = 20)
    private String status;

    @Column
    private BigDecimal score;

    @Column(name = "idempotency_key", length = 200)
    private String idempotencyKey;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
