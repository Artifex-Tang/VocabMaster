package com.vocabmaster.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginAppleRequest {

    @NotBlank(message = "identity_token 不能为空")
    private String identityToken;

    private String authorizationCode;
}
