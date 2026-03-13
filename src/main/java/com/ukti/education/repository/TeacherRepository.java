package com.ukti.education.repository;

import com.ukti.education.entity.Teacher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

    List<Teacher> findBySchoolIdOrderByEmail(UUID schoolId);

    Optional<Teacher> findBySchoolIdAndEmail(UUID schoolId, String email);

    List<Teacher> findByEmail(String email);

    boolean existsBySchoolIdAndEmail(UUID schoolId, String email);
}
