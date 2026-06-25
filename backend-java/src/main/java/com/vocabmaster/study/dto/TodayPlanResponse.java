package com.vocabmaster.study.dto;

import com.vocabmaster.word.dto.WordDetailDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 今日学习计划响应。会缓存进 Redis（10 分钟），经无类型信息的 JSON 序列化器存取 +
 * {@code convertValue} 还原，故须 {@code @NoArgsConstructor}（{@code @Builder} 单用只生成全参构造器，
 * 无法被 Jackson 构造）。嵌套 {@link WordDetailDto} 同理已补全。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayPlanResponse {

    private String date;
    private List<WordDetailDto> reviewWords;
    private List<WordDetailDto> newWords;
    private int reviewCount;
    private int newCount;
    private int estimatedMinutes;
}
