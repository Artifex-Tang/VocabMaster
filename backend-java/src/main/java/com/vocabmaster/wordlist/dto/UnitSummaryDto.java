package com.vocabmaster.wordlist.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnitSummaryDto {
    private Integer unitNo;
    private Integer totalCount;
    private Integer learnedCount;
    private Integer masteredCount;
    private Boolean isCurrent;
    private Boolean completed;
}
