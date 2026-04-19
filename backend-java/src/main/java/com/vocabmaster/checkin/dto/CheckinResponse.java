package com.vocabmaster.checkin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CheckinResponse {

    private String date;
    private int currentStreak;
    private int longestStreak;
    private int totalDays;
    private List<NewAchievement> newAchievements;

    @Data
    @Builder
    public static class NewAchievement {
        private String code;
        private String nameZh;
    }
}
