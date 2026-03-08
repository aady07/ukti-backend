package com.ukti.education.service;

import com.ukti.education.dto.*;
import com.ukti.education.entity.User;
import com.ukti.education.entity.UserActivityProgress;
import com.ukti.education.repository.UserActivityProgressRepository;
import com.ukti.education.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityProgressService {

    private final UserActivityProgressRepository progressRepository;
    private final UserRepository userRepository;

    public List<UnitProgressResponse> getAllProgress(UUID userId) {
        List<UserActivityProgress> all = progressRepository.findByUserIdOrderByCompletedAtDesc(userId);
        return all.stream()
                .collect(Collectors.groupingBy(UserActivityProgress::getUnitSlug))
                .entrySet().stream()
                .map(e -> buildProgressResponse(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public Optional<UnitProgressResponse> getUnitProgress(UUID userId, String unitSlug) {
        List<UserActivityProgress> list = progressRepository.findByUserIdAndUnitSlug(userId, unitSlug);
        return Optional.of(buildProgressResponse(unitSlug, list));
    }

    @Transactional
    public Optional<ActivityCompleteResponse> completeActivity(
            UUID userId,
            String unitSlug,
            String activitySlug,
            Map<String, Object> metadata) {

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return Optional.empty();

        Optional<UserActivityProgress> existing = progressRepository.findByUserIdAndUnitSlugAndActivitySlug(userId, unitSlug, activitySlug);
        UserActivityProgress up;
        if (existing.isPresent()) {
            up = existing.get();
            if (metadata != null) up.setMetadata(metadata);
            up = progressRepository.save(up);
            log.info("UserActivityProgressService: Activity updated userId={}, unitSlug={}, activitySlug={}", userId, unitSlug, activitySlug);
        } else {
            up = UserActivityProgress.builder()
                    .user(userOpt.get())
                    .unitSlug(unitSlug)
                    .activitySlug(activitySlug)
                    .completedAt(Instant.now())
                    .metadata(metadata)
                    .build();
            up = progressRepository.save(up);
            log.info("UserActivityProgressService: Activity completed userId={}, unitSlug={}, activitySlug={}", userId, unitSlug, activitySlug);
        }

        return Optional.of(ActivityCompleteResponse.builder()
                .activityId(activitySlug)
                .completedAt(up.getCompletedAt())
                .metadata(up.getMetadata())
                .build());
    }

    private UnitProgressResponse buildProgressResponse(String unitSlug, List<UserActivityProgress> list) {
        List<UnitProgressResponse.CompletedActivityItem> completed = list.stream()
                .map(up -> UnitProgressResponse.CompletedActivityItem.builder()
                        .activityId(up.getActivitySlug())
                        .taskId(null)
                        .completedAt(up.getCompletedAt().toString())
                        .metadata(up.getMetadata())
                        .build())
                .collect(Collectors.toList());

        return UnitProgressResponse.builder()
                .unitId(unitSlug)
                .completedActivities(completed)
                .completedCount(completed.size())
                .build();
    }
}
