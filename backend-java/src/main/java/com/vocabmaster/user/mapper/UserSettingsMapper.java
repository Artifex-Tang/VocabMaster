package com.vocabmaster.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.user.entity.UserSettings;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserSettingsMapper extends BaseMapper<UserSettings> {
}
