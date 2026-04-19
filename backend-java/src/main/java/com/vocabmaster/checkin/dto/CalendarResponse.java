package com.vocabmaster.checkin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CalendarResponse {

    private String month;
    private List<DayEntry> days;
    private int currentStreak;
    private int longestStreak;

    @Data
    @Builder
    public static class DayEntry {
        private String date;
        private boolean checkedIn;
        private int wordsCount;
    }
}
