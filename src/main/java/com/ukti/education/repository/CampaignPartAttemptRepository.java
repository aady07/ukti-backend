package com.ukti.education.repository;

import com.ukti.education.entity.CampaignPartAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignPartAttemptRepository extends JpaRepository<CampaignPartAttempt, UUID> {

    List<CampaignPartAttempt> findBySessionIdOrderByPartIndexAsc(UUID sessionId);

    Optional<CampaignPartAttempt> findBySessionIdAndPassageIdAndPartIndex(
            UUID sessionId, String passageId, int partIndex);

    List<CampaignPartAttempt> findBySessionIdAndPartIndex(UUID sessionId, int partIndex);
}
