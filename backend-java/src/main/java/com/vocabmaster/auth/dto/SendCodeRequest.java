package com.vocabmaster.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendCodeRequest {

    @NotBlank(message = "类型不能为空")
    @Pattern(regexp = "phone|email", message = "type 只能是 phone 或 email")
    private String type;

    @NotBlank(message = "账号不能为空")
    private String identifier;

    @NotBlank(message = "场景不能为空")
    @Pattern(regexp = "register|login|reset_password|bind", message = "不支持的 scene")
    private String scene;
}
