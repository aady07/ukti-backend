package com.ukti.education.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "class_section_gates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSectionGate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "section_id", nullable = false, length = 150)
    private String sectionId;

    @Column(name = "section_index", nullable = false)
    private int sectionIndex;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "completion_rule", nullable = false, length = 20)
    private String completionRule;

    @Column(name = "completion_target", nullable = false)
    private int completionTarget;

    @Column(name = "unlocked_at")
    private Instant unlockedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
