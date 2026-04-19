package com.vocabmaster.word.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("word_bank")
public class WordBank {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String levelCode;
    private String word;
    private String wordLower;
    private String ipaUk;
    private String ipaUs;
    private String enDefinition;
    private String zhDefinition;
    private String exampleEn;
    private String exampleZh;
    private String topicCode;
    private String audioUrlUk;
    private String audioUrlUs;
    private String imageUrl;
    private String emoji;
    private Integer difficulty;
    private BigDecimal frequency;
    private String pos;

    /** 同义词/反义词，JSON 字符串 */
    private String relatedWords;

    /** 1审核通过 0待审 -1下架 */
    @Builder.Default
    private Integer auditStatus = 1;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now(3)")
    private LocalDateTime deletedAt;
}
