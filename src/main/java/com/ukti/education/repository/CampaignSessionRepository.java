package com.ukti.education.repository;

import com.ukti.education.entity.CampaignSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignSessionRepository extends JpaRepository<CampaignSession, UUID> {

    long countByFreeCompletedAtIsNotNull();

    long countByCognitoSubIsNotNull();

    List<CampaignSession> findByCognitoSubOrderByCreatedAtDesc(String cognitoSub);
}
