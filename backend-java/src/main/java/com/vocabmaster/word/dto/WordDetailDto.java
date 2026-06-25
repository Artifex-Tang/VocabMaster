package com.vocabmaster.word.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.vocabmaster.common.jackson.RawJsonStringDeserializer;
import com.vocabmaster.word.entity.WordBank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 词详情 DTO。
 *
 * <p>会被缓存进 Redis（如 TodayPlanResponse），经 {@code Jackson2JsonRedisSerializer<Object>}
 * 无类型信息存取 + {@code convertValue} 还原，故须有无参构造器（{@code @NoArgsConstructor}）+ setter
 * （{@code @Data} 提供）；{@code related_words} 因 {@code @JsonRawValue} 序列化为原始对象，
 * 反序列化需 {@link RawJsonStringDeserializer} 才能从对象/数组还原回字符串。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WordDetailDto {

    private Long id;
    private String levelCode;
    private String word;
    private String ipaUk;
    private String ipaUs;
    private String enDefinition;
    private String zhDefinition;
    private String exampleEn;
    private String exampleZh;
    private String topicCode;
    private String audioUrlUk;
    private String audioUrlUs;
    private String imageUrl;
    private String emoji;
    private String pos;
    private Integer difficulty;
    private BigDecimal frequency;

    /** 直接输出原始 JSON 字符串，避免二次序列化；反序列化用 RawJsonStringDeserializer 兼容 Redis 往返 */
    @JsonRawValue
    @JsonDeserialize(using = RawJsonStringDeserializer.class)
    private String relatedWords;

    public static WordDetailDto from(WordBank wb) {
        return WordDetailDto.builder()
                .id(wb.getId())
                .levelCode(wb.getLevelCode())
                .word(wb.getWord())
                .ipaUk(wb.getIpaUk())
                .ipaUs(wb.getIpaUs())
                .enDefinition(wb.getEnDefinition())
                .zhDefinition(wb.getZhDefinition())
                .exampleEn(wb.getExampleEn())
                .exampleZh(wb.getExampleZh())
                .topicCode(wb.getTopicCode())
                .audioUrlUk(wb.getAudioUrlUk())
                .audioUrlUs(wb.getAudioUrlUs())
                .imageUrl(wb.getImageUrl())
                .emoji(wb.getEmoji())
                .pos(wb.getPos())
                .difficulty(wb.getDifficulty())
                .frequency(wb.getFrequency())
                .relatedWords(wb.getRelatedWords())
                .build();
    }
}
