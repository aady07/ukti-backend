package com.ukti.education.repository;

import com.ukti.education.entity.CampaignEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CampaignEventRepository extends JpaRepository<CampaignEvent, UUID> {

    List<CampaignEvent> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    long countByEventType(String eventType);

    @Query("select e.eventType, count(e) from CampaignEvent e group by e.eventType")
    List<Object[]> countGroupedByEventType();

    /** Funnel-safe: one session counts once even if the event fired many times. */
    @Query("select count(distinct e.sessionId) from CampaignEvent e where e.eventType = :eventType")
    long countDistinctSessionsByEventType(@Param("eventType") String eventType);

    @Query("select count(distinct e.sessionId) from CampaignEvent e where e.eventType in :types")
    long countDistinctSessionsByEventTypes(@Param("types") Collection<String> types);
}
