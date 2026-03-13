package com.ukti.education.repository;

import com.ukti.education.entity.TeacherClass;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherClassRepository extends JpaRepository<TeacherClass, TeacherClass.TeacherClassId> {

    List<TeacherClass> findByTeacherId(UUID teacherId);

    boolean existsByTeacherIdAndClassId(UUID teacherId, UUID classId);

    void deleteByTeacherIdAndClassId(UUID teacherId, UUID classId);
}
