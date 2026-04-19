package com.ukti.education.dto;

import java.util.Map;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentActivityMediaItemResponse {

    String unitSlug;
    String activitySlug;
    String completedAt;
    /** Short-lived HTTPS URL to view image, or null */
    String imageUrl;
    /** Short-lived HTTPS URL to play audio, or null */
    String audioUrl;
    /** Subset of stored metadata (excludes raw keys if desired) */
    Map<String, Object> metadata;
}
