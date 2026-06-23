package com.vocabmaster.wordlist.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_list_subscription")
public class UserListSubscription {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long listId;
    @Builder.Default
    private Integer currentUnitNo = 1;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime subscribedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
