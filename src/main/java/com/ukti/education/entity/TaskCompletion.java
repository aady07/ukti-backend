package com.ukti.education.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "task_completions", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "module_id", "task_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "module_id", nullable = false, length = 20)
    private String moduleId;

    @Column(name = "task_id", nullable = false, length = 100)
    private String taskId;

    @Column(name = "task_type", length = 50)
    private String taskType;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @PrePersist
    protected void onCreate() {
        if (completedAt == null) completedAt = Instant.now();
    }
}
