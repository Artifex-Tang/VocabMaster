package com.vocabmaster.wrongword.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocabmaster.common.result.PageResult;
import com.vocabmaster.study.entity.WrongWord;
import com.vocabmaster.study.mapper.WrongWordMapper;
import com.vocabmaster.word.dto.WordDetailDto;
import com.vocabmaster.word.entity.WordBank;
import com.vocabmaster.word.mapper.WordBankMapper;
import com.vocabmaster.wrongword.dto.WrongWordDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WrongWordService {

    private final WrongWordMapper wrongWordMapper;
    private final WordBankMapper wordBankMapper;

    public PageResult<WrongWordDto> list(Long userId, String levelCode,
                                          Integer resolved, int page, int pageSize) {
        var p = new Page<WrongWord>(page, pageSize);
        var result = wrongWordMapper.findByUserAndLevel(p, userId, levelCode, resolved);
        return PageResult.of(result, WrongWordDto::from);
    }

    /**
     * 启动错题复习：返回未 resolved 的错词详情列表（供前端渲染学习卡片）。
     * 实际答题走 /study/answer 接口，同 study 模块流程。
     */
    public List<WordDetailDto> reviewWords(Long userId, String levelCode, int limit) {
        var p = new Page<WrongWord>(1, limit);
        var result = wrongWordMapper.findByUserAndLevel(p, userId, levelCode, 0);

        List<Long> wordIds = result.getRecords().stream().map(WrongWord::getWordId).toList();
        if (wordIds.isEmpty()) return List.of();

        List<WordBank> banks = wordBankMapper.selectBatchIds(wordIds);
        return banks.stream().map(WordDetailDto::from).toList();
    }

    /** 手动标记为已解决。 */
    @Transactional
    public void resolve(Long userId, Long wordId) {
        WrongWord ww = wrongWordMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<WrongWord>lambdaQuery()
                        .eq(WrongWord::getUserId, userId)
                        .eq(WrongWord::getWordId, wordId));
        if (ww == null) return;
        ww.setResolved(1);
        wrongWordMapper.updateById(ww);
    }
}
