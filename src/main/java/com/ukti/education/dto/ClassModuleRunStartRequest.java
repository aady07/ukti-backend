package com.ukti.education.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClassModuleRunStartRequest {
    @NotNull
    private UUID schoolId;
    @NotNull
    private UUID classId;
    @NotBlank
    private String moduleId;
    private UUID initiatedBy;
    /** When omitted, derived from curriculum catalog by moduleId. */
    private List<String> sectionIds;

    /**
     * Ordered activity slugs per business section id (same keys as sectionIds).
     * When omitted, derived from curriculum catalog by moduleId.
     */
    private Map<String, List<String>> sectionActivities;

    private String completionRule;
    private Integer completionTarget;

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getClassId() {
        return classId;
    }

    public void setClassId(UUID classId) {
        this.classId = classId;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public UUID getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(UUID initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public List<String> getSectionIds() {
        return sectionIds;
    }

    public void setSectionIds(List<String> sectionIds) {
        this.sectionIds = sectionIds;
    }

    public Map<String, List<String>> getSectionActivities() {
        return sectionActivities;
    }

    public void setSectionActivities(Map<String, List<String>> sectionActivities) {
        this.sectionActivities = sectionActivities;
    }

    public String getCompletionRule() {
        return completionRule;
    }

    public void setCompletionRule(String completionRule) {
        this.completionRule = completionRule;
    }

    public Integer getCompletionTarget() {
        return completionTarget;
    }

    public void setCompletionTarget(Integer completionTarget) {
        this.completionTarget = completionTarget;
    }
}
