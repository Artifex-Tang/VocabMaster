package com.vocabmaster.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "登录类型不能为空")
    @Pattern(regexp = "email|phone", message = "type 只能是 email 或 phone")
    private String type;

    @NotBlank(message = "账号不能为空")
    private String identifier;

    @NotBlank(message = "密码不能为空")
    private String password;
}
