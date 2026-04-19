package com.vocabmaster.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String uuid;
    private String username;

    /** AES-256-GCM 加密后的手机号 */
    @JsonIgnore
    private String phone;

    /** SHA-256 哈希，用于唯一索引和查询 */
    @JsonIgnore
    private String phoneHash;

    private String email;

    @JsonIgnore
    private String passwordHash;

    private String avatarUrl;
    private String nickname;

    @Builder.Default
    private String timezone = "Asia/Shanghai";

    @Builder.Default
    private String locale = "zh-CN";

    @Builder.Default
    private String role = "USER";

    @Builder.Default
    private Integer status = 1;

    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now(3)")
    private LocalDateTime deletedAt;
}
