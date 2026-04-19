package com.vocabmaster.word.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocabmaster.word.entity.WordBank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface WordBankMapper extends BaseMapper<WordBank> {

    /** 全文搜索（word_lower LIKE），走 idx_level_word_lower */
    IPage<WordBank> search(Page<WordBank> page,
                           @Param("levelCode") String levelCode,
                           @Param("keyword") String keyword);

    /**
     * 取该等级中用户尚未学过的词，用于今日计划的"新词"部分。
     * excludeWordIds 为空时返回所有未学词。
     */
    List<WordBank> findNewWords(@Param("levelCode") String levelCode,
                                @Param("excludeIds") Collection<Long> excludeIds,
                                @Param("sortMode") String sortMode,
                                @Param("limit") int limit);

    /**
     * 为选择题生成干扰项：同等级、同主题、排除已知 wordId 的随机 N 条。
     */
    List<WordBank> pickDistractors(@Param("levelCode") String levelCode,
                                   @Param("topicCode") String topicCode,
                                   @Param("excludeId") Long excludeId,
                                   @Param("limit") int limit);

    /** 批量下载某等级全量词库（离线用），支持增量（since 可为 null） */
    List<WordBank> downloadByLevel(@Param("levelCode") String levelCode,
                                   @Param("since") java.time.LocalDateTime since);
}
