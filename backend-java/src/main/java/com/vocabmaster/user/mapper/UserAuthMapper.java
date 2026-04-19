package com.vocabmaster.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.user.entity.UserAuth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAuthMapper extends BaseMapper<UserAuth> {

    @Select("SELECT * FROM user_auth WHERE provider = #{provider} AND provider_user_id = #{providerUserId} LIMIT 1")
    UserAuth findByProviderAndId(@Param("provider") String provider,
                                 @Param("providerUserId") String providerUserId);
}
