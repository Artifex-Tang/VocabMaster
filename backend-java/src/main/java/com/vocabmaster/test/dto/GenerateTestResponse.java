package com.vocabmaster.test.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GenerateTestResponse {

    private String testId;
    private String mode;
    private String levelCode;
    private List<TestQuestion> questions;
}
