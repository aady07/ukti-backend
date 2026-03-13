package com.ukti.education.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class GroupActivityCompleteResponse {

    private UUID id;
    private String activityId;
    private String schoolId;
    private String classId;
    private Integer winnerGroup;
    private Instant createdAt;
}
