package com.vocabmaster.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.vocabmaster.study.dto.TodayPlanResponse;
import com.vocabmaster.word.dto.WordDetailDto;
import com.vocabmaster.word.entity.Level;
import com.vocabmaster.word.entity.WordTopic;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证经 Redis（{@code Jackson2JsonRedisSerializer<Object>} 无类型信息）存取 +
 * {@code convertValue} 还原的几个缓存对象能正确往返。与生产 RedisConfig/JacksonConfig 一致：
 * 全局 ObjectMapper = SNAKE_CASE @Primary，注入到 Redis 序列化器与各 Service。
 *
 * <p>覆盖此前两类同源 bug：
 * <ul>
 *   <li>{@code LevelService} 直接强转 {@code List<LinkedHashMap>} 为 {@code List<Level>} → 现用 convertValue。</li>
 *   <li>{@code TodayPlanService} 的 {@code instanceof TodayPlanResponse} 恒 false → 现用 convertValue；
 *   且 {@code WordDetailDto.relatedWords} 带 {@code @JsonRawValue}，须 {@link
 *   com.vocabmaster.common.jackson.RawJsonStringDeserializer} 才能从对象还原回字符串。</li>
 * </ul>
 */
class RedisCacheRoundTripTest {

    private final ObjectMapper snake = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final Jackson2JsonRedisSerializer<Object> serializer =
            new Jackson2JsonRedisSerializer<>(snake, Object.class);

    private Object roundTrip(Object original) {
        return serializer.deserialize(serializer.serialize(original));
    }

    @Test
    void levelList_roundTrip_restoresTypedLevels() {
        List<Level> original = List.of(
                Level.builder().code("CET4").nameZh("大学四级").sortOrder(1).build(),
                Level.builder().code("CET6").nameZh("大学六级").sortOrder(2).build());

        Object raw = roundTrip(original);
        List<Level> restored = snake.convertValue(raw, new com.fasterxml.jackson.core.type.TypeReference<List<Level>>() {});

        assertEquals(2, restored.size());
        assertEquals("CET4", restored.get(0).getCode());
        assertEquals("大学四级", restored.get(0).getNameZh());
        assertEquals(2, restored.get(1).getSortOrder());
    }

    @Test
    void topicList_roundTrip_restoresTypedTopics() {
        List<WordTopic> original = List.of(
                WordTopic.builder().code("animal").nameZh("动物").icon("🐱").imageType("icon").build());

        Object raw = roundTrip(original);
        List<WordTopic> restored = snake.convertValue(raw, new com.fasterxml.jackson.core.type.TypeReference<List<WordTopic>>() {});

        assertEquals(1, restored.size());
        assertEquals("animal", restored.get(0).getCode());
        assertEquals("icon", restored.get(0).getImageType());
    }

    @Test
    void todayPlan_roundTrip_restoresNestedWordsAndRawRelatedJson() {
        // relatedWords 是原始 JSON 字符串（来自 DB），@JsonRawValue 序列化时作为对象嵌入
        String relatedJson = "{\"derived\":[],\"antonyms\":[\"closed\"],\"synonyms\":[\"open\"]}";
        WordDetailDto word = WordDetailDto.builder()
                .id(10L).word("open").levelCode("CET4")
                .frequency(new BigDecimal("120.5"))
                .relatedWords(relatedJson)
                .build();
        TodayPlanResponse original = TodayPlanResponse.builder()
                .date("2026-06-24")
                .reviewCount(1).newCount(0).estimatedMinutes(1)
                .reviewWords(List.of(word))
                .newWords(List.of())
                .build();

        Object raw = roundTrip(original);
        TodayPlanResponse restored = snake.convertValue(raw, TodayPlanResponse.class);

        assertEquals("2026-06-24", restored.getDate());
        assertNotNull(restored.getReviewWords());
        assertEquals(1, restored.getReviewWords().size());
        WordDetailDto w = restored.getReviewWords().get(0);
        assertEquals("open", w.getWord());
        assertEquals(new BigDecimal("120.5"), w.getFrequency());
        // 关键：@JsonRawValue 存为对象，须靠 RawJsonStringDeserializer 还原回原始 JSON 字符串
        assertEquals(relatedJson, w.getRelatedWords());
    }

    @Test
    void wordDetail_nullRelatedWords_roundTripsToNull() {
        WordDetailDto word = WordDetailDto.builder().id(1L).word("cat").relatedWords(null).build();
        Object raw = roundTrip(List.of(word));
        List<WordDetailDto> restored = snake.convertValue(raw, new com.fasterxml.jackson.core.type.TypeReference<List<WordDetailDto>>() {});
        assertEquals("cat", restored.get(0).getWord());
        assertNull(restored.get(0).getRelatedWords());
    }
}
