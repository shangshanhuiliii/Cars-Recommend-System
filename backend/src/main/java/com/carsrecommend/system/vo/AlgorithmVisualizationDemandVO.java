package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AlgorithmVisualizationDemandVO(
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        List<String> brands,
        List<String> bodyTypes,
        List<String> energyTypes,
        List<String> seatOptions,
        List<String> scenes,
        Map<String, Integer> factorWeights,
        Integer minSeats,
        List<String> excludedBrands,
        List<Long> excludedCarIds,
        String profileText) {
}
