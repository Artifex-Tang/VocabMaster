package com.vocabmaster.stats.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SummaryResponse {

    private String period;
    private String startDate;
    private String endDate;
    private int daysActive;
    private long totalLearned;
    private long totalReviewed;
    private double avgAccuracy;
    private List<DailyBreakdown> dailyBreakdown;
    private List<LevelBreakdown> levelBreakdown;
}
