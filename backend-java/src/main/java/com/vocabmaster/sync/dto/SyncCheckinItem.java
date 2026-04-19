package com.vocabmaster.sync.dto;

import lombok.Data;

import java.time.LocalDate;

/** 离线打卡记录，随 /sync/push 上报。 */
@Data
public class SyncCheckinItem {

    private LocalDate checkinDate;
    private Integer wordsLearned;
    private Integer wordsReviewed;
    private Integer correctCount;
    private Integer durationSeconds;
}
