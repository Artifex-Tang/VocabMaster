package com.vocabmaster.wrongword.dto;

import com.vocabmaster.study.entity.WrongWord;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WrongWordDto {

    private Long id;
    private Long wordId;
    private String levelCode;
    private int wrongCount;
    private LocalDateTime lastWrongAt;
    private int resolved;

    public static WrongWordDto from(WrongWord ww) {
        return WrongWordDto.builder()
                .id(ww.getId())
                .wordId(ww.getWordId())
                .levelCode(ww.getLevelCode())
                .wrongCount(ww.getWrongCount())
                .lastWrongAt(ww.getLastWrongAt())
                .resolved(ww.getResolved())
                .build();
    }
}
