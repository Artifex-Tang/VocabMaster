package com.vocabmaster.stats.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.vocabmaster.common.constant.AppConstants;
import com.vocabmaster.stats.dto.*;
import com.vocabmaster.study.entity.StudyLog;
import com.vocabmaster.study.mapper.StudyLogMapper;
import com.vocabmaster.study.mapper.UserWordProgressMapper;
import com.vocabmaster.study.service.EbbinghausScheduler;
import com.vocabmaster.user.entity.UserSettings;
import com.vocabmaster.user.mapper.UserSettingsMapper;
import com.vocabmaster.word.entity.WordBank;
import com.vocabmaster.word.mapper.WordBankMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final StudyLogMapper studyLogMapper;
    private final UserWordProgressMapper progressMapper;
    private final WordBankMapper wordBankMapper;
    private final UserSettingsMapper settingsMapper;

    // ---- 今日统计 ----

    public TodayStatsResponse todayStats(Long userId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        List<Map<String, Object>> rows = studyLogMapper.dailySummary(userId, from, to);
        Map<String, Object> row = rows.isEmpty() ? Collections.emptyMap() : rows.get(0);

        long learned = toLong(row.get("words_learned"));
        long reviewed = toLong(row.get("words_reviewed"));
        long correct = toLong(row.get("correct_count"));
        long total = toLong(row.get("total_count"));
        long totalMs = toLong(row.get("total_ms"));

        double accuracy = total == 0 ? 0.0 : round3((double) correct / total);

        UserSettings settings = getSettings(userId);

        return TodayStatsResponse.builder()
                .date(today.format(DATE_FMT))
                .wordsLearned(learned)
                .wordsReviewed(reviewed)
                .correctCount(correct)
                .accuracy(accuracy)
                .durationSeconds(totalMs / 1000)
                .goalProgress(TodayStatsResponse.GoalProgress.builder()
                        .newWords(learned + "/" + settings.getDailyNewWordsGoal())
                        .review(reviewed + "/" + settings.getDailyReviewGoal())
                        .build())
                .build();
    }

    // ---- 周报 / 月报 ----

    public SummaryResponse summary(Long userId, String period, String dateStr) {
        LocalDate anchor = dateStr != null ? LocalDate.parse(dateStr, DATE_FMT)
                : LocalDate.now(ZoneOffset.UTC);

        LocalDate startDate;
        LocalDate endDate;

        if ("month".equals(period)) {
            startDate = anchor.with(TemporalAdjusters.firstDayOfMonth());
            endDate = anchor.with(TemporalAdjusters.lastDayOfMonth());
        } else {
            // week: Mon-Sun
            startDate = anchor.with(java.time.DayOfWeek.MONDAY);
            endDate = startDate.plusDays(6);
        }

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.plusDays(1).atStartOfDay();

        List<Map<String, Object>> rows = studyLogMapper.dailySummary(userId, from, to);

        List<DailyBreakdown> daily = rows.stream().map(r -> {
            long c = toLong(r.get("correct_count"));
            long t = toLong(r.get("total_count"));
            return DailyBreakdown.builder()
                    .date(String.valueOf(r.get("stat_date")))
                    .wordsLearned(toLong(r.get("words_learned")))
                    .wordsReviewed(toLong(r.get("words_reviewed")))
                    .accuracy(t == 0 ? 0.0 : round3((double) c / t))
                    .build();
        }).toList();

        long totalLearned = daily.stream().mapToLong(DailyBreakdown::getWordsLearned).sum();
        long totalReviewed = daily.stream().mapToLong(DailyBreakdown::getWordsReviewed).sum();
        int daysActive = (int) daily.stream()
                .filter(d -> d.getWordsLearned() + d.getWordsReviewed() > 0).count();

        long totalCorrect = rows.stream().mapToLong(r -> toLong(r.get("correct_count"))).sum();
        long totalCount = rows.stream().mapToLong(r -> toLong(r.get("total_count"))).sum();
        double avgAccuracy = totalCount == 0 ? 0.0 : round3((double) totalCorrect / totalCount);

        List<LevelBreakdown> levelBreakdown = progressMapper.levelSummary(userId).stream()
                .map(r -> LevelBreakdown.builder()
                        .levelCode(String.valueOf(r.get("level_code")))
                        .mastered(toLong(r.get("mastered")))
                        .learning(toLong(r.get("learning")))
                        .build())
                .toList();

        return SummaryResponse.builder()
                .period(period)
                .startDate(startDate.format(DATE_FMT))
                .endDate(endDate.format(DATE_FMT))
                .daysActive(daysActive)
                .totalLearned(totalLearned)
                .totalReviewed(totalReviewed)
                .avgAccuracy(avgAccuracy)
                .dailyBreakdown(daily)
                .levelBreakdown(levelBreakdown)
                .build();
    }

    // ---- 遗忘曲线 ----

    public ForgettingCurveResponse forgettingCurve(Long userId, Long wordId) {
        WordBank wb = wordBankMapper.selectById(wordId);
        String wordText = wb != null ? wb.getWord() : String.valueOf(wordId);

        List<StudyLog> logs = studyLogMapper.findByUserAndWord(userId, wordId);

        List<ReviewPoint> reviews = logs.stream().map(l -> ReviewPoint.builder()
                .ts(l.getCreatedAt())
                .result(l.getResult())
                .stageAfter(l.getStageAfter() != null ? l.getStageAfter() : 0)
                .build()).toList();

        return ForgettingCurveResponse.builder()
                .wordId(wordId)
                .word(wordText)
                .reviews(reviews)
                .theoreticalCurve(ForgettingCurveResponse.TheoreticalCurve.builder()
                        .type("ebbinghaus")
                        .stages(EbbinghausScheduler.INTERVALS_HOURS)
                        .build())
                .build();
    }

    // ---- 等级概览 ----

    public LevelOverviewResponse levelOverview(Long userId, String levelCode) {
        // 总词数
        long totalWords = wordBankMapper.selectCount(
                Wrappers.<WordBank>lambdaQuery()
                        .eq(WordBank::getLevelCode, levelCode)
                        .eq(WordBank::getAuditStatus, AppConstants.AUDIT_PASS));

        // 各阶段分布
        List<Map<String, Object>> stageRows = progressMapper.countByStage(userId, levelCode);

        Map<Integer, Long> stageMap = stageRows.stream()
                .collect(Collectors.toMap(
                        r -> toInt(r.get("stage")),
                        r -> toLong(r.get("cnt"))));

        long mastered = stageMap.getOrDefault(9, 0L);
        long learning = stageMap.entrySet().stream()
                .filter(e -> e.getKey() > 0 && e.getKey() < 9)
                .mapToLong(Map.Entry::getValue).sum();
        long notStarted = totalWords - mastered - learning;

        List<StageDistribution> dist = new ArrayList<>();
        for (int s = 0; s <= 9; s++) {
            long cnt = stageMap.getOrDefault(s, 0L);
            if (cnt > 0) {
                dist.add(StageDistribution.builder().stage(s).count(cnt).build());
            }
        }

        double masteryRate = totalWords == 0 ? 0.0 : round3((double) mastered / totalWords);

        return LevelOverviewResponse.builder()
                .levelCode(levelCode)
                .totalWords(totalWords)
                .notStarted(Math.max(0, notStarted))
                .learning(learning)
                .mastered(mastered)
                .masteryRate(masteryRate)
                .stageDistribution(dist)
                .build();
    }

    // ---- helpers ----

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return 0L; }
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 0; }
    }

    private double round3(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    private UserSettings getSettings(Long userId) {
        UserSettings s = settingsMapper.selectById(userId);
        return s != null ? s : UserSettings.builder().userId(userId).build();
    }
}
