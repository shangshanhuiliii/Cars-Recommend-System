package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.entity.UserDemand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RecommendationWeightService {

    public static final String ALGORITHM_VERSION = "pareto-topsis-v1";

    private static final BigDecimal ALPHA_EXPLICIT = new BigDecimal("0.75");
    private static final BigDecimal ALPHA_SCENE = new BigDecimal("0.60");
    private static final BigDecimal ONE = BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);
    private static final double EPSILON = 0.0001d;
    private static final int WEIGHT_SCALE = 6;

    private final ObjectMapper objectMapper;

    public RecommendationWeightService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RecommendationWeightSnapshot calculate(UserDemand demand, List<RecommendationScoreVector> scoreVectors) {
        Map<String, BigDecimal> subjectiveWeight = subjectiveWeight(demand);
        Map<String, BigDecimal> objectiveWeight = objectiveWeight(scoreVectors, subjectiveWeight);
        BigDecimal alpha = hasExplicitFactorWeight(demand) ? ALPHA_EXPLICIT : ALPHA_SCENE;
        Map<String, BigDecimal> finalWeight = finalWeight(alpha, subjectiveWeight, objectiveWeight);
        return new RecommendationWeightSnapshot(
                ALGORITHM_VERSION,
                alpha,
                subjectiveWeight,
                objectiveWeight,
                finalWeight);
    }

    private Map<String, BigDecimal> subjectiveWeight(UserDemand demand) {
        Map<String, BigDecimal> rawWeights = new LinkedHashMap<>();
        for (RecommendationDimension dimension : RecommendationDimension.ORDERED) {
            rawWeights.put(dimension.key(), safeWeight(dimension.subjectiveWeight(demand)));
        }
        return normalize(rawWeights);
    }

    private Map<String, BigDecimal> objectiveWeight(
            List<RecommendationScoreVector> scoreVectors,
            Map<String, BigDecimal> subjectiveWeight) {
        if (scoreVectors.size() <= 1) {
            return copyWeights(subjectiveWeight);
        }

        Map<String, Double> differences = new LinkedHashMap<>();
        for (RecommendationDimension dimension : RecommendationDimension.ORDERED) {
            double sum = scoreVectors.stream()
                    .mapToDouble(vector -> positiveScore(dimension.score(vector)))
                    .sum();
            if (sum <= 0) {
                differences.put(dimension.key(), 0d);
                continue;
            }

            double entropySum = 0d;
            for (RecommendationScoreVector vector : scoreVectors) {
                double ratio = positiveScore(dimension.score(vector)) / sum;
                entropySum += ratio * Math.log(ratio);
            }
            double entropy = -entropySum / Math.log(scoreVectors.size());
            differences.put(dimension.key(), Math.max(0d, 1d - entropy));
        }

        double differenceSum = differences.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        if (differenceSum <= 0.00000001d) {
            return copyWeights(subjectiveWeight);
        }

        Map<String, BigDecimal> objectiveWeight = new LinkedHashMap<>();
        for (RecommendationDimension dimension : RecommendationDimension.ORDERED) {
            BigDecimal value = BigDecimal.valueOf(differences.get(dimension.key()) / differenceSum);
            objectiveWeight.put(dimension.key(), value);
        }
        return normalize(objectiveWeight);
    }

    private Map<String, BigDecimal> finalWeight(
            BigDecimal alpha,
            Map<String, BigDecimal> subjectiveWeight,
            Map<String, BigDecimal> objectiveWeight) {
        BigDecimal objectiveRatio = BigDecimal.ONE.subtract(alpha);
        Map<String, BigDecimal> combined = new LinkedHashMap<>();
        for (RecommendationDimension dimension : RecommendationDimension.ORDERED) {
            BigDecimal subjective = subjectiveWeight.get(dimension.key()).multiply(alpha);
            BigDecimal objective = objectiveWeight.get(dimension.key()).multiply(objectiveRatio);
            combined.put(dimension.key(), subjective.add(objective));
        }
        return normalize(combined);
    }

    private Map<String, BigDecimal> normalize(Map<String, BigDecimal> rawWeights) {
        BigDecimal sum = rawWeights.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(BigDecimal.ZERO) <= 0) {
            return uniformWeights();
        }

        Map<String, BigDecimal> normalized = new LinkedHashMap<>();
        BigDecimal roundedSum = BigDecimal.ZERO.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
        for (RecommendationDimension dimension : RecommendationDimension.ORDERED) {
            BigDecimal value = rawWeights.get(dimension.key())
                    .divide(sum, WEIGHT_SCALE, RoundingMode.HALF_UP);
            normalized.put(dimension.key(), value);
            roundedSum = roundedSum.add(value);
        }

        BigDecimal diff = ONE.subtract(roundedSum);
        if (diff.signum() != 0) {
            String key = RecommendationDimension.POPULARITY.key();
            normalized.put(key, normalized.get(key).add(diff).setScale(WEIGHT_SCALE, RoundingMode.HALF_UP));
        }
        return normalized;
    }

    private Map<String, BigDecimal> uniformWeights() {
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        BigDecimal value = BigDecimal.ONE.divide(
                BigDecimal.valueOf(RecommendationDimension.ORDERED.size()), WEIGHT_SCALE, RoundingMode.HALF_UP);
        for (RecommendationDimension dimension : RecommendationDimension.ORDERED) {
            weights.put(dimension.key(), value);
        }
        BigDecimal sum = value.multiply(BigDecimal.valueOf(RecommendationDimension.ORDERED.size()));
        BigDecimal diff = ONE.subtract(sum);
        String key = RecommendationDimension.POPULARITY.key();
        weights.put(key, weights.get(key).add(diff).setScale(WEIGHT_SCALE, RoundingMode.HALF_UP));
        return weights;
    }

    private Map<String, BigDecimal> copyWeights(Map<String, BigDecimal> weights) {
        return new LinkedHashMap<>(weights);
    }

    private BigDecimal safeWeight(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private double positiveScore(BigDecimal value) {
        if (value == null) {
            return EPSILON;
        }
        return Math.max(value.doubleValue(), EPSILON);
    }

    private boolean hasExplicitFactorWeight(UserDemand demand) {
        JsonNode node = readJsonNode(demand.getFactorWeights());
        if (!node.isObject()) {
            return false;
        }
        for (RecommendationDimension dimension : RecommendationDimension.ORDERED) {
            JsonNode value = node.path(dimension.key());
            if (value.isNumber() && value.asInt() > 0) {
                return true;
            }
            if (value.isTextual() && StringUtils.hasText(value.asText()) && Integer.parseInt(value.asText()) > 0) {
                return true;
            }
        }
        return false;
    }

    private JsonNode readJsonNode(String json) {
        if (!StringUtils.hasText(json)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isTextual()) {
                node = objectMapper.readTree(node.asText());
            }
            return node;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to parse recommendation weight json field", exception);
        }
    }
}
