package com.vocabmaster.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_auth")
public class UserAuth {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** wechat / apple / google */
    private String provider;

    /** 第三方的 openid / sub */
    private String providerUserId;

    /** 微信 unionid */
    private String unionId;

    /** 原始第三方 profile，JSON 字符串 */
    private String rawProfile;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
