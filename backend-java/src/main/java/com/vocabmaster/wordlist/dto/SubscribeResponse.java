package com.vocabmaster.wordlist.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscribeResponse {
    private Long listId;
    private Integer currentUnitNo;
}
