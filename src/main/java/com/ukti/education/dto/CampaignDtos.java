package com.ukti.education.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CampaignDtos {

    private CampaignDtos() {}

    @Data
    public static class StartSessionRequest {
        private String email;
        private String utmSource;
        private String utmMedium;
        private String utmCampaign;
        private String packId;
    }

    @Data
    @Builder
    public static class SessionResponse {
        private UUID id;
        private String email;
        private String packId;
        private Instant startedAt;
        private Instant freeCompletedAt;
        private String cognitoSub;
    }

    @Data
    public static class EventItem {
        @NotBlank
        private String eventType;
        private String path;
        private String payloadJson;
        private Instant clientTs;
    }

    @Data
    public static class EventsBatchRequest {
        @NotNull
        private List<EventItem> events;
    }

    @Data
    public static class PartAttemptRequest {
        private String packId;
        @NotBlank
        private String passageId;
        @NotNull
        private Integer partIndex;
        private String expectedText;
        private String spokenTranscript;
        private Long durationMs;
        private Integer pauseCount;
        private Integer wrongCount;
        private Integer skipCount;
        private Integer wordsTotal;
        private Integer wordsMatched;
        private Integer accuracyPct;
        private Boolean freePackComplete;
    }

    @Data
    @Builder
    public static class PartAttemptResponse {
        private UUID id;
        private UUID sessionId;
        private String packId;
        private String passageId;
        private int partIndex;
        private String status;
        private String geminiScoreJson;
        private Integer accuracyPct;
        private Long durationMs;
        private Instant createdAt;
    }

    @Data
    @Builder
    public static class MeAttemptItem {
        private UUID attemptId;
        private UUID sessionId;
        private String packId;
        private String passageId;
        private int partIndex;
        private Integer accuracyPct;
        private Long durationMs;
        private String status;
        private String geminiScoreJson;
        private Instant createdAt;
    }

    @Data
    @Builder
    public static class MePackSummary {
        private String packId;
        /** Distinct reading sessions for this pack. */
        private long attemptCount;
        private MeAttemptItem latest;
    }

    @Data
    @Builder
    public static class MeAttemptsResponse {
        private List<MeAttemptItem> attempts;
        private List<MePackSummary> packs;
    }

    @Data
    public static class PartScoreRequest {
        @NotBlank
        private String geminiScoreJson;
        private String status;
    }

    @Data
    public static class AuthLinkRequest {
        private UUID sessionId;
        private String displayName;
    }

    @Data
    @Builder
    public static class LeadResponse {
        private UUID id;
        private String email;
        private String cognitoSub;
        private String displayName;
        private String status;
        private UUID sessionId;
        private Instant createdAt;
    }

    @Data
    @Builder
    public static class AdminSummaryResponse {
        private long opens;
        private long startedRead;
        private long spoke;
        private long freeCompleted;
        private long signedUp;
        private long loggedIn;
        private Double avgGeminiScore;
        private Map<String, Long> eventCounts;
        private long totalSessions;
        private long totalLeads;
    }

    @Data
    @Builder
    public static class SessionDetailResponse {
        private SessionResponse session;
        private List<PartAttemptResponse> parts;
        private List<EventItem> events;
        private LeadResponse lead;
    }

    @Data
    @Builder
    public static class SchoolOverviewItem {
        private UUID id;
        private String name;
        private long classCount;
        private long studentCount;
        private long teacherCount;
        private long adminCount;
        private Instant createdAt;
    }

    @Data
    @Builder
    public static class SchoolPersonItem {
        private UUID id;
        private String name;
        private String email;
        private String rollNumber;
        private String userType;
    }

    @Data
    @Builder
    public static class SchoolClassDetail {
        private UUID id;
        private String name;
        private long studentCount;
        private List<SchoolPersonItem> students;
        private List<SchoolPersonItem> teachers;
    }

    @Data
    @Builder
    public static class SchoolDetailResponse {
        private UUID id;
        private String name;
        private Instant createdAt;
        private List<SchoolPersonItem> admins;
        private List<SchoolPersonItem> teachers;
        private List<SchoolClassDetail> classes;
        private long studentCount;
        private long teacherCount;
        private long classCount;
        private long adminCount;
    }
}
