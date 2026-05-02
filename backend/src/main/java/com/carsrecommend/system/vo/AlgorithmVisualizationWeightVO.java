package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.util.Map;

public record AlgorithmVisualizationWeightVO(
        Map<String, BigDecimal> subjectiveWeight,
        Map<String, BigDecimal> objectiveWeight,
        Map<String, BigDecimal> finalWeight) {
}
