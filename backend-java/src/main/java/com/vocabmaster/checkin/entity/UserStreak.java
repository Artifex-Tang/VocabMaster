package com.vocabmaster.checkin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_streak")
public class UserStreak {

    @TableId(type = IdType.INPUT)
    private Long userId;

    @Builder.Default
    private Integer currentStreak = 0;

    @Builder.Default
    private Integer longestStreak = 0;

    private LocalDate lastCheckinDate;

    @Builder.Default
    private Integer totalDays = 0;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
