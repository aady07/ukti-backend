package com.ukti.education.repository;

import com.ukti.education.entity.CurriculumRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurriculumReleaseRepository extends JpaRepository<CurriculumRelease, UUID> {

    Optional<CurriculumRelease> findTopByClassLevelOrderByVersionDesc(String classLevel);

    List<CurriculumRelease> findAllByOrderByClassLevelAscVersionDesc();
}
