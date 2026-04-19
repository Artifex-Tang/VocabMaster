package com.vocabmaster.test.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionResult {

    private String questionId;
    private Long wordId;
    private boolean correct;
    private String userAnswer;
    private String correctAnswer;
}
