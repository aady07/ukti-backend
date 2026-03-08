package com.ukti.education.repository;

import com.ukti.education.entity.TaskCompletion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, UUID> {

    List<TaskCompletion> findByUserIdOrderByCompletedAtDesc(UUID userId);

    List<TaskCompletion> findByUserIdAndModuleId(UUID userId, String moduleId);

    List<TaskCompletion> findByUserIdAndModuleIdOrderByCompletedAtDesc(UUID userId, String moduleId);

    Optional<TaskCompletion> findByUserIdAndModuleIdAndTaskId(UUID userId, String moduleId, String taskId);

    boolean existsByUserIdAndModuleIdAndTaskId(UUID userId, String moduleId, String taskId);
}
