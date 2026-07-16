package com.ukti.education.repository;

import com.ukti.education.entity.CampaignEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CampaignEventRepository extends JpaRepository<CampaignEvent, UUID> {

    List<CampaignEvent> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    long countByEventType(String eventType);

    @Query("select e.eventType, count(e) from CampaignEvent e group by e.eventType")
    List<Object[]> countGroupedByEventType();
}
