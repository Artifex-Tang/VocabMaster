package com.vocabmaster.study.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.vocabmaster.common.exception.BizException;
import com.vocabmaster.common.result.ErrorCode;
import com.vocabmaster.study.dto.AnswerRequest;
import com.vocabmaster.study.dto.AnswerResponse;
import com.vocabmaster.study.entity.StudyLog;
import com.vocabmaster.study.entity.UserWordProgress;
import com.vocabmaster.study.entity.WrongWord;
import com.vocabmaster.study.mapper.StudyLogMapper;
import com.vocabmaster.study.mapper.UserWordProgressMapper;
import com.vocabmaster.study.mapper.WrongWordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudyService {

    private static final java.util.Set<String> VALID_RESULTS = java.util.Set.of("correct", "wrong", "skip");

    private final UserWordProgressMapper progressMapper;
    private final StudyLogMapper studyLogMapper;
    private final WrongWordMapper wrongWordMapper;
    private final EbbinghausScheduler scheduler;
    private final TodayPlanService todayPlanService;

    /** 单次答题，事务保证 progress + study_log + wrong_word 原子写入。 */
    @Transactional
    public AnswerResponse answer(Long userId, AnswerRequest req) {
        validate(req);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime clientTs = resolveClientTs(req.getClientTs(), now);

        UserWordProgress progress = getOrCreateProgress(userId, req.getWordId(), req.getLevelCode());

        // 冲突检测：上报的 client_ts 比数据库里的更旧，拒绝
        if (progress.getClientUpdatedAt() != null
                && !clientTs.isAfter(progress.getClientUpdatedAt())) {
            throw new BizException(ErrorCode.CLIENT_TS_TOO_OLD);
        }

        int stageBefore = progress.getStage();
        EbbinghausScheduler.ScheduleResult result = scheduler.schedule(stageBefore, req.getResult(), now);

        applyProgress(progress, result, req.getResult(), clientTs, now);
        progressMapper.updateById(progress);

        writeStudyLog(userId, req, stageBefore, result.stageAfter(), now, clientTs);

        if ("wrong".equals(req.getResult())) {
            upsertWrongWord(userId, req.getWordId(), req.getLevelCode(), now);
        }

        todayPlanService.evict(userId, req.getLevelCode());

        return AnswerResponse.builder()
                .wordId(req.getWordId())
                .stageBefore(stageBefore)
                .stageAfter(result.stageAfter())
                .nextReviewAt(result.nextReviewAt())
                .mastered(result.mastered())
                .build();
    }

    /**
     * 批量答题（离线同步）。按 client_ts 排序后逐条处理，冲突跳过。
     */
    @Transactional
    public List<AnswerResponse> answerBatch(Long userId, List<AnswerRequest> answers) {
        List<AnswerRequest> sorted = new ArrayList<>(answers);
        sorted.sort(Comparator.comparing(r -> r.getClientTs() == null
                ? LocalDateTime.MIN : r.getClientTs()));

        List<AnswerResponse> responses = new ArrayList<>(sorted.size());
        for (AnswerRequest req : sorted) {
            try {
                responses.add(answer(userId, req));
            } catch (BizException e) {
                if (e.getCode() == ErrorCode.CLIENT_TS_TOO_OLD.getCode()) {
                    log.debug("batch: skipping stale answer wordId={}", req.getWordId());
                } else {
                    throw e;
                }
            }
        }
        return responses;
    }

    /** 重置单个单词的学习进度（stage 清零）。 */
    @Transactional
    public void reset(Long userId, Long wordId) {
        UserWordProgress progress = progressMapper.selectOne(
                Wrappers.<UserWordProgress>lambdaQuery()
                        .eq(UserWordProgress::getUserId, userId)
                        .eq(UserWordProgress::getWordId, wordId));
        if (progress == null) return;

        progress.setStage(0);
        progress.setCorrectCount(0);
        progress.setWrongCount(0);
        progress.setLastReviewedAt(null);
        progress.setNextReviewAt(null);
        progress.setFirstLearnedAt(null);
        progress.setMasteredAt(null);
        progress.setClientUpdatedAt(null);
        progressMapper.updateById(progress);

        todayPlanService.evict(userId, progress.getLevelCode());
    }

    /** 直接将单词标记为已掌握（stage = 9，mastered_at = now）。 */
    @Transactional
    public AnswerResponse markMastered(Long userId, Long wordId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        UserWordProgress progress = getOrCreateProgress(userId, wordId, null);

        int stageBefore = progress.getStage();
        progress.setStage(9);
        progress.setLastReviewedAt(now);
        progress.setNextReviewAt(now.plusHours((long) EbbinghausScheduler.INTERVALS_HOURS[8]));
        progress.setMasteredAt(now);
        if (progress.getFirstLearnedAt() == null) {
            progress.setFirstLearnedAt(now);
        }
        progressMapper.updateById(progress);

        todayPlanService.evict(userId, progress.getLevelCode());

        return AnswerResponse.builder()
                .wordId(wordId)
                .stageBefore(stageBefore)
                .stageAfter(9)
                .nextReviewAt(progress.getNextReviewAt())
                .mastered(true)
                .build();
    }

    // ---- private ----

    private void validate(AnswerRequest req) {
        if (!VALID_RESULTS.contains(req.getResult())) {
            throw new BizException(ErrorCode.ANSWER_INVALID);
        }
    }

    private LocalDateTime resolveClientTs(LocalDateTime clientTs, LocalDateTime serverNow) {
        if (clientTs == null) return serverNow;
        // 客户端时钟超前：cap 到服务端时间
        return clientTs.isAfter(serverNow) ? serverNow : clientTs;
    }

    private UserWordProgress getOrCreateProgress(Long userId, Long wordId, String levelCode) {
        UserWordProgress progress = progressMapper.selectOne(
                Wrappers.<UserWordProgress>lambdaQuery()
                        .eq(UserWordProgress::getUserId, userId)
                        .eq(UserWordProgress::getWordId, wordId));
        if (progress != null) return progress;

        // 首次学习：INSERT 新记录
        progress = UserWordProgress.builder()
                .userId(userId)
                .wordId(wordId)
                .levelCode(levelCode)
                .stage(0)
                .correctCount(0)
                .wrongCount(0)
                .build();
        progressMapper.insert(progress);
        return progress;
    }

    private void applyProgress(UserWordProgress p, EbbinghausScheduler.ScheduleResult r,
                                String result, LocalDateTime clientTs, LocalDateTime now) {
        int stageBefore = p.getStage();

        p.setStage(r.stageAfter());
        p.setLastReviewedAt(now);
        p.setNextReviewAt(r.nextReviewAt());
        p.setClientUpdatedAt(clientTs);

        if (stageBefore == 0 && r.stageAfter() > 0) {
            p.setFirstLearnedAt(now);
        }
        if (r.mastered()) {
            p.setMasteredAt(now);
        }
        // stage 9 答错 → 回退到 8，清除掌握时间
        if (stageBefore == 9 && r.stageAfter() < 9) {
            p.setMasteredAt(null);
        }

        if ("correct".equals(result)) {
            p.setCorrectCount(p.getCorrectCount() + 1);
        } else if ("wrong".equals(result)) {
            p.setWrongCount(p.getWrongCount() + 1);
        }
    }

    private void writeStudyLog(Long userId, AnswerRequest req, int stageBefore,
                                int stageAfter, LocalDateTime now, LocalDateTime clientTs) {
        String action = stageBefore == 0 ? "learn" : "review";
        StudyLog log = StudyLog.builder()
                .userId(userId)
                .wordId(req.getWordId())
                .levelCode(req.getLevelCode())
                .action(action)
                .result(req.getResult())
                .mode(req.getMode())
                .stageBefore(stageBefore)
                .stageAfter(stageAfter)
                .durationMs(req.getDurationMs())
                .clientTs(clientTs)
                .build();
        studyLogMapper.insert(log);
    }

    private void upsertWrongWord(Long userId, Long wordId, String levelCode, LocalDateTime now) {
        WrongWord existing = wrongWordMapper.selectOne(
                Wrappers.<WrongWord>lambdaQuery()
                        .eq(WrongWord::getUserId, userId)
                        .eq(WrongWord::getWordId, wordId));
        if (existing != null) {
            existing.setWrongCount(existing.getWrongCount() + 1);
            existing.setLastWrongAt(now);
            existing.setResolved(0);
            wrongWordMapper.updateById(existing);
        } else {
            WrongWord ww = WrongWord.builder()
                    .userId(userId)
                    .wordId(wordId)
                    .levelCode(levelCode)
                    .wrongCount(1)
                    .lastWrongAt(now)
                    .resolved(0)
                    .build();
            wrongWordMapper.insert(ww);
        }
    }
}
