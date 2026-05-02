package com.carsrecommend.system.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ParetoAnalyzer {

    private static final int KEY_DIMENSION_LIMIT = 4;

    public ParetoResult analyze(
            List<RecommendationScoreVector> scoreVectors,
            Map<String, BigDecimal> finalWeight) {
        List<RecommendationDimension> keyDimensions = keyDimensions(finalWeight);
        List<Boolean> dominatedFlags = new ArrayList<>();
        for (int i = 0; i < scoreVectors.size(); i++) {
            dominatedFlags.add(false);
        }

        for (int candidateIndex = 0; candidateIndex < scoreVectors.size(); candidateIndex++) {
            for (int comparedIndex = 0; comparedIndex < scoreVectors.size(); comparedIndex++) {
                if (candidateIndex == comparedIndex) {
                    continue;
                }
                if (dominates(
                        scoreVectors.get(candidateIndex),
                        scoreVectors.get(comparedIndex),
                        keyDimensions)) {
                    dominatedFlags.set(comparedIndex, true);
                }
            }
        }

        return new ParetoResult(
                keyDimensions.stream().map(RecommendationDimension::key).toList(),
                List.copyOf(dominatedFlags));
    }

    List<RecommendationDimension> keyDimensions(Map<String, BigDecimal> finalWeight) {
        return RecommendationDimension.ORDERED.stream()
                .sorted(Comparator.comparing(
                        (RecommendationDimension dimension) -> safeWeight(finalWeight.get(dimension.key())),
                        Comparator.reverseOrder()))
                .limit(KEY_DIMENSION_LIMIT)
                .toList();
    }

    private boolean dominates(
            RecommendationScoreVector candidate,
            RecommendationScoreVector compared,
            List<RecommendationDimension> keyDimensions) {
        boolean hasBetterDimension = false;
        for (RecommendationDimension dimension : keyDimensions) {
            int scoreCompare = score(dimension.score(candidate)).compareTo(score(dimension.score(compared)));
            if (scoreCompare < 0) {
                return false;
            }
            if (scoreCompare > 0) {
                hasBetterDimension = true;
            }
        }
        return hasBetterDimension;
    }

    private BigDecimal score(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal safeWeight(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record ParetoResult(
            List<String> keyDimensions,
            List<Boolean> dominatedFlags) {
    }
}
