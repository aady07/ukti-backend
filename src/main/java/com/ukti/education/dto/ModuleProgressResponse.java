package com.ukti.education.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleProgressResponse {

    private String moduleId;
    private boolean completed;
    private Instant completedAt;
    private List<TaskProgressItem> tasks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskProgressItem {
        private String taskId;
        private String taskType;
        private Instant completedAt;
        private Object metadata;
    }
}
