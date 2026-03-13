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
public class SchoolProgressOverviewResponse {

    private AdminProgressSummary adminProgress;
    private List<ClassProgressSummary> classes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminProgressSummary {
        private int completedCount;
        private int totalCount;
        private int percent;
        private List<UnitProgressSummary> units;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitProgressSummary {
        private String unitId;
        private int completedCount;
        private int totalCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassProgressSummary {
        private String classId;
        private String className;
        private List<StudentProgressSummary> students;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentProgressSummary {
        private String studentId;
        private String rollNumber;
        private String name;
        private int completedCount;
        private int totalCount;
        private int percent;
    }
}
