package com.ukti.education.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "teacher_classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(TeacherClass.TeacherClassId.class)
public class TeacherClass {

    @Id
    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Id
    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "is_main_teacher")
    private Boolean isMainTeacher;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherClassId implements Serializable {
        private UUID teacherId;
        private UUID classId;
    }
}
