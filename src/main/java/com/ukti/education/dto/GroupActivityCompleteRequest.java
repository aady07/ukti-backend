package com.ukti.education.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class GroupActivityCompleteRequest {

    @NotBlank
    private String activityId;

    @NotBlank
    private String schoolId;

    @NotBlank
    private String classId;

    @NotNull
    private Map<String, Object> groups;  // {"1": ["1","2","3"], "2": ["5","6","7"]}

    @NotNull
    private Map<String, Object> scores;  // {"1": 30, "2": 20}

    @NotNull
    private Integer winnerGroup;

    private String sessionId;
}
