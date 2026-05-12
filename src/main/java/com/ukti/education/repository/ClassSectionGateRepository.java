package com.ukti.education.repository;

import com.ukti.education.entity.ClassSectionGate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassSectionGateRepository extends JpaRepository<ClassSectionGate, UUID> {

    List<ClassSectionGate> findByRunIdOrderBySectionIndexAsc(UUID runId);

    Optional<ClassSectionGate> findByRunIdAndSectionId(UUID runId, String sectionId);

    Optional<ClassSectionGate> findByRunIdAndSectionIndex(UUID runId, int sectionIndex);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ClassSectionGate> findWithLockByRunIdAndSectionId(UUID runId, String sectionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ClassSectionGate> findWithLockByRunIdAndSectionIndex(UUID runId, int sectionIndex);
}
