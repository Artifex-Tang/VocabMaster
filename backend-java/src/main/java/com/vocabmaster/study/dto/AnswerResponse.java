package com.vocabmaster.study.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AnswerResponse {

    private Long wordId;
    private Integer stageBefore;
    private Integer stageAfter;
    private LocalDateTime nextReviewAt;
    private boolean mastered;
}
