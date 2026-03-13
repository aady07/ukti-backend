package com.ukti.education.repository;

import com.ukti.education.entity.GroupActivitySession;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupActivitySessionRepository extends JpaRepository<GroupActivitySession, UUID> {

    List<GroupActivitySession> findBySchoolIdAndClassIdOrderByCreatedAtDesc(UUID schoolId, UUID classId);
}
