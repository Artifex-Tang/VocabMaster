package com.vocabmaster.sync.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sync_token")
public class SyncToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String deviceId;

    /** web / miniprogram / android */
    private String deviceType;

    private LocalDateTime lastSyncAt;
    private LocalDateTime lastPushAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
