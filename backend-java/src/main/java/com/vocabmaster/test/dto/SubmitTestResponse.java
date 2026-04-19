package com.vocabmaster.test.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SubmitTestResponse {

    private String testId;
    private String mode;
    private int totalCount;
    private int correctCount;
    private double accuracy;
    private List<QuestionResult> results;
}
