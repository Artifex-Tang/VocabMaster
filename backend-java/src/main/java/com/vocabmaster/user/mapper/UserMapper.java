package com.vocabmaster.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM `user` WHERE uuid = #{uuid} AND deleted_at IS NULL LIMIT 1")
    User findByUuid(@Param("uuid") String uuid);

    @Select("SELECT * FROM `user` WHERE email = #{email} AND deleted_at IS NULL LIMIT 1")
    User findByEmail(@Param("email") String email);

    @Select("SELECT * FROM `user` WHERE phone_hash = #{phoneHash} AND deleted_at IS NULL LIMIT 1")
    User findByPhoneHash(@Param("phoneHash") String phoneHash);
}
