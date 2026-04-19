package com.vocabmaster.stats.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TodayStatsResponse {

    private String date;
    private long wordsLearned;
    private long wordsReviewed;
    private long correctCount;
    private double accuracy;
    private long durationSeconds;
    private GoalProgress goalProgress;

    @Data
    @Builder
    public static class GoalProgress {
        private String newWords;   // "18/20"
        private String review;     // "42/100"
    }
}
