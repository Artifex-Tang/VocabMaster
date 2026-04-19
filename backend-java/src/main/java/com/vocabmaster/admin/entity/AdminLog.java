package com.vocabmaster.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("admin_log")
public class AdminLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminUserId;
    private String action;
    private String targetType;
    private String targetId;

    /** 操作详情，JSON 字符串 */
    private String detail;

    private String ip;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
