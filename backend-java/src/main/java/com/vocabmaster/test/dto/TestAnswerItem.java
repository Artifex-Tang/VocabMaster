package com.vocabmaster.test.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestAnswerItem {

    @NotBlank
    private String questionId;

    /** 用户作答（拼写/听写填写单词，选择填选中的单词） */
    private String answer;

    private Integer durationMs;
}
