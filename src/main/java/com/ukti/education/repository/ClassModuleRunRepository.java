package com.ukti.education.repository;

import com.ukti.education.entity.ClassModuleRun;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface ClassModuleRunRepository extends JpaRepository<ClassModuleRun, UUID> {

    Optional<ClassModuleRun> findBySchoolIdAndClassIdAndModuleIdAndStatus(
            UUID schoolId, UUID classId, String moduleId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ClassModuleRun> findWithLockById(UUID id);
}
