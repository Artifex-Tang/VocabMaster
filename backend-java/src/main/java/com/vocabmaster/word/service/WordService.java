package com.vocabmaster.word.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocabmaster.common.constant.AppConstants;
import com.vocabmaster.common.exception.BizException;
import com.vocabmaster.common.result.ErrorCode;
import com.vocabmaster.common.result.PageResult;
import com.vocabmaster.word.dto.WordDetailDto;
import com.vocabmaster.word.dto.WordDownloadResponse;
import com.vocabmaster.word.dto.WordSummaryDto;
import com.vocabmaster.word.entity.WordBank;
import com.vocabmaster.word.mapper.WordBankMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordService {

    private final WordBankMapper wordBankMapper;

    public WordDetailDto getById(Long id) {
        WordBank wb = wordBankMapper.selectById(id);
        if (wb == null || wb.getDeletedAt() != null) {
            throw new BizException(ErrorCode.WORD_NOT_FOUND);
        }
        return WordDetailDto.from(wb);
    }

    public WordDetailDto getByLevelAndWord(String levelCode, String word) {
        WordBank wb = wordBankMapper.selectOne(
                Wrappers.<WordBank>lambdaQuery()
                        .eq(WordBank::getLevelCode, levelCode)
                        .eq(WordBank::getWordLower, word.toLowerCase())
                        .eq(WordBank::getAuditStatus, AppConstants.AUDIT_PASS));
        if (wb == null) throw new BizException(ErrorCode.WORD_NOT_FOUND);
        return WordDetailDto.from(wb);
    }

    public PageResult<WordSummaryDto> search(String levelCode, String keyword,
                                             int page, int pageSize) {
        pageSize = Math.min(pageSize, AppConstants.MAX_PAGE_SIZE);
        var p = new Page<WordBank>(page, pageSize);
        var result = wordBankMapper.search(p,
                (levelCode == null || levelCode.isBlank()) ? null : levelCode,
                keyword == null ? null : keyword.toLowerCase());
        return PageResult.of(result, WordSummaryDto::from);
    }

    public WordDownloadResponse download(String levelCode, String since) {
        validateLevelCode(levelCode);

        LocalDateTime sinceTs = null;
        if (since != null && !since.isBlank()) {
            // 支持 yyyyMMdd 或 ISO 8601 两种格式
            sinceTs = parseSince(since);
        }

        List<WordBank> words = wordBankMapper.downloadByLevel(levelCode, sinceTs);
        List<WordDetailDto> dtos = words.stream().map(WordDetailDto::from).toList();

        return WordDownloadResponse.builder()
                .levelCode(levelCode)
                .version(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .total(dtos.size())
                .words(dtos)
                .build();
    }

    // ---- private ----

    private void validateLevelCode(String levelCode) {
        if (levelCode == null || levelCode.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "level 参数不能为空");
        }
    }

    private LocalDateTime parseSince(String since) {
        try {
            if (since.length() == 8) {
                // yyyyMMdd
                return LocalDateTime.parse(since + "000000",
                        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            }
            return LocalDateTime.parse(since, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "since 格式不正确，支持 yyyyMMdd 或 ISO 8601");
        }
    }
}
