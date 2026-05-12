package com.ukti.education.repository;

import com.ukti.education.entity.StudentSectionProgress;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentSectionProgressRepository extends JpaRepository<StudentSectionProgress, UUID> {

    List<StudentSectionProgress> findByRunIdAndRollNumberOrderBySectionIdAsc(UUID runId, String rollNumber);

    Optional<StudentSectionProgress> findByRunIdAndRollNumberAndSectionId(UUID runId, String rollNumber, String sectionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StudentSectionProgress> findWithLockByRunIdAndRollNumberAndSectionId(UUID runId, String rollNumber, String sectionId);

    long countByRunIdAndSectionIdAndStatus(UUID runId, String sectionId, String status);

    List<StudentSectionProgress> findByRunIdAndSectionId(UUID runId, String sectionId);
}
