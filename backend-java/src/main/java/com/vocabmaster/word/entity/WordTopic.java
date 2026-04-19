package com.vocabmaster.word.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("word_topic")
public class WordTopic {

    @TableId(type = IdType.INPUT)
    private String code;

    private String nameZh;
    private String nameEn;
    private String icon;
    private Integer sortOrder;
}
