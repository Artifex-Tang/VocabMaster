package com.vocabmaster.wordlist.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("word_list")
public class WordList {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** NULL=系统内置共享；非空=用户个人上传 */
    private Long ownerUserId;
    private String name;
    private String description;
    /** builtin | imported */
    private String sourceType;
    private String originLevelCode;
    private Integer wordCount;
    private String coverEmoji;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now(3)")
    private LocalDateTime deletedAt;
}
