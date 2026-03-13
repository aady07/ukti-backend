package com.ukti.education.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class GroupActivityHistoryResponse {

    @Data
    @Builder
    public static class SessionSummary {
        private UUID id;
        private String activityId;
        private Map<String, Object> groups;
        private Map<String, Object> scores;
        private Integer winnerGroup;
        private Instant createdAt;
    }

    private List<SessionSummary> sessions;
}
