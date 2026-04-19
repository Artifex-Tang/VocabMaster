package com.vocabmaster.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("wrong_word")
public class WrongWord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long wordId;
    private String levelCode;

    @Builder.Default
    private Integer wrongCount = 1;

    private LocalDateTime lastWrongAt;

    /** 连续答对 N 次后置 1 */
    @Builder.Default
    private Integer resolved = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
