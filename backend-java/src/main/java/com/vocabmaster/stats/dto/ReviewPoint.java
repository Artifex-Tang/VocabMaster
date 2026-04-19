package com.vocabmaster.stats.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewPoint {

    private LocalDateTime ts;
    private String result;
    private int stageAfter;
}
