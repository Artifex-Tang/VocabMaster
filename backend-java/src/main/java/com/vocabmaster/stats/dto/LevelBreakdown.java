package com.vocabmaster.stats.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LevelBreakdown {

    private String levelCode;
    private long mastered;
    private long learning;
}
