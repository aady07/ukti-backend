package com.ukti.education.service;

import com.ukti.education.dto.GroupActivityCompleteRequest;
import com.ukti.education.dto.GroupActivityCompleteResponse;
import com.ukti.education.dto.GroupActivityHistoryResponse;
import com.ukti.education.dto.GroupActivityLeaderboardResponse;
import com.ukti.education.entity.GroupActivitySession;
import com.ukti.education.repository.GroupActivitySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupActivityService {

    private final GroupActivitySessionRepository repository;
    private final SchoolService schoolService;

    public GroupActivityCompleteResponse complete(GroupActivityCompleteRequest request, UUID schoolId, UUID classId,
                                                  UUID createdByUserId, UUID createdByTeacherId) {
        GroupActivitySession session = GroupActivitySession.builder()
                .schoolId(schoolId)
                .classId(classId)
                .activityId(request.getActivityId())
                .groups(request.getGroups())
                .scores(request.getScores())
                .winnerGroup(request.getWinnerGroup())
                .sessionId(request.getSessionId())
                .createdByUserId(createdByUserId)
                .createdByTeacherId(createdByTeacherId)
                .build();
        session = repository.save(session);

        return GroupActivityCompleteResponse.builder()
                .id(session.getId())
                .activityId(session.getActivityId())
                .schoolId(session.getSchoolId().toString())
                .classId(session.getClassId().toString())
                .winnerGroup(session.getWinnerGroup())
                .createdAt(session.getCreatedAt())
                .build();
    }

    public GroupActivityHistoryResponse getHistory(UUID schoolId, UUID classId) {
        List<GroupActivitySession> sessions = repository.findBySchoolIdAndClassIdOrderByCreatedAtDesc(schoolId, classId);
        List<GroupActivityHistoryResponse.SessionSummary> summaries = sessions.stream()
                .map(s -> GroupActivityHistoryResponse.SessionSummary.builder()
                        .id(s.getId())
                        .activityId(s.getActivityId())
                        .groups(s.getGroups())
                        .scores(s.getScores())
                        .winnerGroup(s.getWinnerGroup())
                        .createdAt(s.getCreatedAt())
                        .build())
                .toList();

        return GroupActivityHistoryResponse.builder().sessions(summaries).build();
    }

    public GroupActivityLeaderboardResponse getLeaderboard(UUID schoolId, UUID classId) {
        List<GroupActivitySession> sessions = repository.findBySchoolIdAndClassIdOrderByCreatedAtDesc(schoolId, classId);
        Map<Integer, Long> winsByGroup = sessions.stream()
                .collect(Collectors.groupingBy(GroupActivitySession::getWinnerGroup, Collectors.counting()));

        List<GroupActivityLeaderboardResponse.GroupWinCount> leaderboard = winsByGroup.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> GroupActivityLeaderboardResponse.GroupWinCount.builder()
                        .groupNumber(String.valueOf(e.getKey()))
                        .winCount(e.getValue().intValue())
                        .build())
                .toList();

        return GroupActivityLeaderboardResponse.builder().leaderboard(leaderboard).build();
    }
}
