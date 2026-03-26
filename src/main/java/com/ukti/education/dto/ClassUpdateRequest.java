package com.ukti.education.dto;

import java.util.List;

/**
 * PATCH body for /schools/{schoolId}/classes/{classId}.
 * Explicit accessors so IDE/javac always resolve (Lombok optional).
 */
public class ClassUpdateRequest {

    /** Legacy: single teacher UUID; blank string clears. Ignored if teacherIds is non-null. */
    private String teacherId;

    /**
     * If non-null (including empty list), replaces all class–teacher links.
     * Empty list = unassign all teachers. First id is marked main teacher.
     */
    private List<String> teacherIds;

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public List<String> getTeacherIds() {
        return teacherIds;
    }

    public void setTeacherIds(List<String> teacherIds) {
        this.teacherIds = teacherIds;
    }
}
