package com.vocabmaster.study.dto;

import com.vocabmaster.word.dto.WordDetailDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TodayPlanResponse {

    private String date;
    private List<WordDetailDto> reviewWords;
    private List<WordDetailDto> newWords;
    private int reviewCount;
    private int newCount;
    private int estimatedMinutes;
}
