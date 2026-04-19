package com.vocabmaster.word.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vocabmaster.word.entity.WordBank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WordSummaryDto {

    private Long id;
    private String levelCode;
    private String word;
    private String ipaUk;
    private String zhDefinition;
    private String pos;
    private String emoji;
    private String topicCode;

    public static WordSummaryDto from(WordBank wb) {
        return WordSummaryDto.builder()
                .id(wb.getId())
                .levelCode(wb.getLevelCode())
                .word(wb.getWord())
                .ipaUk(wb.getIpaUk())
                .zhDefinition(wb.getZhDefinition())
                .pos(wb.getPos())
                .emoji(wb.getEmoji())
                .topicCode(wb.getTopicCode())
                .build();
    }
}
