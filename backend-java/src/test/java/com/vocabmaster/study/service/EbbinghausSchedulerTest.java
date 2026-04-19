package com.vocabmaster.study.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class EbbinghausSchedulerTest {

    private EbbinghausScheduler scheduler;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        scheduler = new EbbinghausScheduler();
        now = LocalDateTime.of(2026, 4, 17, 10, 0, 0);
    }

    // ---- nextStage 状态转移 ----

    @Test
    @DisplayName("stage 0 答对 → 1")
    void correctFromStage0() {
        assertThat(scheduler.nextStage(0, "correct")).isEqualTo(1);
    }

    @Test
    @DisplayName("stage 8 答对 → 9（上限）")
    void correctToStage9() {
        assertThat(scheduler.nextStage(8, "correct")).isEqualTo(9);
    }

    @Test
    @DisplayName("stage 9 答对 → 9（上限不超过 9）")
    void correctAtStage9StaysAt9() {
        assertThat(scheduler.nextStage(9, "correct")).isEqualTo(9);
    }

    @Test
    @DisplayName("stage 3 答错 → 2（回退 1 级）")
    void wrongFromStage3() {
        assertThat(scheduler.nextStage(3, "wrong")).isEqualTo(2);
    }

    @Test
    @DisplayName("stage 1 答错 → 1（不低于 1）")
    void wrongAtStage1Stays1() {
        assertThat(scheduler.nextStage(1, "wrong")).isEqualTo(1);
    }

    @Test
    @DisplayName("stage 9 答错 → 8（回退离开掌握区）")
    void wrongFromStage9() {
        assertThat(scheduler.nextStage(9, "wrong")).isEqualTo(8);
    }

    @Test
    @DisplayName("stage 0 skip → 1（保证进入复习循环）")
    void skipFromStage0() {
        assertThat(scheduler.nextStage(0, "skip")).isEqualTo(1);
    }

    @Test
    @DisplayName("stage 5 skip → 5（stage 不变）")
    void skipKeepsStage() {
        assertThat(scheduler.nextStage(5, "skip")).isEqualTo(5);
    }

    @Test
    @DisplayName("非法 result 抛 IllegalArgumentException")
    void unknownResultThrows() {
        assertThatThrownBy(() -> scheduler.nextStage(1, "invalid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- computeNextReviewAt ----

    @Test
    @DisplayName("stage 1 → next 5 分钟后")
    void nextReviewStage1Is5Min() {
        LocalDateTime next = scheduler.computeNextReviewAt(1, now);
        assertThat(next).isEqualTo(now.plusMinutes(5));
    }

    @Test
    @DisplayName("stage 2 → next 30 分钟后")
    void nextReviewStage2Is30Min() {
        LocalDateTime next = scheduler.computeNextReviewAt(2, now);
        assertThat(next).isEqualTo(now.plusMinutes(30));
    }

    @Test
    @DisplayName("stage 3 → next 12 小时后")
    void nextReviewStage3Is12h() {
        LocalDateTime next = scheduler.computeNextReviewAt(3, now);
        assertThat(next).isEqualTo(now.plusHours(12));
    }

    @Test
    @DisplayName("stage 9 → next 720 小时后（30 天）")
    void nextReviewStage9Is30Days() {
        LocalDateTime next = scheduler.computeNextReviewAt(9, now);
        assertThat(next).isEqualTo(now.plusHours(720));
    }

    // ---- schedule（整体结果）----

    @Test
    @DisplayName("首次学习（stage 0 答对）→ mastered=false, stageAfter=1, next=5min")
    void scheduleFirstLearn() {
        var r = scheduler.schedule(0, "correct", now);
        assertThat(r.stageBefore()).isEqualTo(0);
        assertThat(r.stageAfter()).isEqualTo(1);
        assertThat(r.nextReviewAt()).isEqualTo(now.plusMinutes(5));
        assertThat(r.mastered()).isFalse();
    }

    @Test
    @DisplayName("stage 8 答对 → mastered=true（升到 9 视为刚掌握）")
    void scheduleStage8CorrectMastered() {
        var r = scheduler.schedule(8, "correct", now);
        assertThat(r.stageAfter()).isEqualTo(9);
        assertThat(r.mastered()).isTrue();
    }

    @Test
    @DisplayName("stage 9 答对 → mastered=false（已经掌握不重复触发）")
    void scheduleStage9CorrectNoMastered() {
        var r = scheduler.schedule(9, "correct", now);
        assertThat(r.mastered()).isFalse();
    }

    @Test
    @DisplayName("skip → next 延后 10 分钟，stage 不推进")
    void scheduleSkipDelay10Min() {
        var r = scheduler.schedule(3, "skip", now);
        assertThat(r.stageAfter()).isEqualTo(3);
        assertThat(r.nextReviewAt()).isEqualTo(now.plusMinutes(10));
    }

    // ---- spec 文档测试用例对齐 ----

    @Test
    @DisplayName("文档示例：stage 3 答错 → stage 2，next = stage 2 间隔（30min）后")
    void docExampleWrong() {
        // stage 3 答错 → stage_after = 2，next = now + 30min
        var r = scheduler.schedule(3, "wrong", now);
        assertThat(r.stageAfter()).isEqualTo(2);
        assertThat(r.nextReviewAt()).isEqualTo(now.plusMinutes(30));
    }

    @Test
    @DisplayName("全流程：0→1→2→3（三次答对间隔正确）")
    void fullFlowThreeCorrect() {
        LocalDateTime t = now;

        var r1 = scheduler.schedule(0, "correct", t);
        assertThat(r1.stageAfter()).isEqualTo(1);
        t = r1.nextReviewAt(); // +5 min

        var r2 = scheduler.schedule(1, "correct", t);
        assertThat(r2.stageAfter()).isEqualTo(2);
        t = r2.nextReviewAt(); // +30 min

        var r3 = scheduler.schedule(2, "correct", t);
        assertThat(r3.stageAfter()).isEqualTo(3);
        assertThat(r3.nextReviewAt()).isEqualTo(t.plusHours(12));
    }
}
