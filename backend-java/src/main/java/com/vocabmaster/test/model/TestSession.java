package com.vocabmaster.test.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 存储在 Redis 的测试会话（含正确答案，不下发给客户端）。 */
@Data
@Builder
public class TestSession implements Serializable {

    private String testId;
    private Long userId;
    private String levelCode;
    private String mode;
    private List<SessionQuestion> questions;

    @Data
    @Builder
    public static class SessionQuestion implements Serializable {
        private String questionId;
        private Long wordId;
        /** 标准答案（小写），评分时做 equalsIgnoreCase 比较。 */
        private String correctAnswer;
    }
}
