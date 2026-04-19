package com.vocabmaster.word.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("level")
public class Level {

    /** 等级代码，如 KET / CET4 / TEM8 */
    @TableId(type = IdType.INPUT)
    private String code;

    private String nameZh;
    private String nameEn;
    private Integer sortOrder;
    private Integer targetWordCount;
    private String description;
}
