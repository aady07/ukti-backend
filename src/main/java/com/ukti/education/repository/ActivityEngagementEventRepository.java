package com.ukti.education.repository;

import com.ukti.education.entity.ActivityEngagementEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActivityEngagementEventRepository extends JpaRepository<ActivityEngagementEvent, UUID> {

    long countBySessionId(UUID sessionId);
}
