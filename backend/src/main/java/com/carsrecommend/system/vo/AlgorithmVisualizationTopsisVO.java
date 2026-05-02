package com.carsrecommend.system.vo;

import java.math.BigDecimal;

public record AlgorithmVisualizationTopsisVO(
        BigDecimal closeness,
        BigDecimal positiveDistance,
        BigDecimal negativeDistance) {
}
