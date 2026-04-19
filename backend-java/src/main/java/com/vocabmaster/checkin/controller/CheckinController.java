package com.vocabmaster.checkin.controller;

import com.vocabmaster.checkin.dto.AchievementsResponse;
import com.vocabmaster.checkin.dto.CalendarResponse;
import com.vocabmaster.checkin.dto.CheckinResponse;
import com.vocabmaster.checkin.service.CheckinService;
import com.vocabmaster.common.result.R;
import com.vocabmaster.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkin")
@RequiredArgsConstructor
@Tag(name = "打卡", description = "每日打卡 / 打卡日历 / 成就")
public class CheckinController {

    private final CheckinService checkinService;

    @PostMapping("/today")
    @Operation(summary = "今日打卡（幂等）")
    public R<CheckinResponse> checkIn() {
        return R.ok(checkinService.checkIn(UserContext.currentUserId()));
    }

    @GetMapping("/calendar")
    @Operation(summary = "打卡日历（month=yyyy-MM，默认当月）")
    public R<CalendarResponse> calendar(@RequestParam(required = false) String month) {
        return R.ok(checkinService.calendar(UserContext.currentUserId(), month));
    }

    @GetMapping("/achievements")
    @Operation(summary = "成就列表（已解锁 + 未解锁）")
    public R<AchievementsResponse> achievements() {
        return R.ok(checkinService.achievements(UserContext.currentUserId()));
    }
}
