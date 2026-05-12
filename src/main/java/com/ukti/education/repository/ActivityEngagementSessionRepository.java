package com.ukti.education.repository;

import com.ukti.education.entity.ActivityEngagementSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ActivityEngagementSessionRepository extends JpaRepository<ActivityEngagementSession, UUID> {

    Optional<ActivityEngagementSession> findByClientSessionId(UUID clientSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ActivityEngagementSession s WHERE s.clientSessionId = :clientSessionId")
    Optional<ActivityEngagementSession> findByClientSessionIdForUpdate(@Param("clientSessionId") UUID clientSessionId);
}
