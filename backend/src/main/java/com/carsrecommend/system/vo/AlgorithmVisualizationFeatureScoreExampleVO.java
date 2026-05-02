package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AlgorithmVisualizationFeatureScoreExampleVO(
        Long carId,
        String brand,
        String modelName,
        BigDecimal guidePrice,
        String bodyType,
        String energyType,
        Integer seats,
        Map<String, Object> params,
        Map<String, BigDecimal> scores,
        List<AlgorithmVisualizationScoreBreakdownVO> scoreBreakdown) {
}
