package com.vocabmaster.study.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 固定间隔九阶段艾宾浩斯复习调度器（纯函数，无 IO，便于单元测试）。
 *
 * 间隔数组（小时）：
 *   index 0 → stage 1→2 : 5 min
 *   index 1 → stage 2→3 : 30 min
 *   index 2 → stage 3→4 : 12 h
 *   index 3 → stage 4→5 : 24 h
 *   index 4 → stage 5→6 : 48 h
 *   index 5 → stage 6→7 : 96 h
 *   index 6 → stage 7→8 : 168 h (7 days)
 *   index 7 → stage 8→9 : 360 h (15 days)
 *   index 8 → stage 9 掌握保留期 : 720 h (30 days)
 */
@Component
public class EbbinghausScheduler {

    /** 单位：小时（double 支持 5/60 = 0.0833h） */
    static final double[] INTERVALS_HOURS = {
        5.0 / 60,   // 5 min
        0.5,        // 30 min
        12.0,
        24.0,
        48.0,
        96.0,
        168.0,
        360.0,
        720.0       // 30 days — stage 9 保留期后视为掌握
    };

    public record ScheduleResult(int stageBefore, int stageAfter,
                                 LocalDateTime nextReviewAt, boolean mastered) {}

    /**
     * 计算答题后的新状态。
     *
     * @param stageBefore 答题前 stage（0-9）
     * @param result      "correct" / "wrong" / "skip"
     * @param now         服务端 UTC 时间
     * @return 新阶段 + 下次复习时间
     */
    public ScheduleResult schedule(int stageBefore, String result, LocalDateTime now) {
        int stageAfter = nextStage(stageBefore, result);

        LocalDateTime nextReviewAt;
        if ("skip".equals(result)) {
            // skip：将 next_review_at 延后 10 分钟，不推进 stage
            nextReviewAt = now.plusMinutes(10);
        } else {
            nextReviewAt = computeNextReviewAt(stageAfter, now);
        }

        boolean mastered = (stageAfter == 9 && stageBefore < 9);
        return new ScheduleResult(stageBefore, stageAfter, nextReviewAt, mastered);
    }

    /** 仅计算新 stage，不含时间（供测试单独断言）。 */
    public int nextStage(int stageBefore, String result) {
        return switch (result) {
            case "correct" -> Math.min(stageBefore + 1, 9);
            case "wrong"   -> Math.max(1, stageBefore - 1);
            case "skip"    -> Math.max(1, stageBefore); // 确保已学词留在复习循环
            default        -> throw new IllegalArgumentException("unknown result: " + result);
        };
    }

    /**
     * 根据 stageAfter 计算下次复习时间。
     * stage 9 时用最后一个间隔（720h），答对后 StudyService 负责设置 mastered_at。
     */
    public LocalDateTime computeNextReviewAt(int stageAfter, LocalDateTime now) {
        int idx = Math.min(stageAfter - 1, INTERVALS_HOURS.length - 1);
        double hours = INTERVALS_HOURS[idx];
        long minutes = Math.round(hours * 60);
        return now.plusMinutes(minutes);
    }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
