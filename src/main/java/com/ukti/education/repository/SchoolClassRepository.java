package com.ukti.education.repository;

import com.ukti.education.entity.SchoolClass;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {

    List<SchoolClass> findBySchoolIdOrderByName(UUID schoolId);

    Optional<SchoolClass> findBySchoolIdAndName(UUID schoolId, String name);

    boolean existsBySchoolIdAndName(UUID schoolId, String name);
}
