package com.ukti.education.dto;

import java.util.UUID;

/**
 * Resolved student (or at-home user) for engagement ingest — mirrors progress identity rules.
 */
public record EngagementSubject(UUID userId, UUID schoolId, UUID classId, String rollNumber) {}
