package com.vocabmaster.checkin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.checkin.entity.Checkin;
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
}
