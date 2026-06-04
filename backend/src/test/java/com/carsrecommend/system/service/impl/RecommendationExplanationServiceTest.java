package com.carsrecommend.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecommendationExplanationServiceTest {

    private final RecommendationExplanationService service = new RecommendationExplanationService();

    @Test
    void reasonTextComesFromHighContributionDimensions() {
        RecommendationExplanationService.Explanation explanation = service.generate(
                vector(85, 95, 72, 60, 70, 92, 65, 80, 78),
                result(
                        score("88.00"),
                        weighted(
                                "price", "0.0500",
                                "space", "0.2400",
                                "safety", "0.0600",
                                "energy", "0.0300",
                                "intelligence", "0.0500",
                                "comfort", "0.1900",
                                "power", "0.0200",
                                "reputation", "0.0700",
                                "popularity", "0.0400"),
                        weighted(
                                "price", "0.0600",
                                "space", "0.2500",
                                "safety", "0.0700",
                                "energy", "0.0600",
                                "intelligence", "0.0600",
                                "comfort", "0.2000",
                                "power", "0.0700",
                                "reputation", "0.0800",
                                "popularity", "0.0500")),
                weights());

        assertTrue(explanation.reasonText().contains("空间表现"));
        assertTrue(explanation.reasonText().contains("舒适性表现"));
        assertFalse(explanation.reasonText().contains("。；"));
        assertFalse(explanation.reasonText().endsWith("。"));
        assertFalse(explanation.reasonText().contains("TOPSIS"));
    }

    @Test
    void weaknessTextComesFromHighWeightLargeGapDimensions() {
        RecommendationExplanationService.Explanation explanation = service.generate(
                vector(82, 88, 58, 90, 72, 76, 70, 80, 75),
                result(
                        score("62.00"),
                        weighted(
                                "price", "0.0500",
                                "space", "0.1200",
                                "safety", "0.0500",
                                "energy", "0.1100",
                                "intelligence", "0.0600",
                                "comfort", "0.0500",
                                "power", "0.0300",
                                "reputation", "0.0400",
                                "popularity", "0.0200"),
                        weighted(
                                "price", "0.0600",
                                "space", "0.1300",
                                "safety", "0.1800",
                                "energy", "0.1150",
                                "intelligence", "0.0900",
                                "comfort", "0.0600",
                                "power", "0.0400",
                                "reputation", "0.0500",
                                "popularity", "0.0300")),
                weights());

        assertTrue(explanation.weaknessText().contains("安全配置"));
        assertFalse(explanation.weaknessText().contains("。；"));
        assertFalse(explanation.weaknessText().endsWith("。"));
        assertFalse(explanation.weaknessText().contains("理想解"));
    }

    @Test
    void weaknessTextUsesDefaultWhenNoObviousShortcoming() {
        Map<String, BigDecimal> ideal = weighted(
                "price", "0.0900",
                "space", "0.1200",
                "safety", "0.1000",
                "energy", "0.0700",
                "intelligence", "0.0600",
                "comfort", "0.0800",
                "power", "0.0500",
                "reputation", "0.0400",
                "popularity", "0.0300");

        RecommendationExplanationService.Explanation explanation = service.generate(
                vector(85, 86, 84, 83, 82, 85, 81, 84, 80),
                result(score("85.00"), ideal, ideal),
                weights());

        assertTrue(explanation.weaknessText().contains("暂无明显短板"));
        assertFalse(explanation.weaknessText().endsWith("。"));
    }

    @Test
    void tagsDoNotContainTechnicalStatusWords() {
        RecommendationExplanationService.Explanation explanation = service.generate(
                vector(92, 90, 88, 86, 84, 82, 80, 88, 91),
                result(
                        score("90.00"),
                        weighted(
                                "price", "0.0900",
                                "space", "0.1200",
                                "safety", "0.1000",
                                "energy", "0.0700",
                                "intelligence", "0.0600",
                                "comfort", "0.0800",
                                "power", "0.0500",
                                "reputation", "0.0400",
                                "popularity", "0.0300"),
                        weighted(
                                "price", "0.1000",
                                "space", "0.1300",
                                "safety", "0.1100",
                                "energy", "0.0800",
                                "intelligence", "0.0700",
                                "comfort", "0.0900",
                                "power", "0.0600",
                                "reputation", "0.0500",
                                "popularity", "0.0400")),
                weights());

        assertFalse(containsAny(explanation.tags(), List.of(
                "完全匹配",
                "降级推荐",
                "放宽预算",
                "放宽车型",
                "放宽动力",
                "相似推荐",
                "STRICT",
                "RELAX_BUDGET",
                "RELAX_BODY_TYPE",
                "RELAX_ENERGY_TYPE",
                "SIMILAR_RECOMMEND",
                "TOPSIS",
                "Pareto")));
    }

    private RecommendationScoreVector vector(
            int price,
            int space,
            int safety,
            int energy,
            int intelligence,
            int comfort,
            int power,
            int reputation,
            int popularity) {
        return new RecommendationScoreVector(
                score(price),
                score(space),
                score(safety),
                score(energy),
                score(intelligence),
                score(comfort),
                score(power),
                score(reputation),
                score(popularity));
    }

    private TopsisRanker.TopsisItemResult result(
            BigDecimal totalScore,
            Map<String, BigDecimal> weightedNormalizedScore,
            Map<String, BigDecimal> positiveIdeal) {
        return new TopsisRanker.TopsisItemResult(
                totalScore,
                weightedNormalizedScore,
                weightedNormalizedScore,
                positiveIdeal);
    }

    private Map<String, BigDecimal> weights() {
        return weighted(
                "price", "0.10",
                "space", "0.22",
                "safety", "0.20",
                "energy", "0.14",
                "intelligence", "0.08",
                "comfort", "0.12",
                "power", "0.05",
                "reputation", "0.05",
                "popularity", "0.04");
    }

    private Map<String, BigDecimal> weighted(String... values) {
        java.util.LinkedHashMap<String, BigDecimal> result = new java.util.LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(values[index], new BigDecimal(values[index + 1]));
        }
        return result;
    }

    private BigDecimal score(int value) {
        return new BigDecimal(value).setScale(2);
    }

    private BigDecimal score(String value) {
        return new BigDecimal(value);
    }

    private boolean containsAny(List<String> values, List<String> forbiddenValues) {
        for (String value : values) {
            if (forbiddenValues.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
