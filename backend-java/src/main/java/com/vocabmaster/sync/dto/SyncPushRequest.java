package com.vocabmaster.sync.dto;

import com.vocabmaster.study.dto.AnswerRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SyncPushRequest {

    @NotBlank
    private String deviceId;

    private List<AnswerRequest> answers;
    private List<SyncCheckinItem> checkins;
}
