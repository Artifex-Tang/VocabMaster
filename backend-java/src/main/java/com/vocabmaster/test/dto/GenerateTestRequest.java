package com.vocabmaster.test.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateTestRequest {

    @NotBlank
    private String levelCode;

    /** spelling / choice / listening */
    @NotBlank
    private String mode;

    /** 题目数量，1-50 */
    @Min(1) @Max(50)
    private int size = 20;

    /** due / all / wrong_words */
    private String source = "due";
}
