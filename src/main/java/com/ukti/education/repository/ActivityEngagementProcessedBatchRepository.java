package com.ukti.education.repository;

import com.ukti.education.entity.ActivityEngagementProcessedBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActivityEngagementProcessedBatchRepository extends JpaRepository<ActivityEngagementProcessedBatch, UUID> {

    boolean existsBySessionIdAndBatchId(UUID sessionId, UUID batchId);
}
