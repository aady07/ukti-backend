package com.ukti.education.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/** PATCH /v1/class-modules/runs/{runId} — deep-merge {@code classRuntimeState}. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassModuleRunPatchRequest {
    /**
     * Arbitrary JSON object. Use {@code Map} — Jackson 3 cannot deserialize request bodies into
     * {@code JsonNode}/{@code ObjectNode} fields reliably.
     */
    private Map<String, Object> classRuntimeState;
}
