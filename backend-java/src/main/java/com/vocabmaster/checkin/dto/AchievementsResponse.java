package com.vocabmaster.checkin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AchievementsResponse {

    private List<Unlocked> unlocked;
    private List<Locked> locked;

    @Data
    @Builder
    public static class Unlocked {
        private String code;
        private String nameZh;
        private String icon;
        private LocalDateTime achievedAt;
    }

    @Data
    @Builder
    public static class Locked {
        private String code;
        private String nameZh;
        private String icon;
    }
}
