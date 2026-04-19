package com.vocabmaster.test.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 下发给客户端的题目（不含正确答案）。 */
@Data
@Builder
public class TestQuestion {

    private String questionId;
    private Long wordId;
    private QuestionPrompt prompt;

    /** 仅 choice 模式有值，已随机打乱顺序。 */
    private List<String> choices;
}
