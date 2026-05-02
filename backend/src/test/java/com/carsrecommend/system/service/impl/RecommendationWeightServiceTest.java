package com.carsrecommend.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.carsrecommend.system.entity.UserDemand;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecommendationWeightServiceTest {

    private final RecommendationWeightService service = new RecommendationWeightService(new ObjectMapper());

    @Test
    void explicitFactorWeightsUseSubjectiveAlphaAndNormalizeFinalWeight() {
        UserDemand demand = demand("""
                {
                  "price": 0,
                  "space": 8,
                  "safety": 2,
                  "energy": 0,
                  "intelligence": 0,
                  "comfort": 0,
                  "power": 0,
                  "reputation": 0,
                  "popularity": 0
                }
                """);
        demand.setWeightSpace(new BigDecimal("0.8000"));
        demand.setWeightSafety(new BigDecimal("0.2000"));

        RecommendationWeightSnapshot snapshot = service.calculate(demand, variedVectors());

        assertEquals("pareto-topsis-v1", snapshot.algorithmVersion());
        assertEquals(new BigDecimal("0.75"), snapshot.alpha());
        assertEquals(new BigDecimal("0.800000"), snapshot.subjectiveWeight().get("space"));
        assertEquals(new BigDecimal("0.200000"), snapshot.subjectiveWeight().get("safety"));
        assertWeightSumIsOne(snapshot.finalWeight());
    }

    @Test
    void sceneWeightsUseSceneAlphaAndSingleCandidateFallsBackToSubjectiveObjectiveWeight() {
        UserDemand demand = demand("{}");
        demand.setWeightPrice(new BigDecimal("0.1500"));
        demand.setWeightSpace(new BigDecimal("0.1300"));
        demand.setWeightSafety(new BigDecimal("0.1500"));
        demand.setWeightEnergy(new BigDecimal("0.1300"));
        demand.setWeightIntelligence(new BigDecimal("0.1200"));
        demand.setWeightComfort(new BigDecimal("0.1200"));
        demand.setWeightPower(new BigDecimal("0.0800"));
        demand.setWeightReputation(new BigDecimal("0.0700"));
        demand.setWeightPopularity(new BigDecimal("0.0500"));

        RecommendationWeightSnapshot snapshot = service.calculate(demand, List.of(vector(90, 80, 70)));

        assertEquals(new BigDecimal("0.60"), snapshot.alpha());
        assertEquals(snapshot.subjectiveWeight(), snapshot.objectiveWeight());
        assertEquals(snapshot.subjectiveWeight(), snapshot.finalWeight());
        assertWeightSumIsOne(snapshot.finalWeight());
    }

    @Test
    void entropyObjectiveWeightFavorsDimensionsWithHigherCandidateVariation() {
        UserDemand demand = demand("{}");
        demand.setWeightPrice(new BigDecimal("0.5000"));
        demand.setWeightSpace(new BigDecimal("0.5000"));

        RecommendationWeightSnapshot snapshot = service.calculate(demand, List.of(
                vector(20, 60, 60),
                vector(50, 60, 60),
                vector(90, 60, 60)));

        assertTrue(snapshot.objectiveWeight().get("price")
                .compareTo(snapshot.objectiveWeight().get("space")) > 0);
        assertWeightSumIsOne(snapshot.objectiveWeight());
        assertWeightSumIsOne(snapshot.finalWeight());
    }

    private UserDemand demand(String factorWeights) {
        UserDemand demand = new UserDemand();
        demand.setFactorWeights(factorWeights);
        demand.setWeightPrice(BigDecimal.ZERO);
        demand.setWeightSpace(BigDecimal.ZERO);
        demand.setWeightSafety(BigDecimal.ZERO);
        demand.setWeightEnergy(BigDecimal.ZERO);
        demand.setWeightIntelligence(BigDecimal.ZERO);
        demand.setWeightComfort(BigDecimal.ZERO);
        demand.setWeightPower(BigDecimal.ZERO);
        demand.setWeightReputation(BigDecimal.ZERO);
        demand.setWeightPopularity(BigDecimal.ZERO);
        return demand;
    }

    private List<RecommendationScoreVector> variedVectors() {
        return List.of(
                vector(90, 85, 80),
                vector(80, 70, 60),
                vector(70, 65, 50));
    }

    private RecommendationScoreVector vector(int price, int space, int safety) {
        return new RecommendationScoreVector(
                score(price),
                score(space),
                score(safety),
                score(60),
                score(60),
                score(60),
                score(60),
                score(60),
                score(60));
    }

    private BigDecimal score(int value) {
        return new BigDecimal(value).setScale(2);
    }

    private void assertWeightSumIsOne(Map<String, BigDecimal> weights) {
        BigDecimal sum = weights.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("1.000000").compareTo(sum));
    }
}
