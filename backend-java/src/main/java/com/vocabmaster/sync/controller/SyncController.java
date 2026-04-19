package com.vocabmaster.sync.controller;

import com.vocabmaster.common.result.R;
import com.vocabmaster.security.UserContext;
import com.vocabmaster.sync.dto.SyncPullResponse;
import com.vocabmaster.sync.dto.SyncPushRequest;
import com.vocabmaster.sync.dto.SyncPushResponse;
import com.vocabmaster.sync.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
@Tag(name = "多端同步", description = "增量拉取 / 离线队列推送")
public class SyncController {

    private final SyncService syncService;

    @GetMapping("/pull")
    @Operation(summary = "增量拉取（since=ISO 8601，首次同步可不传）")
    public R<SyncPullResponse> pull(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        Long userId = UserContext.currentUserId();
        return R.ok(syncService.pull(userId, deviceId != null ? deviceId : "unknown", since));
    }

    @PostMapping("/push")
    @Operation(summary = "推送离线答题 / 打卡队列")
    public R<SyncPushResponse> push(@Valid @RequestBody SyncPushRequest req) {
        Long userId = UserContext.currentUserId();
        return R.ok(syncService.push(userId, req));
    }
}
