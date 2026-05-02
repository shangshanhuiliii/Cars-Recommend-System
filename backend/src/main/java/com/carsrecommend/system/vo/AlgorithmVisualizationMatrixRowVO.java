package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.util.Map;

public record AlgorithmVisualizationMatrixRowVO(
        Integer rankNo,
        Long carId,
        String carName,
        String matchLevel,
        Map<String, BigDecimal> scores) {
}
