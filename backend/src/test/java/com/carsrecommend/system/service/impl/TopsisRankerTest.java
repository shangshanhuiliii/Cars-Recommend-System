package com.carsrecommend.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TopsisRankerTest {

    private final TopsisRanker ranker = new TopsisRanker();

    @Test
    void ranksByRelativeClosenessAndReturnsScoreWithinRange() {
        List<BigDecimal> scores = ranker.rank(
                List.of(
                        vector(95, 90, 88, 86),
                        vector(60, 70, 72, 68),
                        vector(80, 82, 80, 78)),
                weights(),
                List.of(score("91.23"), score("72.34"), score("81.11")));

        assertEquals(3, scores.size());
        assertTrue(scores.get(0).compareTo(scores.get(2)) > 0);
        assertTrue(scores.get(2).compareTo(scores.get(1)) > 0);
        for (BigDecimal score : scores) {
            assertTrue(score.compareTo(BigDecimal.ZERO) >= 0);
            assertTrue(score.compareTo(new BigDecimal("100")) <= 0);
            assertEquals(2, score.scale());
        }
    }

    @Test
    void singleCandidateFallsBackToWeightedUtilityScore() {
        List<BigDecimal> scores = ranker.rank(
                List.of(vector(80, 80, 80, 80)),
                weights(),
                List.of(score("86.789")));

        assertEquals(List.of(expectedScore("86.79")), scores);
    }

    @Test
    void indistinguishableCandidatesFallBackToWeightedUtilityScores() {
        List<BigDecimal> scores = ranker.rank(
                List.of(
                        vector(80, 80, 80, 80),
                        vector(80, 80, 80, 80)),
                weights(),
                List.of(score("75.555"), score("66.661")));

        assertEquals(List.of(expectedScore("75.56"), expectedScore("66.66")), scores);
    }

    private RecommendationScoreVector vector(int price, int space, int safety, int energy) {
        return new RecommendationScoreVector(
                score(price),
                score(space),
                score(safety),
                score(energy),
                score(60),
                score(60),
                score(60),
                score(60),
                score(60));
    }

    private BigDecimal score(int value) {
        return new BigDecimal(value).setScale(2);
    }

    private BigDecimal score(String value) {
        return new BigDecimal(value);
    }

    private BigDecimal expectedScore(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> weights() {
        return Map.of(
                "price", new BigDecimal("0.25"),
                "space", new BigDecimal("0.20"),
                "safety", new BigDecimal("0.18"),
                "energy", new BigDecimal("0.12"),
                "intelligence", new BigDecimal("0.08"),
                "comfort", new BigDecimal("0.07"),
                "power", new BigDecimal("0.04"),
                "reputation", new BigDecimal("0.04"),
                "popularity", new BigDecimal("0.02"));
    }
}
