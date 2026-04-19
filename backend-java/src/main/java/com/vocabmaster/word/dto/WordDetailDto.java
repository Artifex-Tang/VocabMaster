package com.vocabmaster.word.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.vocabmaster.word.entity.WordBank;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
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

    /** 直接输出原始 JSON 字符串，避免二次序列化 */
    @JsonRawValue
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
