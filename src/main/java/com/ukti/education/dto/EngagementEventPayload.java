package com.ukti.education.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
public class EngagementEventPayload {

    private UUID id;

    private Instant ts;

    private String type;

    private String phase;

    @JsonProperty("challengeIndex")
    private Integer challengeIndex;

    private Map<String, Object> payload;
}
