package com.vocabmaster.checkin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.checkin.entity.Checkin;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CheckinMapper extends BaseMapper<Checkin> {

    @Select("SELECT * FROM checkin WHERE user_id = #{userId} AND checkin_date = #{date} LIMIT 1")
    Checkin findByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("""
        SELECT * FROM checkin
        WHERE user_id = #{userId}
          AND checkin_date >= #{from} AND checkin_date <= #{to}
        ORDER BY checkin_date ASC
    """)
    List<Checkin> findByMonth(@Param("userId") Long userId,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);

    /**
     * 每次答题后累加今日打卡统计（INSERT ... ON DUPLICATE KEY UPDATE）。
     * 依赖 checkin 表上 uk_user_date (user_id, checkin_date) 唯一键。
     */
    @Insert("""
        INSERT INTO checkin (user_id, checkin_date, words_learned, words_reviewed,
                             correct_count, duration_seconds)
        VALUES (#{userId}, #{date}, #{wordsLearned}, #{wordsReviewed},
                #{correctCount}, #{durationSeconds})
        ON DUPLICATE KEY UPDATE
            words_learned    = words_learned    + #{wordsLearned},
            words_reviewed   = words_reviewed   + #{wordsReviewed},
            correct_count    = correct_count    + #{correctCount},
            duration_seconds = duration_seconds + #{durationSeconds}
    """)
    void upsertStats(@Param("userId") Long userId,
                     @Param("date") LocalDate date,
                     @Param("wordsLearned") int wordsLearned,
                     @Param("wordsReviewed") int wordsReviewed,
                     @Param("correctCount") int correctCount,
                     @Param("durationSeconds") int durationSeconds);
}
