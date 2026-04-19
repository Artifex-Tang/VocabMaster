package com.vocabmaster.checkin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("achievement")
public class Achievement {

    @TableId(type = IdType.INPUT)
    private String code;

    private String nameZh;
    private String descriptionZh;
    private String icon;

    /** streak / volume / accuracy / level */
    private String category;

    /** 触发规则，JSON 字符串 */
    private String triggerRule;

    private Integer sortOrder;
}
