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
public class UserProgressResponse {

    private List<ModuleProgressItem> modules;
    private List<TaskProgressItem> tasks;
    private List<ExperientialStatItem> experientialStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleProgressItem {
        private String moduleId;
        private String completedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskProgressItem {
        private String moduleId;
        private String taskId;
        private String taskType;
        private String completedAt;
        private Object metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperientialStatItem {
        private String moduleId;
        private String statType;
        private int count;
    }
}
