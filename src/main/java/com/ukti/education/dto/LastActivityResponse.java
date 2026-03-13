package com.ukti.education.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LastActivityResponse {

    @Data
    @Builder
    public static class LastActivity {
        private String activityId;
        private String activityLabel;
        private String unitId;
        private Instant completedAt;
        private String rollNumber;
        private String studentName;
    }

    private LastActivity lastActivity;
}
