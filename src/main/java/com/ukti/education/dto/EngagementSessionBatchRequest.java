package com.ukti.education.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class EngagementSessionBatchRequest {

    @NotNull
    private UUID batchId;

    @NotNull
    private UUID clientSessionId;

    @NotBlank
    private String unitSlug;

    @NotBlank
    private String activitySlug;

    private UUID runId;

    private String sectionId;

    private Instant startedAt;

    private Instant endedAt;

    private Boolean sessionClosed;

    @NotNull
    @Valid
    private EngagementCountersDelta counters;

    private List<EngagementEventPayload> events;

    @NotNull
    private Integer payloadVersion;
}
