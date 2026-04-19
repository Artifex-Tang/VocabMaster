package com.vocabmaster.study.service;

import com.vocabmaster.common.constant.AppConstants;
import com.vocabmaster.common.constant.RedisKey;
import com.vocabmaster.study.dto.TodayPlanResponse;
import com.vocabmaster.study.entity.UserWordProgress;
import com.vocabmaster.study.mapper.UserWordProgressMapper;
import com.vocabmaster.user.entity.UserSettings;
import com.vocabmaster.user.mapper.UserSettingsMapper;
import com.vocabmaster.word.dto.WordDetailDto;
import com.vocabmaster.word.entity.WordBank;
import com.vocabmaster.word.mapper.WordBankMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TodayPlanService {

    private static final int PLAN_CACHE_MINUTES = 10;
    /** 估算：每词平均 20 秒 */
    private static final int SECONDS_PER_WORD = 20;

    private final UserWordProgressMapper progressMapper;
    private final WordBankMapper wordBankMapper;
    private final UserSettingsMapper settingsMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取今日学习计划（复习 + 新词），Redis 缓存 10 分钟。
     * 答题后需主动调用 {@link #evict} 失效缓存。
     */
    @SuppressWarnings("unchecked")
    public TodayPlanResponse getTodayPlan(Long userId, String levelCode) {
        String today = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String cacheKey = RedisKey.studyPlan(userId, levelCode, today);

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof TodayPlanResponse plan) {
            return plan;
        }

        TodayPlanResponse plan = buildPlan(userId, levelCode, today);
        redisTemplate.opsForValue().set(cacheKey, plan, PLAN_CACHE_MINUTES, TimeUnit.MINUTES);
        return plan;
    }

    /** 答题后主动失效今日计划缓存。 */
    public void evict(Long userId, String levelCode) {
        String today = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE);
        redisTemplate.delete(RedisKey.studyPlan(userId, levelCode, today));
    }

    // ---- private ----

    private TodayPlanResponse buildPlan(Long userId, String levelCode, String today) {
        UserSettings settings = getSettings(userId);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // 1. 到期复习词
        List<UserWordProgress> dueProgress = progressMapper.findDueForReview(
                userId, levelCode, now, settings.getDailyReviewGoal());

        List<Long> dueWordIds = dueProgress.stream().map(UserWordProgress::getWordId).toList();
        List<WordDetailDto> reviewWords = fetchWords(dueWordIds);

        // 2. 新词候选：排除已学过的词（stage > 0）
        List<Long> learnedIds = progressMapper.findLearnedWordIds(userId, levelCode);
        List<WordBank> newWordBanks = wordBankMapper.findNewWords(
                levelCode, learnedIds, settings.getDefaultSortMode(), settings.getDailyNewWordsGoal());
        List<WordDetailDto> newWords = newWordBanks.stream().map(WordDetailDto::from).toList();

        int totalWords = reviewWords.size() + newWords.size();
        int estimatedMinutes = Math.max(1, (totalWords * SECONDS_PER_WORD) / 60);

        return TodayPlanResponse.builder()
                .date(today)
                .reviewWords(reviewWords)
                .newWords(newWords)
                .reviewCount(reviewWords.size())
                .newCount(newWords.size())
                .estimatedMinutes(estimatedMinutes)
                .build();
    }

    private List<WordDetailDto> fetchWords(List<Long> wordIds) {
        if (wordIds.isEmpty()) return List.of();
        // 批量查询词条详情，保持顺序（IN 查询后按 wordIds 顺序重排）
        var words = wordBankMapper.selectBatchIds(wordIds);
        return words.stream().map(WordDetailDto::from).toList();
    }

    private UserSettings getSettings(Long userId) {
        UserSettings settings = settingsMapper.selectById(userId);
        if (settings == null) {
            // 用户设置不存在时使用默认值
            return UserSettings.builder().userId(userId).build();
        }
        return settings;
    }
}
