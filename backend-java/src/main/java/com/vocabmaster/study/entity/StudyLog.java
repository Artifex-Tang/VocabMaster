package com.vocabmaster.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("study_log")
public class StudyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long wordId;
    private String levelCode;

    /** learn / review / test */
    private String action;

    /** correct / wrong / skip */
    private String result;

    /** card / spelling / choice / listening */
    private String mode;

    private Integer stageBefore;
    private Integer stageAfter;
    private Integer durationMs;
    private LocalDateTime clientTs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
