package com.ukti.education.service;

import com.ukti.education.entity.UserActivityProgress;
import com.ukti.education.repository.UserActivityProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Counts "curriculum activities" completed, not raw progress rows.
 * <p>
 * Slugs ending in {@code -show} / {@code -say} map to a base activity id (suffix stripped).
 * {@code body-movements-*}: say-only (complete when {@code -say} exists, or unsuffixed row).
 * Other activities: dual (complete when both show and say exist, or unsuffixed row counts as full).
 */
@Service
@RequiredArgsConstructor
public class ActivityLogicalCompletionService {

    private final UserActivityProgressRepository progressRepository;

    public int countLogicalCompletedActivities(UUID userId) {
        List<UserActivityProgress> rows = progressRepository.findByUserIdOrderByCompletedAtDesc(userId);
        return countLogicalCompletedActivities(rows);
    }

    int countLogicalCompletedActivities(List<UserActivityProgress> rows) {
        Map<String, StepState> byUnitAndBase = new HashMap<>();
        for (UserActivityProgress p : rows) {
            String unit = p.getUnitSlug();
            String slug = p.getActivitySlug();
            if (slug == null || slug.isBlank()) continue;
            String base = parseBaseSlug(slug);
            String key = unit + "\0" + base;
            StepState st = byUnitAndBase.computeIfAbsent(key, k -> new StepState());
            if (slug.endsWith("-show")) st.show = true;
            else if (slug.endsWith("-say")) st.say = true;
            else st.full = true;
        }

        int count = 0;
        for (Map.Entry<String, StepState> e : byUnitAndBase.entrySet()) {
            String key = e.getKey();
            int sep = key.indexOf('\0');
            String base = sep >= 0 ? key.substring(sep + 1) : key;
            StepState st = e.getValue();
            if (isSayOnlyBase(base)) {
                if (st.full || st.say) count++;
            } else {
                if (st.full || (st.show && st.say)) count++;
            }
        }
        return count;
    }

    /**
     * Base slug after stripping -show / -say, e.g. {@code body-movements-1}.
     */
    static String parseBaseSlug(String activitySlug) {
        if (activitySlug.endsWith("-show")) {
            return activitySlug.substring(0, activitySlug.length() - "-show".length());
        }
        if (activitySlug.endsWith("-say")) {
            return activitySlug.substring(0, activitySlug.length() - "-say".length());
        }
        return activitySlug;
    }

    /**
     * Body movement: video then mic only — progress keys are say-style only.
     */
    static boolean isSayOnlyBase(String baseSlug) {
        return baseSlug.startsWith("body-movements-");
    }

    private static final class StepState {
        boolean show;
        boolean say;
        /** Unsuffixed slug — treat as fully complete for that activity */
        boolean full;
    }
}
