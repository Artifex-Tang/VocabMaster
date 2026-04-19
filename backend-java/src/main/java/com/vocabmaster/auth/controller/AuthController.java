package com.vocabmaster.auth.controller;

import com.vocabmaster.auth.dto.*;
import com.vocabmaster.auth.service.AuthService;
import com.vocabmaster.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "注册、登录、Token 管理")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "注册（邮箱/手机号）")
    public R<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return R.ok(authService.register(req));
    }

    @PostMapping("/send-code")
    @Operation(summary = "发送验证码")
    public R<Map<String, Integer>> sendCode(@Valid @RequestBody SendCodeRequest req) {
        int expiresIn = authService.sendCode(req);
        return R.ok(Map.of("expires_in", expiresIn));
    }

    @PostMapping("/login")
    @Operation(summary = "密码登录")
    public R<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return R.ok(authService.login(req));
    }

    @PostMapping("/login-by-code")
    @Operation(summary = "验证码登录（手机号，未注册则自动注册）")
    public R<AuthResponse> loginByCode(@Valid @RequestBody LoginByCodeRequest req) {
        return R.ok(authService.loginByCode(req));
    }

    @PostMapping("/login-wechat")
    @Operation(summary = "微信小程序登录")
    public R<AuthResponse> loginWechat(@Valid @RequestBody LoginWechatRequest req) {
        return R.ok(authService.loginWechat(req));
    }

    @PostMapping("/login-apple")
    @Operation(summary = "Apple 登录")
    public R<AuthResponse> loginApple(@Valid @RequestBody LoginAppleRequest req) {
        return R.ok(authService.loginApple(req));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token")
    public R<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return R.ok(authService.refresh(req));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出（吊销当前 Token）")
    public R<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7));
        }
        return R.ok();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码")
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return R.ok();
    }
}
