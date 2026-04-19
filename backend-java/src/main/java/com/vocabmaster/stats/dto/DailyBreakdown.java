package com.vocabmaster.stats.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyBreakdown {

    private String date;
    private long wordsLearned;
    private long wordsReviewed;
    private double accuracy;
}
