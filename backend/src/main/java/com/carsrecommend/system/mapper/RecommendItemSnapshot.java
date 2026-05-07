package com.carsrecommend.system.mapper;

import java.math.BigDecimal;

public record RecommendItemSnapshot(
        Integer rankNo,
        Long carId,
        String brand,
        String series,
        String modelName,
        BigDecimal guidePrice,
        String bodyType,
        String energyType,
        Integer seats,
        String imageUrl,
        BigDecimal totalScore,
        BigDecimal priceScore,
        BigDecimal spaceScore,
        BigDecimal safetyScore,
        BigDecimal energyScore,
        BigDecimal intelligenceScore,
        BigDecimal comfortScore,
        BigDecimal powerScore,
        BigDecimal reputationScore,
        BigDecimal popularityScore,
        String tags,
        String matchLevel,
        String reasonText,
        String weaknessText) {
}
