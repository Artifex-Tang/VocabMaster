package com.vocabmaster.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocabmaster.admin.dto.AdminUserDto;
import com.vocabmaster.admin.dto.DashboardResponse;
import com.vocabmaster.checkin.mapper.CheckinMapper;
import com.vocabmaster.common.constant.AppConstants;
import com.vocabmaster.common.exception.BizException;
import com.vocabmaster.common.result.ErrorCode;
import com.vocabmaster.common.result.PageResult;
import com.vocabmaster.study.mapper.StudyLogMapper;
import com.vocabmaster.user.entity.User;
import com.vocabmaster.user.mapper.UserMapper;
import com.vocabmaster.word.entity.WordBank;
import com.vocabmaster.word.mapper.WordBankMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final WordBankMapper wordBankMapper;
    private final StudyLogMapper studyLogMapper;
    private final CheckinMapper checkinMapper;

    public DashboardResponse dashboard() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();

        long totalUsers = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .isNull(User::getDeletedAt));

        long newUsersToday = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .ge(User::getCreatedAt, todayStart)
                .lt(User::getCreatedAt, tomorrow));

        long dauToday = studyLogMapper.countDau(todayStart, tomorrow);

        long totalWords = wordBankMapper.selectCount(Wrappers.<WordBank>lambdaQuery()
                .eq(WordBank::getAuditStatus, AppConstants.AUDIT_PASS));

        long totalCheckins = checkinMapper.selectCount(null);

        return DashboardResponse.builder()
                .date(today.toString())
                .totalUsers(totalUsers)
                .newUsersToday(newUsersToday)
                .dauToday(dauToday)
                .totalWords(totalWords)
                .totalCheckins(totalCheckins)
                .build();
    }

    public PageResult<AdminUserDto> listUsers(String keyword, int page, int pageSize) {
        var p = new Page<User>(page, pageSize);
        var wrapper = Wrappers.<User>lambdaQuery().isNull(User::getDeletedAt);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getEmail, keyword).or().like(User::getNickname, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);
        var result = userMapper.selectPage(p, wrapper);
        return PageResult.of(result, AdminUserDto::from);
    }

    @Transactional
    public void setUserStatus(String uuid, int status) {
        User user = userMapper.findByUuid(uuid);
        if (user == null) throw new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Transactional
    public void auditWord(Long wordId, int auditStatus) {
        WordBank wb = wordBankMapper.selectById(wordId);
        if (wb == null) throw new BizException(ErrorCode.WORD_NOT_FOUND);
        wb.setAuditStatus(auditStatus);
        wordBankMapper.updateById(wb);
    }

    public PageResult<WordBank> listWords(String levelCode, String keyword, int page, int pageSize) {
        var p = new Page<WordBank>(page, pageSize);
        var wrapper = Wrappers.<WordBank>lambdaQuery();
        if (levelCode != null && !levelCode.isBlank()) wrapper.eq(WordBank::getLevelCode, levelCode);
        if (keyword != null && !keyword.isBlank()) wrapper.like(WordBank::getWordLower, keyword.toLowerCase());
        wrapper.orderByDesc(WordBank::getId);
        return PageResult.of(wordBankMapper.selectPage(p, wrapper), w -> w);
    }

    private long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0L; }
    }
}
