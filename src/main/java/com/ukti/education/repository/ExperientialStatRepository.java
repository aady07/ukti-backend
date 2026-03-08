package com.ukti.education.repository;

import com.ukti.education.entity.ExperientialStat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperientialStatRepository extends JpaRepository<ExperientialStat, UUID> {

    List<ExperientialStat> findByUserId(UUID userId);

    Optional<ExperientialStat> findByUserIdAndModuleIdAndStatType(UUID userId, String moduleId, String statType);

    List<ExperientialStat> findByUserIdAndModuleId(UUID userId, String moduleId);
}
