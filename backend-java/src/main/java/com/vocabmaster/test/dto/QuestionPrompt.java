package com.vocabmaster.test.dto;

import lombok.Builder;
import lombok.Data;

/** 展示给用户的题目提示，根据模式字段非空/为 null。 */
@Data
@Builder
public class QuestionPrompt {

    /** spelling / choice 模式展示 */
    private String zhDefinition;

    /** spelling / listening 模式展示 */
    private String audioUrlUk;
    private String audioUrlUs;
}
