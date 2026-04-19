package com.vocabmaster.user.controller;

import com.vocabmaster.common.result.R;
import com.vocabmaster.security.UserContext;
import com.vocabmaster.user.dto.UpdateProfileRequest;
import com.vocabmaster.user.dto.UpdateSettingsRequest;
import com.vocabmaster.user.dto.UserProfileDto;
import com.vocabmaster.user.entity.UserSettings;
import com.vocabmaster.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户", description = "个人信息 / 设置")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public R<UserProfileDto> me() {
        return R.ok(userService.getProfile(UserContext.currentUserId()));
    }

    @PatchMapping("/me")
    @Operation(summary = "更新用户信息（部分更新）")
    public R<UserProfileDto> updateMe(@RequestBody UpdateProfileRequest req) {
        return R.ok(userService.updateProfile(UserContext.currentUserId(), req));
    }

    @GetMapping("/settings")
    @Operation(summary = "获取用户设置")
    public R<UserSettings> settings() {
        return R.ok(userService.getSettings(UserContext.currentUserId()));
    }

    @PatchMapping("/settings")
    @Operation(summary = "更新用户设置（部分更新）")
    public R<UserSettings> updateSettings(@RequestBody UpdateSettingsRequest req) {
        return R.ok(userService.updateSettings(UserContext.currentUserId(), req));
    }

    @GetMapping("/export")
    @Operation(summary = "导出学习记录（CSV 直接下载）")
    public ResponseEntity<byte[]> export() {
        String csv = userService.exportCsv(UserContext.currentUserId());
        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"study_records.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @PostMapping("/delete-account")
    @Operation(summary = "注销账户（需验证码二次确认）")
    public R<Void> deleteAccount(@RequestParam String confirmCode) {
        userService.deleteAccount(UserContext.currentUserId(), confirmCode);
        return R.ok(null);
    }
}
