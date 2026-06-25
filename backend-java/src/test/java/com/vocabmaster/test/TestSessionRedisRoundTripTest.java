package com.vocabmaster.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.vocabmaster.test.model.TestSession;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 复现 /test/submit 500 的根因：Redis 值序列化器为
 * {@code Jackson2JsonRedisSerializer<Object>}（不带类型信息），TestSession 存进再读回时
 * 变成 {@code LinkedHashMap}，{@code TestService.loadSession} 用
 * {@code ObjectMapper.convertValue} 还原。该路径要求 TestSession 可被 Jackson 构造
 * （无参构造器 + setter），否则抛 {@code InvalidDefinitionException} → 500。
 */
class TestSessionRedisRoundTripTest {

    @Test
    void redisRoundTrip_thenConvertValue_restoresSessionWithNestedQuestions() {
        // 与生产一致：全局 @Primary ObjectMapper = SNAKE_CASE（JacksonConfig），
        // 同一 bean 注入 RedisConfig 的 Jackson2JsonRedisSerializer。
        ObjectMapper snake = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(snake, Object.class);

        TestSession original = TestSession.builder()
                .testId("tst_abc")
                .userId(7L)
                .levelCode("CET4")
                .mode("choice")
                .questions(List.of(
                        TestSession.SessionQuestion.builder()
                                .questionId("q1").wordId(101L).correctAnswer("apple").build(),
                        TestSession.SessionQuestion.builder()
                                .questionId("q2").wordId(202L).correctAnswer("banana").build()))
                .build();

        // 模拟 Redis 存取：写 JSON → 读回 Object（无类型信息 → LinkedHashMap）
        byte[] payload = serializer.serialize(original);
        Object raw = serializer.deserialize(payload);

        // loadSession 的还原路径：convertValue(LinkedHashMap, TestSession.class)
        TestSession restored = snake.convertValue(raw, TestSession.class);

        assertNotNull(restored);
        assertEquals("tst_abc", restored.getTestId());
        assertEquals(7L, restored.getUserId());
        assertNotNull(restored.getQuestions());
        assertEquals(2, restored.getQuestions().size());
        assertEquals("q1", restored.getQuestions().get(0).getQuestionId());
        assertEquals("apple", restored.getQuestions().get(0).getCorrectAnswer());
        assertEquals(202L, restored.getQuestions().get(1).getWordId());
    }
}
