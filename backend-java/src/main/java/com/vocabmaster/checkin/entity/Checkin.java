package com.vocabmaster.checkin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("checkin")
public class Checkin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private LocalDate checkinDate;

    @Builder.Default
    private Integer wordsLearned = 0;

    @Builder.Default
    private Integer wordsReviewed = 0;

    @Builder.Default
    private Integer correctCount = 0;

    @Builder.Default
    private Integer durationSeconds = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
