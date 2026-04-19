package com.vocabmaster.sync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.sync.entity.SyncToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SyncTokenMapper extends BaseMapper<SyncToken> {

    @Select("SELECT * FROM sync_token WHERE user_id = #{userId} AND device_id = #{deviceId} LIMIT 1")
    SyncToken findByUserAndDevice(@Param("userId") Long userId, @Param("deviceId") String deviceId);
}
