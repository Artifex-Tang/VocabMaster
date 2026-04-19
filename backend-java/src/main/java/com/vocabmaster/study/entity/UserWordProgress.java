package com.vocabmaster.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_word_progress")
public class UserWordProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long wordId;
    private String levelCode;

    /** 0=未学, 1-9=艾宾浩斯阶段 */
    @Builder.Default
    private Integer stage = 0;

    @Builder.Default
    private Integer correctCount = 0;

    @Builder.Default
    private Integer wrongCount = 0;

    private LocalDateTime lastReviewedAt;
    private LocalDateTime nextReviewAt;
    private LocalDateTime firstLearnedAt;
    private LocalDateTime masteredAt;

    /** 客户端时间戳，用于多端同步冲突解决 */
    private LocalDateTime clientUpdatedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
