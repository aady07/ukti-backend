package com.ukti.education.repository;

import com.ukti.education.entity.UserActivityProgress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityProgressRepository extends JpaRepository<UserActivityProgress, UUID> {

    List<UserActivityProgress> findByUserIdOrderByCompletedAtDesc(UUID userId);

    List<UserActivityProgress> findByUserIdAndUnitSlug(UUID userId, String unitSlug);

    Optional<UserActivityProgress> findByUserIdAndUnitSlugAndActivitySlug(UUID userId, String unitSlug, String activitySlug);
}
