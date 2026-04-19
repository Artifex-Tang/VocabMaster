package com.vocabmaster.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.vocabmaster.auth.service.VerificationCodeService;
import com.vocabmaster.common.exception.BizException;
import com.vocabmaster.common.result.ErrorCode;
import com.vocabmaster.study.entity.StudyLog;
import com.vocabmaster.study.mapper.StudyLogMapper;
import com.vocabmaster.user.dto.UpdateProfileRequest;
import com.vocabmaster.user.dto.UpdateSettingsRequest;
import com.vocabmaster.user.dto.UserProfileDto;
import com.vocabmaster.user.entity.User;
import com.vocabmaster.user.entity.UserAuth;
import com.vocabmaster.user.entity.UserSettings;
import com.vocabmaster.user.mapper.UserAuthMapper;
import com.vocabmaster.user.mapper.UserMapper;
import com.vocabmaster.user.mapper.UserSettingsMapper;
import com.vocabmaster.util.PhoneCryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;
    private final UserSettingsMapper settingsMapper;
    private final StudyLogMapper studyLogMapper;
    private final VerificationCodeService verificationCodeService;
    private final PhoneCryptoUtil phoneCryptoUtil;

    public UserProfileDto getProfile(Long userId) {
        User user = requireUser(userId);

        // 解密手机号用于脱敏展示
        String phoneMasked = null;
        if (user.getPhone() != null) {
            try {
                String plain = phoneCryptoUtil.decrypt(user.getPhone());
                phoneMasked = maskPhone(plain);
            } catch (Exception ignored) {}
        }

        List<String> providers = userAuthMapper.selectList(
                Wrappers.<UserAuth>lambdaQuery().eq(UserAuth::getUserId, userId))
                .stream().map(UserAuth::getProvider).toList();

        return UserProfileDto.from(user, phoneMasked, providers);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest req) {
        User user = requireUser(userId);

        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
        if (req.getTimezone() != null) user.setTimezone(req.getTimezone());
        if (req.getLocale() != null) user.setLocale(req.getLocale());

        userMapper.updateById(user);
        return getProfile(userId);
    }

    public UserSettings getSettings(Long userId) {
        UserSettings s = settingsMapper.selectById(userId);
        return s != null ? s : UserSettings.builder().userId(userId).build();
    }

    @Transactional
    public UserSettings updateSettings(Long userId, UpdateSettingsRequest req) {
        UserSettings s = settingsMapper.selectById(userId);
        boolean isNew = (s == null);
        if (isNew) {
            s = UserSettings.builder().userId(userId).build();
        }

        if (req.getDailyNewWordsGoal() != null) s.setDailyNewWordsGoal(req.getDailyNewWordsGoal());
        if (req.getDailyReviewGoal() != null) s.setDailyReviewGoal(req.getDailyReviewGoal());
        if (req.getDefaultSortMode() != null) s.setDefaultSortMode(req.getDefaultSortMode());
        if (req.getPreferredAccent() != null) s.setPreferredAccent(req.getPreferredAccent());
        if (req.getAutoPlayAudio() != null) s.setAutoPlayAudio(req.getAutoPlayAudio() ? 1 : 0);
        if (req.getTheme() != null) s.setTheme(req.getTheme());
        if (req.getActiveLevels() != null) s.setActiveLevels(req.getActiveLevels());
        if (req.getNotificationTime() != null) {
            try {
                s.setNotificationTime(LocalTime.parse(req.getNotificationTime()));
            } catch (DateTimeParseException ignored) {}
        }

        if (isNew) settingsMapper.insert(s);
        else settingsMapper.updateById(s);

        return s;
    }

    /**
     * 导出学习记录为 CSV 字符串（直接内联生成，适合 MVP 量级）。
     */
    public String exportCsv(Long userId) {
        List<StudyLog> logs = studyLogMapper.findByDateRange(
                userId,
                LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.now(ZoneOffset.UTC).plusDays(1));

        StringJoiner csv = new StringJoiner("\n");
        csv.add("word_id,level_code,action,result,mode,stage_before,stage_after,duration_ms,client_ts,created_at");
        for (StudyLog l : logs) {
            csv.add(String.join(",",
                    str(l.getWordId()), str(l.getLevelCode()), str(l.getAction()),
                    str(l.getResult()), str(l.getMode()),
                    str(l.getStageBefore()), str(l.getStageAfter()),
                    str(l.getDurationMs()), str(l.getClientTs()), str(l.getCreatedAt())));
        }
        return csv.toString();
    }

    /**
     * 注销账户（软删除）。需提供验证码二次确认。
     * identifier 为用户的 email 或手机号（解密后）。
     */
    @Transactional
    public void deleteAccount(Long userId, String confirmCode) {
        User user = requireUser(userId);

        // 优先用 email 验证；没有 email 时用手机号
        String identifier = user.getEmail();
        String type = "email";
        if (identifier == null && user.getPhone() != null) {
            try {
                identifier = phoneCryptoUtil.decrypt(user.getPhone());
                type = "phone";
            } catch (Exception ignored) {}
        }
        if (identifier == null) throw new BizException(ErrorCode.PARAM_INVALID, "账号未绑定邮箱或手机号");

        verificationCodeService.verifyAndConsume(type, identifier, "delete_account", confirmCode);

        user.setStatus(0);
        userMapper.deleteById(user.getId()); // 触发逻辑删除（deletedAt = now(3)）
    }

    // ---- private ----

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        return user;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String str(Object v) {
        return v == null ? "" : v.toString();
    }
}
