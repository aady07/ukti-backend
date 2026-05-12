package com.ukti.education.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "class_module_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassModuleRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "module_id", nullable = false, length = 100)
    private String moduleId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "active_section_index", nullable = false)
    private int activeSectionIndex;

    @Column(name = "section_order_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> sectionOrder;

    /** Per business-section ordered activity slugs (curriculum). Required for resolve-next to return real activity ids. */
    @Column(name = "section_activities_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, List<String>> sectionActivities;

    @Column(name = "completion_rule", nullable = false, length = 20)
    private String completionRule;

    @Column(name = "completion_target", nullable = false)
    private int completionTarget;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** Hint only: last roll that recorded activity completion on this run (server-authoritative). */
    @Column(name = "last_roll_number", length = 100)
    private String lastRollNumber;

    @Column(name = "last_activity_id", length = 150)
    private String lastActivityId;

    @Column(name = "last_section_id", length = 150)
    private String lastSectionId;

    /** Server clock when last interaction hint was updated (maps to API field updatedAt). */
    @Column(name = "last_interaction_at")
    private Instant lastInteractionAt;

    /**
     * Arbitrary client-sync state for live class flow (e.g. stationFlowV1). Deep-merged on PATCH.
     */
    @Column(name = "class_runtime_state_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode classRuntimeState;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (startedAt == null) startedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
