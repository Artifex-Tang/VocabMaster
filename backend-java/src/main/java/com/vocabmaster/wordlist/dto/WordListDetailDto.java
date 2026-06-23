package com.vocabmaster.wordlist.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WordListDetailDto {
    private Long id;
    private String name;
    private String description;
    private String originLevelCode;
    private Integer wordCount;
    private Integer unitCount;
    private Boolean subscribed;
    private Integer currentUnitNo;
    private List<UnitSummaryDto> units;
}
