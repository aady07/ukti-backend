package com.ukti.education.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClassSectionSummaryResponse {
    private int totalStudents;
    private long completedStudents;
    private int completionPct;
    private String gateStatus;
    private List<StudentItem> students;

    @Data
    @Builder
    public static class StudentItem {
        private String rollNumber;
        private String status;
        private String lastActivity;
        private String startedAt;
        private String completedAt;
    }
}
