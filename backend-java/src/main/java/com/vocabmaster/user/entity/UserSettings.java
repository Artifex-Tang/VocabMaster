package com.vocabmaster.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_settings")
public class UserSettings {

    /** 与 user.id 对应，无自增 */
    @TableId(type = IdType.INPUT)
    private Long userId;

    @Builder.Default
    private Integer dailyNewWordsGoal = 20;

    @Builder.Default
    private Integer dailyReviewGoal = 100;

    @Builder.Default
    private String defaultSortMode = "alpha";

    @Builder.Default
    private String preferredAccent = "uk";

    @Builder.Default
    private Integer autoPlayAudio = 1;

    @Builder.Default
    private LocalTime notificationTime = LocalTime.of(20, 0);

    @Builder.Default
    private String theme = "light";

    /** JSON 数组字符串，如 ["CET4","FCE"] */
    private String activeLevels;

    /** 预留扩展字段，JSON 字符串 */
    private String extra;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private java.time.LocalDateTime updatedAt;
}
