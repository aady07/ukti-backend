package com.ukti.education.repository;

import com.ukti.education.entity.UserActivityProgress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserActivityProgressRepository extends JpaRepository<UserActivityProgress, UUID> {

    @Query("SELECT p FROM UserActivityProgress p JOIN p.user u WHERE u.schoolUuid = :schoolId AND u.classId = :classId AND u.userType = 'student' ORDER BY p.completedAt DESC")
    List<UserActivityProgress> findLatestBySchoolAndClass(@Param("schoolId") UUID schoolId, @Param("classId") UUID classId, Pageable pageable);

    List<UserActivityProgress> findByUserIdOrderByCompletedAtDesc(UUID userId);

    @Query("SELECT COUNT(u) FROM UserActivityProgress u WHERE u.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    List<UserActivityProgress> findByUserIdAndUnitSlug(UUID userId, String unitSlug);

    Optional<UserActivityProgress> findByUserIdAndUnitSlugAndActivitySlug(UUID userId, String unitSlug, String activitySlug);
}
