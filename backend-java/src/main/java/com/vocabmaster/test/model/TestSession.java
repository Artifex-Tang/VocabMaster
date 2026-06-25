package com.vocabmaster.test.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 存储在 Redis 的测试会话（含正确答案，不下发给客户端）。
 *
 * <p>Redis 用 {@code Jackson2JsonRedisSerializer<Object>}（不带类型信息）存取，
 * 读回时是 {@code LinkedHashMap}，需经 {@code ObjectMapper.convertValue} 还原。
 * 因此必须有可被 Jackson 构造的无参构造器（{@code @NoArgsConstructor}）+ setter
 * （{@code @Data} 提供），否则 {@code convertValue} 抛
 * {@code InvalidDefinitionException: no Creators} → /test/submit 500。
 * {@code @Builder} 单用只生成全参构造器，故显式补无参 + 全参。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSession implements Serializable {

    private String testId;
    private Long userId;
    private String levelCode;
    private String mode;
    private List<SessionQuestion> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionQuestion implements Serializable {
        private String questionId;
        private Long wordId;
        /** 标准答案（小写），评分时做 equalsIgnoreCase 比较。 */
        private String correctAnswer;
    }
}
