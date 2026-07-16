package com.ukti.education.repository;

import com.ukti.education.entity.CampaignSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CampaignSessionRepository extends JpaRepository<CampaignSession, UUID> {
}
