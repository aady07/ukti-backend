package com.ukti.education.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class ClassModuleRunStartRequest {
    @NotNull
    private UUID schoolId;
    @NotNull
    private UUID classId;
    @NotBlank
    private String moduleId;
    private UUID initiatedBy;
    @NotNull
    private List<String> sectionIds;

    /**
     * Ordered activity slugs per business section id (same keys as sectionIds).
     * Example: { "section-1": ["playgroup-colour-1", "playgroup-letter-1"], ... }
     */
    @NotNull
    private Map<String, List<String>> sectionActivities;

    private String completionRule;
    private Integer completionTarget;
}
