package com.vocabmaster.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.study.entity.StudyLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface StudyLogMapper extends BaseMapper<StudyLog> {

    /**
     * 查询用户某时间段的学习日志（走 idx_user_created）。
     * 用于统计模块的今日/周报/月报。
     */
    List<StudyLog> findByDateRange(@Param("userId") Long userId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    /**
     * 按日期汇总学习统计（新学词数、复习数、正确率、时长）。
     * 返回 Map 列表：date / words_learned / words_reviewed / correct / total / duration_ms
     */
    List<Map<String, Object>> dailySummary(@Param("userId") Long userId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    /**
     * 查询某单词的全部复习历史（遗忘曲线用）。
     */
    List<StudyLog> findByUserAndWord(@Param("userId") Long userId,
                                     @Param("wordId") Long wordId);
}
