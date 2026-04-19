package com.vocabmaster.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    @Pattern(regexp = "email|phone", message = "type 只能是 email 或 phone")
    private String type;

    @NotBlank(message = "账号不能为空")
    private String identifier;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
