package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DemandTextParseVO(
        Long userId,
        String rawText,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        List<String> bodyTypes,
        List<String> energyTypes,
        Integer minSeats,
        List<String> scenes,
        Map<String, Integer> factorWeights,
        List<String> excludedBrands,
        List<Long> excludedCarIds,
        String profileText,
        List<String> unsupportedTerms,
        List<String> ambiguousTerms,
        BigDecimal confidenceScore) {
}
