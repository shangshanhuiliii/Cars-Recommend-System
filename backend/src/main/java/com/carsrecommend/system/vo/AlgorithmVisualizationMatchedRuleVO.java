package com.carsrecommend.system.vo;

import java.math.BigDecimal;

public record AlgorithmVisualizationMatchedRuleVO(
        String ruleName,
        BigDecimal delta,
        String reason) {
}
