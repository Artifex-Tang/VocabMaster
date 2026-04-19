package com.vocabmaster.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.study.entity.UserWordProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserWordProgressMapper extends BaseMapper<UserWordProgress> {

    /**
     * 查询到期需复习的单词（核心查询，走 idx_user_next_review 索引）。
     * stage 在 1-8 之间（stage=9 且 mastered 后 next_review_at 是 30 天后，
     * 到期后也会出现在复习队列直到答对置 mastered）。
     */
    @Select("""
        SELECT * FROM user_word_progress
        WHERE user_id = #{userId}
          AND level_code = #{levelCode}
          AND stage > 0 AND stage < 9
          AND next_review_at <= #{now}
        ORDER BY next_review_at ASC
        LIMIT #{limit}
    """)
    List<UserWordProgress> findDueForReview(@Param("userId") Long userId,
                                            @Param("levelCode") String levelCode,
                                            @Param("now") LocalDateTime now,
                                            @Param("limit") int limit);

    /**
     * 查询该用户该等级所有已学词的 word_id（stage > 0），
     * 用于计算新词候选集（差集）。
     */
    @Select("""
        SELECT word_id FROM user_word_progress
        WHERE user_id = #{userId} AND level_code = #{levelCode} AND stage > 0
    """)
    List<Long> findLearnedWordIds(@Param("userId") Long userId,
                                  @Param("levelCode") String levelCode);

    /** 查询某等级各阶段分布（stats 用） */
    @Select("""
        SELECT stage, COUNT(*) AS cnt
        FROM user_word_progress
        WHERE user_id = #{userId} AND level_code = #{levelCode}
        GROUP BY stage
    """)
    List<java.util.Map<String, Object>> countByStage(@Param("userId") Long userId,
                                                     @Param("levelCode") String levelCode);

    /** 跨等级汇总 mastered/learning 分布（周报/月报 level_breakdown 用）。 */
    @Select("""
        SELECT level_code,
               SUM(CASE WHEN stage = 9 THEN 1 ELSE 0 END) AS mastered,
               SUM(CASE WHEN stage > 0 AND stage < 9 THEN 1 ELSE 0 END) AS learning
        FROM user_word_progress
        WHERE user_id = #{userId}
        GROUP BY level_code
    """)
    List<java.util.Map<String, Object>> levelSummary(@Param("userId") Long userId);
}
