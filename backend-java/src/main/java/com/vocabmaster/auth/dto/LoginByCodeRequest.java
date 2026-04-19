package com.vocabmaster.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginByCodeRequest {

    @NotBlank(message = "手机号不能为空")
    private String identifier;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
