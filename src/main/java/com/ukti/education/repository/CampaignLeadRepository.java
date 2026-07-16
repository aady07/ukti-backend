package com.ukti.education.repository;

import com.ukti.education.entity.CampaignLead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignLeadRepository extends JpaRepository<CampaignLead, UUID> {

    Optional<CampaignLead> findByCognitoSub(String cognitoSub);

    Optional<CampaignLead> findByEmailIgnoreCase(String email);

    List<CampaignLead> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
