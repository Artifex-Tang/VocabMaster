package com.vocabmaster.checkin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_achievement")
public class UserAchievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String achievementCode;

    /** 由 Service 层显式设置，或依赖 MySQL DEFAULT CURRENT_TIMESTAMP */
    @TableField(insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NOT_NULL)
    private LocalDateTime achievedAt;
}
