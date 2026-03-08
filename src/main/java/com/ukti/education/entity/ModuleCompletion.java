package com.ukti.education.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "module_completions", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "module_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "module_id", nullable = false, length = 20)
    private String moduleId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        if (completedAt == null) completedAt = Instant.now();
    }
}
