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
public class ClassResponse {

    private String id;
    private String name;
    private Integer studentCount;
    /** Primary teacher (main, or first assigned) — same as GET /classes */
    private String teacherId;
    private String teacherName;
    /** All teachers for this class (join table); use for multi-teacher UI */
    private List<ClassTeacherSummary> teachers;
}
