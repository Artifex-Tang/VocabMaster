package com.vocabmaster.study.controller;

import com.vocabmaster.common.annotation.RateLimit;
import com.vocabmaster.common.result.R;
import com.vocabmaster.security.UserContext;
import com.vocabmaster.study.dto.*;
import com.vocabmaster.study.service.StudyService;
import com.vocabmaster.study.service.TodayPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
@Tag(name = "学习", description = "今日计划 / 答题 / 进度管理")
public class StudyController {

    private final TodayPlanService todayPlanService;
    private final StudyService studyService;

    @GetMapping("/today")
    @Operation(summary = "获取今日学习计划（复习 + 新词）")
    public R<TodayPlanResponse> today(@RequestParam String level) {
        Long userId = UserContext.currentUserId();
        return R.ok(todayPlanService.getTodayPlan(userId, level));
    }

    @PostMapping("/answer")
    @RateLimit(key = "study:answer", limit = 60, window = 60)
    @Operation(summary = "上报单次答题结果")
    public R<AnswerResponse> answer(@Valid @RequestBody AnswerRequest req) {
        Long userId = UserContext.currentUserId();
        return R.ok(studyService.answer(userId, req));
    }

    @PostMapping("/answer-batch")
    @Operation(summary = "批量上报答题结果（离线同步）")
    public R<List<AnswerResponse>> answerBatch(@Valid @RequestBody AnswerBatchRequest req) {
        Long userId = UserContext.currentUserId();
        return R.ok(studyService.answerBatch(userId, req.getAnswers()));
    }

    @PostMapping("/reset")
    @Operation(summary = "重置单词学习进度")
    public R<Void> reset(@RequestParam Long wordId) {
        Long userId = UserContext.currentUserId();
        studyService.reset(userId, wordId);
        return R.ok(null);
    }

    @PostMapping("/mark-mastered")
    @Operation(summary = "标记单词为已掌握")
    public R<AnswerResponse> markMastered(@RequestParam Long wordId) {
        Long userId = UserContext.currentUserId();
        return R.ok(studyService.markMastered(userId, wordId));
    }
}
