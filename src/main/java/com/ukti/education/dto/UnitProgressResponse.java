package com.ukti.education.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitProgressResponse {

    private String unitId;  // unit_slug
    private List<CompletedActivityItem> completedActivities;
    private int completedCount;  // number of completed activities (frontend has total from static data)

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompletedActivityItem {
        private String activityId;
        private String taskId;
        private String completedAt;
        private Object metadata;
    }
}
