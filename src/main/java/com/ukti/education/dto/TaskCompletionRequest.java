package com.ukti.education.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class TaskCompletionRequest {

    @NotBlank(message = "moduleId is required")
    private String moduleId;

    @NotBlank(message = "taskId is required")
    private String taskId;

    private String taskType;  // "word", "challenge", "exercise", "video_chapter"

    private Map<String, Object> metadata;
}
