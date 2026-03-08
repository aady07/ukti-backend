package com.ukti.education.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExperientialStatRequest {

    @NotBlank(message = "statType is required")
    private String statType;  // "questions_attempted", "challenges_completed", etc.

    private Integer increment;  // Default 1
}
