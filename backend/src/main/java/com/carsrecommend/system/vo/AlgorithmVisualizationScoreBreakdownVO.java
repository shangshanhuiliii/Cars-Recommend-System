package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.util.List;

public record AlgorithmVisualizationScoreBreakdownVO(
        String dimension,
        String label,
        BigDecimal finalScore,
        String formulaText,
        List<AlgorithmVisualizationMatchedRuleVO> matchedRules,
        String explanation) {
}
