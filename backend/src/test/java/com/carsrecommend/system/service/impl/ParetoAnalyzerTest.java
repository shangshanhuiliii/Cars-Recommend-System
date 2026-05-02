package com.carsrecommend.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ParetoAnalyzerTest {

    private final ParetoAnalyzer analyzer = new ParetoAnalyzer();

    @Test
    void usesTopFourFinalWeightsAsKeyDimensions() {
        ParetoAnalyzer.ParetoResult result = analyzer.analyze(
                List.of(vector(90, 90, 90, 90, 50)),
                weights(
                        "price", "0.10",
                        "space", "0.30",
                        "safety", "0.20",
                        "energy", "0.05",
                        "intelligence", "0.03",
                        "comfort", "0.25",
                        "power", "0.02",
                        "reputation", "0.04",
                        "popularity", "0.01"));

        assertEquals(List.of("space", "comfort", "safety", "price"), result.keyDimensions());
    }

    @Test
    void marksDominatedCandidatesWithoutRemovingThem() {
        ParetoAnalyzer.ParetoResult result = analyzer.analyze(
                List.of(
                        vector(90, 88, 86, 84, 20),
                        vector(80, 88, 70, 84, 99),
                        vector(95, 70, 90, 80, 10)),
                weights(
                        "price", "0.30",
                        "space", "0.25",
                        "safety", "0.20",
                        "energy", "0.15",
                        "intelligence", "0.03",
                        "comfort", "0.02",
                        "power", "0.02",
                        "reputation", "0.02",
                        "popularity", "0.01"));

        assertEquals(3, result.dominatedFlags().size());
        assertEquals(List.of(false, true, false), result.dominatedFlags());
    }

    @Test
    void nonKeyDimensionAdvantageDoesNotCreateDominance() {
        ParetoAnalyzer.ParetoResult result = analyzer.analyze(
                List.of(
                        vector(90, 80, 80, 80, 10),
                        vector(90, 80, 80, 80, 99)),
                weights(
                        "price", "0.30",
                        "space", "0.25",
                        "safety", "0.20",
                        "energy", "0.15",
                        "intelligence", "0.03",
                        "comfort", "0.02",
                        "power", "0.02",
                        "reputation", "0.02",
                        "popularity", "0.01"));

        assertEquals(List.of(false, false), result.dominatedFlags());
    }

    private RecommendationScoreVector vector(
            int price,
            int space,
            int safety,
            int energy,
            int popularity) {
        return new RecommendationScoreVector(
                score(price),
                score(space),
                score(safety),
                score(energy),
                score(60),
                score(60),
                score(60),
                score(60),
                score(popularity));
    }

    private BigDecimal score(int value) {
        return new BigDecimal(value).setScale(2);
    }

    private Map<String, BigDecimal> weights(String... values) {
        java.util.LinkedHashMap<String, BigDecimal> weights = new java.util.LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            weights.put(values[index], new BigDecimal(values[index + 1]));
        }
        return weights;
    }
}
