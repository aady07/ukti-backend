package com.ukti.education.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;

/** PATCH /v1/class-modules/runs/{runId} — deep-merge {@code classRuntimeState}. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassModuleRunPatchRequest {
    private ObjectNode classRuntimeState;
}
