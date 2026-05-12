package com.ukti.education.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Per-batch counter increments. Omitted or null fields count as 0. Server stores running totals (delta merge).
 */
@Data
public class EngagementCountersDelta {

    @JsonProperty("video_complete")
    private Integer videoComplete;

    @JsonProperty("video_skip")
    private Integer videoSkip;

    @JsonProperty("video_replay")
    private Integer videoReplay;

    @JsonProperty("video_error")
    private Integer videoError;

    @JsonProperty("vision_attempts")
    private Integer visionAttempts;

    @JsonProperty("vision_passes")
    private Integer visionPasses;

    @JsonProperty("vision_failures")
    private Integer visionFailures;

    @JsonProperty("stt_listen_starts")
    private Integer sttListenStarts;

    @JsonProperty("pron_pass")
    private Integer pronPass;

    @JsonProperty("pron_fail")
    private Integer pronFail;

    @JsonProperty("skip_audio")
    private Integer skipAudio;

    @JsonProperty("stt_empty_cycles")
    private Integer sttEmptyCycles;

    @JsonProperty("client_error")
    private Integer clientError;
}
