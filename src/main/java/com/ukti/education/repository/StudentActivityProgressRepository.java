package com.ukti.education.repository;

import com.ukti.education.entity.StudentActivityProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentActivityProgressRepository extends JpaRepository<StudentActivityProgress, UUID> {

    Optional<StudentActivityProgress> findByRunIdAndIdempotencyKey(UUID runId, String idempotencyKey);

    List<StudentActivityProgress> findByRunIdAndSectionIdAndRollNumberOrderByCreatedAtAsc(
            UUID runId, String sectionId, String rollNumber);
}
