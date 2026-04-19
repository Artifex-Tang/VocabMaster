package com.vocabmaster.stats.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LevelOverviewResponse {

    private String levelCode;
    private long totalWords;
    private long notStarted;
    private long learning;
    private long mastered;
    private double masteryRate;
    private List<StageDistribution> stageDistribution;
}
