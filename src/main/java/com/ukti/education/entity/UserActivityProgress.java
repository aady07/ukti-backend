package com.ukti.education.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user_activity_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "unit_slug", "activity_slug"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "unit_slug", nullable = false, length = 100)
    private String unitSlug;

    @Column(name = "activity_slug", nullable = false, length = 150)
    private String activitySlug;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @PrePersist
    protected void onCreate() {
        if (completedAt == null) completedAt = Instant.now();
    }
}
