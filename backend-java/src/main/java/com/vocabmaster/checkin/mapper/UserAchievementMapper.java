package com.vocabmaster.checkin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.checkin.entity.UserAchievement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserAchievementMapper extends BaseMapper<UserAchievement> {

    @Select("SELECT achievement_code FROM user_achievement WHERE user_id = #{userId}")
    List<String> findCodesByUserId(@Param("userId") Long userId);
}
