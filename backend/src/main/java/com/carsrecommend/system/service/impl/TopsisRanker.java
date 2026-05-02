package com.carsrecommend.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TopsisRanker {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int SCORE_SCALE = 2;
    private static final double ZERO_TOLERANCE = 0.00000001d;

    public List<BigDecimal> rank(
            List<RecommendationScoreVector> scoreVectors,
            Map<String, BigDecimal> finalWeight,
            List<BigDecimal> fallbackScores) {
        return analyze(scoreVectors, finalWeight, fallbackScores).items().stream()
                .map(TopsisItemResult::totalScore)
                .toList();
    }

    public TopsisRankingResult analyze(
            List<RecommendationScoreVector> scoreVectors,
            Map<String, BigDecimal> finalWeight,
            List<BigDecimal> fallbackScores) {
        if (scoreVectors.isEmpty()) {
            return new TopsisRankingResult(List.of());
        }

        double[][] normalizedMatrix = normalizedMatrix(scoreVectors);
        double[][] weightedMatrix = weightedMatrix(normalizedMatrix, finalWeight);
        double[] positiveIdeal = positiveIdeal(weightedMatrix);
        double[] negativeIdeal = negativeIdeal(weightedMatrix);

        List<TopsisItemResult> items = new ArrayList<>();
        for (int i = 0; i < scoreVectors.size(); i++) {
            double positiveDistance = distance(weightedMatrix[i], positiveIdeal);
            double negativeDistance = distance(weightedMatrix[i], negativeIdeal);
            double distanceSum = positiveDistance + negativeDistance;
            BigDecimal totalScore;
            if (distanceSum <= ZERO_TOLERANCE) {
                totalScore = normalizeScore(fallbackScores.get(i));
            } else {
                totalScore = normalizeScore(BigDecimal.valueOf(negativeDistance / distanceSum)
                        .multiply(ONE_HUNDRED));
            }
            items.add(new TopsisItemResult(
                    totalScore,
                    toScoreMap(normalizedMatrix[i]),
                    toScoreMap(weightedMatrix[i]),
                    toScoreMap(positiveIdeal)));
        }
        return new TopsisRankingResult(List.copyOf(items));
    }

    private double[][] normalizedMatrix(List<RecommendationScoreVector> scoreVectors) {
        int rowCount = scoreVectors.size();
        int columnCount = RecommendationDimension.ORDERED.size();
        double[][] normalizedMatrix = new double[rowCount][columnCount];

        for (int column = 0; column < columnCount; column++) {
            RecommendationDimension dimension = RecommendationDimension.ORDERED.get(column);
            double squareSum = 0d;
            for (RecommendationScoreVector vector : scoreVectors) {
                double score = score(dimension.score(vector));
                squareSum += score * score;
            }
            double denominator = Math.sqrt(squareSum);
            for (int row = 0; row < rowCount; row++) {
                if (denominator <= ZERO_TOLERANCE) {
                    normalizedMatrix[row][column] = 0d;
                } else {
                    normalizedMatrix[row][column] = score(dimension.score(scoreVectors.get(row))) / denominator;
                }
            }
        }
        return normalizedMatrix;
    }

    private double[][] weightedMatrix(double[][] normalizedMatrix, Map<String, BigDecimal> finalWeight) {
        int rowCount = normalizedMatrix.length;
        int columnCount = RecommendationDimension.ORDERED.size();
        double[][] weightedMatrix = new double[rowCount][columnCount];
        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                RecommendationDimension dimension = RecommendationDimension.ORDERED.get(column);
                weightedMatrix[row][column] = normalizedMatrix[row][column]
                        * weight(finalWeight.get(dimension.key()));
            }
        }
        return weightedMatrix;
    }

    private double[] positiveIdeal(double[][] weightedMatrix) {
        int columnCount = RecommendationDimension.ORDERED.size();
        double[] positiveIdeal = new double[columnCount];
        for (int column = 0; column < columnCount; column++) {
            positiveIdeal[column] = Double.NEGATIVE_INFINITY;
            for (double[] row : weightedMatrix) {
                positiveIdeal[column] = Math.max(positiveIdeal[column], row[column]);
            }
        }
        return positiveIdeal;
    }

    private double[] negativeIdeal(double[][] weightedMatrix) {
        int columnCount = RecommendationDimension.ORDERED.size();
        double[] negativeIdeal = new double[columnCount];
        for (int column = 0; column < columnCount; column++) {
            negativeIdeal[column] = Double.POSITIVE_INFINITY;
            for (double[] row : weightedMatrix) {
                negativeIdeal[column] = Math.min(negativeIdeal[column], row[column]);
            }
        }
        return negativeIdeal;
    }

    private double distance(double[] values, double[] ideal) {
        double squareSum = 0d;
        for (int index = 0; index < values.length; index++) {
            double diff = values[index] - ideal[index];
            squareSum += diff * diff;
        }
        return Math.sqrt(squareSum);
    }

    private double score(BigDecimal value) {
        return value == null ? 0d : value.doubleValue();
    }

    private double weight(BigDecimal value) {
        return value == null ? 0d : value.doubleValue();
    }

    private BigDecimal normalizeScore(BigDecimal value) {
        if (value == null) {
            value = BigDecimal.ZERO;
        }
        return value.max(BigDecimal.ZERO)
                .min(ONE_HUNDRED)
                .setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> toScoreMap(double[] values) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int index = 0; index < RecommendationDimension.ORDERED.size(); index++) {
            result.put(
                    RecommendationDimension.ORDERED.get(index).key(),
                    BigDecimal.valueOf(values[index]).setScale(8, RoundingMode.HALF_UP));
        }
        return result;
    }

    public record TopsisRankingResult(List<TopsisItemResult> items) {
    }

    public record TopsisItemResult(
            BigDecimal totalScore,
            Map<String, BigDecimal> normalizedScore,
            Map<String, BigDecimal> weightedNormalizedScore,
            Map<String, BigDecimal> positiveIdeal) {
    }
}
