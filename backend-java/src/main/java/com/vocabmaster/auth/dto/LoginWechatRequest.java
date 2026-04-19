package com.vocabmaster.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginWechatRequest {

    /** wx.login() 拿到的 code */
    @NotBlank(message = "微信 code 不能为空")
    private String code;

    /** 首次授权时前端传入的用户信息（可选） */
    private WechatUserInfoDto userInfo;

    @Data
    public static class WechatUserInfoDto {
        private String nickname;
        private String avatarUrl;
    }
}
