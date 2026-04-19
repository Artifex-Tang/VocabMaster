package com.vocabmaster.sync.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.vocabmaster.checkin.entity.Checkin;
import com.vocabmaster.checkin.mapper.CheckinMapper;
import com.vocabmaster.study.dto.AnswerResponse;
import com.vocabmaster.study.entity.UserWordProgress;
import com.vocabmaster.study.mapper.UserWordProgressMapper;
import com.vocabmaster.study.service.StudyService;
import com.vocabmaster.sync.dto.SyncCheckinItem;
import com.vocabmaster.sync.dto.SyncPullResponse;
import com.vocabmaster.sync.dto.SyncPushRequest;
import com.vocabmaster.sync.dto.SyncPushResponse;
import com.vocabmaster.sync.entity.SyncToken;
import com.vocabmaster.sync.mapper.SyncTokenMapper;
import com.vocabmaster.user.entity.UserSettings;
import com.vocabmaster.user.mapper.UserSettingsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private final UserWordProgressMapper progressMapper;
    private final CheckinMapper checkinMapper;
    private final UserSettingsMapper settingsMapper;
    private final SyncTokenMapper syncTokenMapper;
    private final StudyService studyService;

    /**
     * 增量拉取：返回 since 之后其他设备产生的学习进度、打卡、设置变更。
     * 同时更新本设备的 last_sync_at。
     */
    @Transactional
    public SyncPullResponse pull(Long userId, String deviceId, LocalDateTime since) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // 更新设备同步时间戳
        upsertSyncToken(userId, deviceId, now, null);

        // 进度变更：updatedAt > since
        List<UserWordProgress> progressChanges = since == null
                ? Collections.emptyList()
                : progressMapper.selectList(
                        Wrappers.<UserWordProgress>lambdaQuery()
                                .eq(UserWordProgress::getUserId, userId)
                                .gt(UserWordProgress::getUpdatedAt, since));

        // 打卡变更：createdAt > since（打卡记录不可变，只需看新增）
        List<Checkin> checkinChanges = since == null
                ? Collections.emptyList()
                : checkinMapper.selectList(
                        Wrappers.<Checkin>lambdaQuery()
                                .eq(Checkin::getUserId, userId)
                                .gt(Checkin::getCreatedAt, since));

        // 设置变更：updatedAt > since
        UserSettings settings = null;
        if (since != null) {
            UserSettings s = settingsMapper.selectById(userId);
            if (s != null && s.getUpdatedAt() != null && s.getUpdatedAt().isAfter(since)) {
                settings = s;
            }
        }

        return SyncPullResponse.builder()
                .serverTs(now)
                .changes(SyncPullResponse.Changes.builder()
                        .progress(progressChanges)
                        .checkin(checkinChanges)
                        .settings(settings)
                        .build())
                .build();
    }

    /**
     * 推送离线队列：按 client_ts 排序处理答题记录，upsert 打卡记录。
     */
    @Transactional
    public SyncPushResponse push(Long userId, SyncPushRequest req) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // 处理答题队列
        List<AnswerResponse> answerResults = Collections.emptyList();
        int accepted = 0, skipped = 0;

        if (req.getAnswers() != null && !req.getAnswers().isEmpty()) {
            answerResults = studyService.answerBatch(userId, req.getAnswers());
            accepted = answerResults.size();
            skipped = req.getAnswers().size() - accepted;
        }

        // 处理离线打卡
        int checkinsUpserted = 0;
        if (req.getCheckins() != null) {
            for (SyncCheckinItem item : req.getCheckins()) {
                if (item.getCheckinDate() == null) continue;
                try {
                    checkinMapper.upsertStats(
                            userId,
                            item.getCheckinDate(),
                            item.getWordsLearned() != null ? item.getWordsLearned() : 0,
                            item.getWordsReviewed() != null ? item.getWordsReviewed() : 0,
                            item.getCorrectCount() != null ? item.getCorrectCount() : 0,
                            item.getDurationSeconds() != null ? item.getDurationSeconds() : 0);
                    checkinsUpserted++;
                } catch (Exception e) {
                    log.warn("sync push: checkin upsert failed date={}", item.getCheckinDate(), e);
                }
            }
        }

        upsertSyncToken(userId, req.getDeviceId(), null, now);

        return SyncPushResponse.builder()
                .serverTs(now)
                .answersAccepted(accepted)
                .answersSkipped(skipped)
                .checkinsUpserted(checkinsUpserted)
                .answerResults(answerResults)
                .build();
    }

    // ---- private ----

    private void upsertSyncToken(Long userId, String deviceId,
                                  LocalDateTime lastSyncAt, LocalDateTime lastPushAt) {
        SyncToken token = syncTokenMapper.findByUserAndDevice(userId, deviceId);
        if (token == null) {
            token = SyncToken.builder()
                    .userId(userId)
                    .deviceId(deviceId)
                    .build();
        }
        if (lastSyncAt != null) token.setLastSyncAt(lastSyncAt);
        if (lastPushAt != null) token.setLastPushAt(lastPushAt);

        if (token.getId() == null) syncTokenMapper.insert(token);
        else syncTokenMapper.updateById(token);
    }
}
