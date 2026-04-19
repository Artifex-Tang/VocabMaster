package com.vocabmaster.user.dto;

import lombok.Data;

@Data
public class UpdateSettingsRequest {

    private Integer dailyNewWordsGoal;
    private Integer dailyReviewGoal;
    private String defaultSortMode;
    private String preferredAccent;
    private Boolean autoPlayAudio;
    private String notificationTime;
    private String theme;
    private String activeLevels;
}
