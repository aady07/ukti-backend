package com.ukti.education.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClassLeaderboardResponse {

    @Data
    @Builder
    public static class LeaderboardEntry {
        private int rank;
        private String rollNumber;
        private String name;
        private int completedCount;
    }

    private List<LeaderboardEntry> leaderboard;
}
