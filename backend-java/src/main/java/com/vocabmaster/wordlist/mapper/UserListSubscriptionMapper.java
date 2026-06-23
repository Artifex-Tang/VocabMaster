package com.vocabmaster.wordlist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.wordlist.entity.UserListSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserListSubscriptionMapper extends BaseMapper<UserListSubscription> {

    /** 订阅 upsert：不存在则建（current_unit=1），存在则只刷新 updated_at。 */
    @Update("""
        INSERT INTO user_list_subscription (user_id, list_id, current_unit_no, subscribed_at, updated_at)
        VALUES (#{userId}, #{listId}, 1, NOW(3), NOW(3))
        ON DUPLICATE KEY UPDATE updated_at = NOW(3)
    """)
    int upsertSubscribe(@Param("userId") Long userId, @Param("listId") Long listId);

    /** 推进单元游标。 */
    @Update("""
        UPDATE user_list_subscription
        SET current_unit_no = #{unitNo}, updated_at = NOW(3)
        WHERE user_id = #{userId} AND list_id = #{listId}
    """)
    int advanceUnit(@Param("userId") Long userId,
                    @Param("listId") Long listId,
                    @Param("unitNo") Integer unitNo);

    @Select("""
        SELECT * FROM user_list_subscription
        WHERE user_id = #{userId} AND list_id = #{listId}
    """)
    UserListSubscription find(@Param("userId") Long userId, @Param("listId") Long listId);
}
