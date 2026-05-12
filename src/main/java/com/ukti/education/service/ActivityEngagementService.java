package com.ukti.education.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukti.education.dto.*;
import com.ukti.education.entity.*;
import com.ukti.education.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Ingests engagement batches. Merge rule: {@code counters} are <strong>deltas</strong> added to stored totals
 * (see product contract). Idempotent per {@code batchId} per session and optional {@code Idempotency-Key} header.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityEngagementService {

    private static final int MAX_EVENTS_PER_SESSION = 50;

    /** Spring Boot 4 does not always register an {@code ObjectMapper} bean; keep a local mapper for JSONB snapshots. */
    private static final ObjectMapper EVENT_JSON = new ObjectMapper().findAndRegisterModules();

    private final ActivityEngagementSessionRepository sessionRepository;
    private final ActivityEngagementEventRepository eventRepository;
    private final ActivityEngagementProcessedBatchRepository processedBatchRepository;
    private final ActivityEngagementIdempotencyRepository idempotencyRepository;

    @Transactional
    public EngagementSessionBatchResponse ingestBatch(
            EngagementSubject subject,
            EngagementSessionBatchRequest request,
            String idempotencyKey) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String key = idempotencyKey.trim();
            if (key.length() > 200) {
                throw new IllegalArgumentException("Idempotency-Key must be at most 200 characters");
            }
            Optional<ActivityEngagementIdempotency> cached = idempotencyRepository.findById(key);
            if (cached.isPresent()) {
                ActivityEngagementIdempotency row = cached.get();
                return EngagementSessionBatchResponse.builder()
                        .sessionId(row.getSessionId())
                        .receivedBatchId(row.getReceivedBatchId())
                        .build();
            }
        }

        ActivityEngagementSession session = lockSession(subject, request);

        if (processedBatchRepository.existsBySessionIdAndBatchId(session.getId(), request.getBatchId())) {
            return EngagementSessionBatchResponse.builder()
                    .sessionId(session.getId())
                    .receivedBatchId(request.getBatchId())
                    .build();
        }

        applyCounterDeltas(session, request.getCounters());
        session.setPayloadVersion(Math.max(session.getPayloadVersion(), request.getPayloadVersion()));

        if (session.getRunId() == null && request.getRunId() != null) {
            session.setRunId(request.getRunId());
        }
        if ((session.getSectionId() == null || session.getSectionId().isBlank()) && request.getSectionId() != null && !request.getSectionId().isBlank()) {
            session.setSectionId(request.getSectionId());
        }

        if (Boolean.TRUE.equals(request.getSessionClosed())) {
            Instant end = request.getEndedAt() != null ? request.getEndedAt() : Instant.now();
            if (session.getEndedAt() == null || end.isAfter(session.getEndedAt())) {
                session.setEndedAt(end);
            }
        }

        if (request.getEvents() != null && !request.getEvents().isEmpty()) {
            persistEvents(session.getId(), request.getEvents());
            session.setRawEvents(EVENT_JSON.valueToTree(request.getEvents()));
        }

        sessionRepository.save(session);

        processedBatchRepository.save(ActivityEngagementProcessedBatch.builder()
                .sessionId(session.getId())
                .batchId(request.getBatchId())
                .build());

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String key = idempotencyKey.trim();
            try {
                idempotencyRepository.save(ActivityEngagementIdempotency.builder()
                        .idempotencyKey(key)
                        .sessionId(session.getId())
                        .receivedBatchId(request.getBatchId())
                        .build());
            } catch (DataIntegrityViolationException e) {
                ActivityEngagementIdempotency row = idempotencyRepository.findById(key)
                        .orElseThrow(() -> e);
                return EngagementSessionBatchResponse.builder()
                        .sessionId(row.getSessionId())
                        .receivedBatchId(row.getReceivedBatchId())
                        .build();
            }
        }

        return EngagementSessionBatchResponse.builder()
                .sessionId(session.getId())
                .receivedBatchId(request.getBatchId())
                .build();
    }

    private ActivityEngagementSession lockSession(EngagementSubject subject, EngagementSessionBatchRequest request) {
        UUID clientSessionId = request.getClientSessionId();
        ActivityEngagementSession session = sessionRepository.findByClientSessionIdForUpdate(clientSessionId).orElse(null);
        if (session == null) {
            try {
                sessionRepository.save(buildNewSession(subject, request));
            } catch (DataIntegrityViolationException ex) {
                log.debug("engagement_session_create_race clientSessionId={}", clientSessionId);
            }
            session = sessionRepository.findByClientSessionIdForUpdate(clientSessionId)
                    .orElseThrow(() -> new IllegalStateException("Engagement session missing after create"));
        }

        if (!session.getUserId().equals(subject.userId())) {
            throw new SecurityException("clientSessionId belongs to another user");
        }
        if (!session.getUnitSlug().equals(request.getUnitSlug()) || !session.getActivitySlug().equals(request.getActivitySlug())) {
            throw new IllegalArgumentException("unitSlug/activitySlug do not match existing session");
        }

        return session;
    }

    private ActivityEngagementSession buildNewSession(EngagementSubject subject, EngagementSessionBatchRequest request) {
        Instant started = request.getStartedAt() != null ? request.getStartedAt() : Instant.now();
        return ActivityEngagementSession.builder()
                .clientSessionId(request.getClientSessionId())
                .userId(subject.userId())
                .schoolId(subject.schoolId())
                .classId(subject.classId())
                .rollNumber(subject.rollNumber())
                .runId(request.getRunId())
                .sectionId(request.getSectionId())
                .unitSlug(request.getUnitSlug())
                .activitySlug(request.getActivitySlug())
                .startedAt(started)
                .payloadVersion(Objects.requireNonNullElse(request.getPayloadVersion(), 1))
                .build();
    }

    private void applyCounterDeltas(ActivityEngagementSession session, EngagementCountersDelta d) {
        session.setVideoComplete(addNonNegative(session.getVideoComplete(), d.getVideoComplete()));
        session.setVideoSkip(addNonNegative(session.getVideoSkip(), d.getVideoSkip()));
        session.setVideoReplay(addNonNegative(session.getVideoReplay(), d.getVideoReplay()));
        session.setVideoError(addNonNegative(session.getVideoError(), d.getVideoError()));
        session.setVisionAttempts(addNonNegative(session.getVisionAttempts(), d.getVisionAttempts()));
        session.setVisionPasses(addNonNegative(session.getVisionPasses(), d.getVisionPasses()));
        session.setVisionFailures(addNonNegative(session.getVisionFailures(), d.getVisionFailures()));
        session.setSttListenStarts(addNonNegative(session.getSttListenStarts(), d.getSttListenStarts()));
        session.setPronPass(addNonNegative(session.getPronPass(), d.getPronPass()));
        session.setPronFail(addNonNegative(session.getPronFail(), d.getPronFail()));
        session.setSkipAudio(addNonNegative(session.getSkipAudio(), d.getSkipAudio()));
        session.setSttEmptyCycles(addNonNegative(session.getSttEmptyCycles(), d.getSttEmptyCycles()));
        session.setClientError(addNonNegative(session.getClientError(), d.getClientError()));
    }

    private static int addNonNegative(int total, Integer delta) {
        int d = delta != null ? delta : 0;
        return Math.max(0, total + d);
    }

    private void persistEvents(UUID sessionId, List<EngagementEventPayload> events) {
        long existing = eventRepository.countBySessionId(sessionId);
        int room = (int) Math.max(0, MAX_EVENTS_PER_SESSION - existing);
        if (room == 0) {
            return;
        }
        int n = 0;
        for (EngagementEventPayload e : events) {
            if (n >= room) break;
            if (e == null || e.getType() == null || e.getType().isBlank()) {
                continue;
            }
            Instant ts = e.getTs() != null ? e.getTs() : Instant.now();
            eventRepository.save(ActivityEngagementEvent.builder()
                    .sessionId(sessionId)
                    .ts(ts)
                    .type(e.getType())
                    .phase(e.getPhase())
                    .challengeIndex(e.getChallengeIndex())
                    .payload(e.getPayload())
                    .build());
            n++;
        }
    }
}
