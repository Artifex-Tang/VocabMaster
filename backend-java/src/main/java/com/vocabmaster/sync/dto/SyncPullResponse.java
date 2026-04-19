package com.vocabmaster.sync.dto;

import com.vocabmaster.checkin.entity.Checkin;
import com.vocabmaster.study.entity.UserWordProgress;
import com.vocabmaster.user.entity.UserSettings;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SyncPullResponse {

    /** 本次拉取的服务端时间戳，下次 pull 时用作 since */
    private LocalDateTime serverTs;

    private Changes changes;

    @Data
    @Builder
    public static class Changes {
        private List<UserWordProgress> progress;
        private List<Checkin> checkin;
        private UserSettings settings;
    }
}
