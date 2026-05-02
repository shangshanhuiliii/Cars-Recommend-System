package com.carsrecommend.system.service.impl;

import java.math.BigDecimal;
import java.util.Map;

public record RecommendationWeightSnapshot(
        String algorithmVersion,
        BigDecimal alpha,
        Map<String, BigDecimal> subjectiveWeight,
        Map<String, BigDecimal> objectiveWeight,
        Map<String, BigDecimal> finalWeight) {
}
