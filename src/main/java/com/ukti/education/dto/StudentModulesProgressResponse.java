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
public class StudentModulesProgressResponse {

    private String classId;
    private String classLevel;
    private String rollNumber;
    private List<ModuleProgressItem> modules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleProgressItem {
        private String moduleId;
        private int completedCount;
        private int totalCount;
        private int percent;
    }
}
