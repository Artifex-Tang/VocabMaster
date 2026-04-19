package com.vocabmaster.stats.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ForgettingCurveResponse {

    private Long wordId;
    private String word;
    private List<ReviewPoint> reviews;
    private TheoreticalCurve theoreticalCurve;

    @Data
    @Builder
    public static class TheoreticalCurve {
        private String type;
        /** 各阶段间隔（小时） */
        private double[] stages;
    }
}
