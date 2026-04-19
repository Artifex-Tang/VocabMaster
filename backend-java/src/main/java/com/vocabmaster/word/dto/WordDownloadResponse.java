package com.vocabmaster.word.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WordDownloadResponse {

    private String levelCode;
    /** 数据版本号，格式 yyyyMMdd */
    private String version;
    private int total;
    private List<WordDetailDto> words;
}
