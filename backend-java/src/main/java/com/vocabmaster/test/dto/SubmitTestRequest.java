package com.vocabmaster.test.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SubmitTestRequest {

    @NotBlank
    private String testId;

    @NotEmpty
    @Valid
    private List<TestAnswerItem> answers;
}
