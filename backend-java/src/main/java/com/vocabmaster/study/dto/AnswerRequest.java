package com.vocabmaster.study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnswerRequest {

    @NotNull
    private Long wordId;

    @NotBlank
    private String levelCode;

    /** correct / wrong / skip */
    @NotBlank
    private String result;

    /** card / spelling / choice / listening */
    private String mode = "card";

    private Integer durationMs;

    /** 客户端时间戳（ISO 8601），用于多端同步冲突解决 */
    private LocalDateTime clientTs;
}
