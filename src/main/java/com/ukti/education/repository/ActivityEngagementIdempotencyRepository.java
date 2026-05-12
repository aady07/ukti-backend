package com.ukti.education.repository;

import com.ukti.education.entity.ActivityEngagementIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityEngagementIdempotencyRepository extends JpaRepository<ActivityEngagementIdempotency, String> {}
