package com.vocabmaster.stats.controller;

import com.vocabmaster.common.result.R;
import com.vocabmaster.security.UserContext;
import com.vocabmaster.stats.dto.*;
import com.vocabmaster.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Tag(name = "统计", description = "今日报表 / 周月报 / 遗忘曲线 / 等级概览")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/today")
    @Operation(summary = "今日学习统计")
    public R<TodayStatsResponse> today() {
        return R.ok(statsService.todayStats(UserContext.currentUserId()));
    }

    @GetMapping("/summary")
    @Operation(summary = "周报 / 月报（period=week|month，date=yyyy-MM-dd）")
    public R<SummaryResponse> summary(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) String date) {
        return R.ok(statsService.summary(UserContext.currentUserId(), period, date));
    }

    @GetMapping("/forgetting-curve")
    @Operation(summary = "单词遗忘曲线数据")
    public R<ForgettingCurveResponse> forgettingCurve(@RequestParam Long wordId) {
        return R.ok(statsService.forgettingCurve(UserContext.currentUserId(), wordId));
    }

    @GetMapping("/level-overview")
    @Operation(summary = "等级学习概览（词数 / 掌握率 / 阶段分布）")
    public R<LevelOverviewResponse> levelOverview(@RequestParam String level) {
        return R.ok(statsService.levelOverview(UserContext.currentUserId(), level));
    }
}
