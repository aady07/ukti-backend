package com.ukti.education.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroupActivityLeaderboardResponse {

    @Data
    @Builder
    public static class GroupWinCount {
        private String groupNumber;
        private int winCount;
    }

    private List<GroupWinCount> leaderboard;
}
