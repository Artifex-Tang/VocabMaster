package com.vocabmaster.checkin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.vocabmaster.checkin.dto.AchievementsResponse;
import com.vocabmaster.checkin.dto.CalendarResponse;
import com.vocabmaster.checkin.dto.CheckinResponse;
import com.vocabmaster.checkin.entity.*;
import com.vocabmaster.checkin.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckinService {

    private final CheckinMapper checkinMapper;
    private final UserStreakMapper userStreakMapper;
    private final AchievementMapper achievementMapper;
    private final UserAchievementMapper userAchievementMapper;

    /**
     * 今日打卡（幂等）。若已打卡直接返回当前状态。
     */
    @Transactional
    public CheckinResponse checkIn(Long userId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // 幂等：今日已打卡则直接返回
        Checkin existing = checkinMapper.findByUserAndDate(userId, today);
        UserStreak streak = getOrCreateStreak(userId);

        if (existing != null) {
            return buildResponse(today, streak, List.of());
        }

        // 创建今日打卡记录
        Checkin checkin = Checkin.builder()
                .userId(userId)
                .checkinDate(today)
                .build();
        checkinMapper.insert(checkin);

        // 更新连续天数
        List<CheckinResponse.NewAchievement> newAchievements = updateStreak(userId, streak, today);

        return buildResponse(today, streak, newAchievements);
    }

    /** 打卡日历（按月）。 */
    public CalendarResponse calendar(Long userId, String monthStr) {
        YearMonth ym = monthStr != null ? YearMonth.parse(monthStr) : YearMonth.now(ZoneOffset.UTC);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<Checkin> checkins = checkinMapper.findByMonth(userId, from, to);
        Map<LocalDate, Checkin> checkinMap = checkins.stream()
                .collect(Collectors.toMap(Checkin::getCheckinDate, c -> c));

        List<CalendarResponse.DayEntry> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            Checkin c = checkinMap.get(d);
            days.add(CalendarResponse.DayEntry.builder()
                    .date(d.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .checkedIn(c != null)
                    .wordsCount(c != null ? c.getWordsLearned() + c.getWordsReviewed() : 0)
                    .build());
        }

        UserStreak streak = getOrCreateStreak(userId);
        return CalendarResponse.builder()
                .month(ym.toString())
                .days(days)
                .currentStreak(streak.getCurrentStreak())
                .longestStreak(streak.getLongestStreak())
                .build();
    }

    /** 成就列表（已解锁 + 未解锁）。 */
    public AchievementsResponse achievements(Long userId) {
        List<Achievement> all = achievementMapper.selectList(
                Wrappers.<Achievement>lambdaQuery().orderByAsc(Achievement::getSortOrder));

        Set<String> unlockedCodes = new HashSet<>(userAchievementMapper.findCodesByUserId(userId));

        Map<String, LocalDateTime> achievedAtMap = userAchievementMapper.selectList(
                Wrappers.<UserAchievement>lambdaQuery().eq(UserAchievement::getUserId, userId))
                .stream().collect(Collectors.toMap(
                        UserAchievement::getAchievementCode,
                        UserAchievement::getAchievedAt));

        List<AchievementsResponse.Unlocked> unlocked = new ArrayList<>();
        List<AchievementsResponse.Locked> locked = new ArrayList<>();

        for (Achievement a : all) {
            if (unlockedCodes.contains(a.getCode())) {
                unlocked.add(AchievementsResponse.Unlocked.builder()
                        .code(a.getCode())
                        .nameZh(a.getNameZh())
                        .icon(a.getIcon())
                        .achievedAt(achievedAtMap.get(a.getCode()))
                        .build());
            } else {
                locked.add(AchievementsResponse.Locked.builder()
                        .code(a.getCode())
                        .nameZh(a.getNameZh())
                        .icon(a.getIcon())
                        .build());
            }
        }

        return AchievementsResponse.builder()
                .unlocked(unlocked)
                .locked(locked)
                .build();
    }

    // ---- private ----

    private UserStreak getOrCreateStreak(Long userId) {
        UserStreak streak = userStreakMapper.selectById(userId);
        if (streak == null) {
            streak = UserStreak.builder().userId(userId).build();
            userStreakMapper.insert(streak);
        }
        return streak;
    }

    /**
     * 更新连续打卡天数，返回本次新解锁的成就列表。
     * 规则：昨天打过卡 → streak+1；否则重置为 1。
     */
    private List<CheckinResponse.NewAchievement> updateStreak(Long userId,
                                                               UserStreak streak,
                                                               LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        boolean consecutive = yesterday.equals(streak.getLastCheckinDate());

        int newStreak = consecutive ? streak.getCurrentStreak() + 1 : 1;
        int longest = Math.max(streak.getLongestStreak(), newStreak);

        streak.setCurrentStreak(newStreak);
        streak.setLongestStreak(longest);
        streak.setLastCheckinDate(today);
        streak.setTotalDays(streak.getTotalDays() + 1);
        userStreakMapper.updateById(streak);

        return checkAndGrantAchievements(userId, newStreak, streak.getTotalDays() + 1);
    }

    /** 检查并发放打卡相关成就（streak 里程碑）。 */
    private List<CheckinResponse.NewAchievement> checkAndGrantAchievements(
            Long userId, int currentStreak, int totalDays) {

        Set<String> alreadyUnlocked = new HashSet<>(userAchievementMapper.findCodesByUserId(userId));
        List<Achievement> all = achievementMapper.selectList(
                Wrappers.<Achievement>lambdaQuery().eq(Achievement::getCategory, "streak"));

        List<CheckinResponse.NewAchievement> newOnes = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        for (Achievement a : all) {
            if (alreadyUnlocked.contains(a.getCode())) continue;

            int threshold = parseStreakThreshold(a.getTriggerRule());
            if (threshold > 0 && currentStreak >= threshold) {
                UserAchievement ua = UserAchievement.builder()
                        .userId(userId)
                        .achievementCode(a.getCode())
                        .achievedAt(now)
                        .build();
                userAchievementMapper.insert(ua);
                newOnes.add(CheckinResponse.NewAchievement.builder()
                        .code(a.getCode())
                        .nameZh(a.getNameZh())
                        .build());
            }
        }
        return newOnes;
    }

    /** 解析 trigger_rule JSON 中的 streak 阈值，如 {"streak":7} → 7。 */
    private int parseStreakThreshold(String triggerRule) {
        if (triggerRule == null) return 0;
        try {
            // 简单解析，避免引入额外依赖
            int idx = triggerRule.indexOf("\"streak\"");
            if (idx < 0) return 0;
            String after = triggerRule.substring(idx + 8).replaceAll("[^0-9]", " ").trim();
            String[] parts = after.split("\\s+");
            return parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private CheckinResponse buildResponse(LocalDate date, UserStreak streak,
                                           List<CheckinResponse.NewAchievement> newAchievements) {
        return CheckinResponse.builder()
                .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .currentStreak(streak.getCurrentStreak())
                .longestStreak(streak.getLongestStreak())
                .totalDays(streak.getTotalDays())
                .newAchievements(newAchievements)
                .build();
    }
}
