package com.ukti.education.repository;

import com.ukti.education.entity.CurriculumModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CurriculumModuleRepository extends JpaRepository<CurriculumModule, String> {
    List<CurriculumModule> findByClassLevelOrderByModuleIdAsc(String classLevel);
    Optional<CurriculumModule> findByModuleIdAndClassLevel(String moduleId, String classLevel);
}
