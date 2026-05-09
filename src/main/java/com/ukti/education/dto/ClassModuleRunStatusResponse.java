package com.ukti.education.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClassModuleRunStatusResponse {
    private UUID runId;
    private UUID schoolId;
    private UUID classId;
    private String moduleId;
    private String status;
    private Integer activeSectionIndex;
    private List<ClassModuleRunStartResponse.GateItem> sectionGates;

    /** Hint: last roll that had activity progress recorded on this run. */
    private String lastRollNumber;
    private String lastActivityId;
    private String lastSectionId;

    /** ISO-8601 instant when the last-interaction hint was updated (same as lastInteractionAt). */
    @JsonProperty("updatedAt")
    private Instant updatedAt;

    /**
     * Client-authored sync blob (e.g. {@code stationFlowV1}). Omitted when never set.
     */
    private JsonNode classRuntimeState;
}
