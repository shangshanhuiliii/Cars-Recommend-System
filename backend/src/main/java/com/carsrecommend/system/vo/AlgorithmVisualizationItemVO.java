package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AlgorithmVisualizationItemVO(
        Integer rankNo,
        String group,
        Long carId,
        String brand,
        String series,
        String modelName,
        BigDecimal guidePrice,
        String bodyType,
        String energyType,
        String matchLevel,
        String matchLevelLabel,
        BigDecimal totalScore,
        BigDecimal priceScore,
        Map<String, BigDecimal> scores,
        boolean paretoDominated,
        AlgorithmVisualizationTopsisVO topsis,
        Map<String, BigDecimal> contribution,
        Map<String, BigDecimal> gap,
        List<String> tags,
        String reasonText,
        String weaknessText,
        boolean featureScoreSnapshotMismatch,
        boolean featureScoreSourceAvailable,
        String featureScoreSourceNote) {
}
