package com.vocabmaster.wordlist.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WordListSummaryDto {
    private Long id;
    private String name;
    private String description;
    private String sourceType;
    private String originLevelCode;
    private Integer wordCount;
    private String coverEmoji;
    private Boolean subscribed;
}
