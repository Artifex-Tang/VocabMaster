package com.vocabmaster.sync.dto;

import com.vocabmaster.study.dto.AnswerResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SyncPushResponse {

    private LocalDateTime serverTs;
    private int answersAccepted;
    private int answersSkipped;
    private int checkinsUpserted;
    private List<AnswerResponse> answerResults;
}
