package com.vocabmaster.stats.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StageDistribution {

    private int stage;
    private long count;
}
