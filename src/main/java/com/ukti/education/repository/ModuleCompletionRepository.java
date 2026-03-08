package com.ukti.education.repository;

import com.ukti.education.entity.ModuleCompletion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleCompletionRepository extends JpaRepository<ModuleCompletion, UUID> {

    List<ModuleCompletion> findByUserIdOrderByCompletedAtDesc(UUID userId);

    Optional<ModuleCompletion> findByUserIdAndModuleId(UUID userId, String moduleId);

    boolean existsByUserIdAndModuleId(UUID userId, String moduleId);
}
