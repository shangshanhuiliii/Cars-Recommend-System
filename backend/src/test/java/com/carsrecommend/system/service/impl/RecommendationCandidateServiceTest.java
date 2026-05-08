package com.carsrecommend.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carsrecommend.system.common.enums.MatchLevel;
import com.carsrecommend.system.entity.CarFeatureScore;
import com.carsrecommend.system.entity.CarModel;
import com.carsrecommend.system.entity.UserDemand;
import com.carsrecommend.system.mapper.CarFeatureScoreMapper;
import com.carsrecommend.system.mapper.CarModelMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RecommendationCandidateServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void strictBudgetRequiresFullBudgetRangeAndRelaxBudgetKeepsNearbyOutOfRangeCarsOnly() {
        RecommendationCandidateGroups groups = generate(
                demand(new BigDecimal("200000"), new BigDecimal("210000")),
                List.of(
                        car(1L, "179999", "SUV", "插混"),
                        car(2L, "180000", "SUV", "插混"),
                        car(3L, "190000", "SUV", "插混"),
                        car(4L, "199999", "SUV", "插混"),
                        car(5L, "200000", "SUV", "插混"),
                        car(6L, "205000", "SUV", "插混"),
                        car(7L, "210000", "SUV", "插混"),
                        car(8L, "210001", "SUV", "插混"),
                        car(9L, "231000", "SUV", "插混"),
                        car(10L, "231001", "SUV", "插混"),
                        car(11L, "190000", "MPV", "插混")));

        assertEquals(List.of(5L, 6L, 7L), carIds(groups.strictCandidates()));
        assertAllStrictPricesInRange(groups.strictCandidates(), new BigDecimal("200000"), new BigDecimal("210000"));

        Map<Long, MatchLevel> recommendationLevels = matchLevelsByCarId(groups.recommendationCandidates());
        assertEquals(MatchLevel.RELAX_BUDGET, recommendationLevels.get(2L));
        assertEquals(MatchLevel.RELAX_BUDGET, recommendationLevels.get(3L));
        assertEquals(MatchLevel.RELAX_BUDGET, recommendationLevels.get(4L));
        assertEquals(MatchLevel.RELAX_BUDGET, recommendationLevels.get(8L));
        assertEquals(MatchLevel.RELAX_BUDGET, recommendationLevels.get(9L));
        assertFalse(MatchLevel.RELAX_BUDGET.equals(recommendationLevels.get(1L)));
        assertFalse(MatchLevel.RELAX_BUDGET.equals(recommendationLevels.get(10L)));
        assertFalse(MatchLevel.RELAX_BUDGET.equals(recommendationLevels.get(11L)));
    }

    @Test
    void maxOnlyBudgetUsesUpperBoundForStrictAndUpperRelaxWindowForRelaxBudget() {
        RecommendationCandidateGroups groups = generate(
                demand(null, new BigDecimal("210000")),
                List.of(
                        car(1L, "199999", "SUV", "插混"),
                        car(2L, "210000", "SUV", "插混"),
                        car(3L, "210001", "SUV", "插混"),
                        car(4L, "231000", "SUV", "插混"),
                        car(5L, "231001", "SUV", "插混")));

        assertEquals(List.of(1L, 2L), carIds(groups.strictCandidates()));
        assertAllStrictPricesAtMost(groups.strictCandidates(), new BigDecimal("210000"));

        Map<Long, MatchLevel> recommendationLevels = matchLevelsByCarId(groups.recommendationCandidates());
        assertEquals(MatchLevel.RELAX_BUDGET, recommendationLevels.get(3L));
        assertEquals(MatchLevel.RELAX_BUDGET, recommendationLevels.get(4L));
        assertFalse(MatchLevel.RELAX_BUDGET.equals(recommendationLevels.get(5L)));
    }

    @Test
    void minOnlyBudgetUsesLowerBoundForStrictAndLowerRelaxWindowForRelaxBudget() {
        RecommendationCandidateGroups groups = generate(
                demand(new BigDecimal("200000"), null),
                List.of(
                        car(1L, "179999", "SUV", "插混"),
                        car(2L, "180000", "SUV", "插混"),
                        car(3L, "199999", "SUV", "插混"),
                        car(4L, "200000", "SUV", "插混"),
                        car(5L, "210000", "SUV", "插混")));

        assertEquals(List.of(4L, 5L), carIds(groups.strictCandidates()));
        assertAllStrictPricesAtLeast(groups.strictCandidates(), new BigDecimal("200000"));

        Map<Long, MatchLevel> recommendationLevels = matchLevelsByCarId(groups.recommendationCandidates());
        assertEquals(MatchLevel.RELAX_BUDGET, recommendationLevels.get(2L));
        assertEquals(MatchLevel.RELAX_BUDGET, recommendationLevels.get(3L));
        assertFalse(MatchLevel.RELAX_BUDGET.equals(recommendationLevels.get(1L)));
    }

    @Test
    void positiveBrandAndSeatOptionsFilterCandidatesBeforeRelaxedGroups() {
        UserDemand demand = demand(null, null);
        demand.setBrands("[\"BrandA\"]");
        demand.setSeatOptions("[\"4\",\"7_PLUS\"]");
        demand.setMinSeats(7);

        RecommendationCandidateGroups groups = generate(
                demand,
                List.of(
                        car(1L, "180000", "SUV", "插混", "BrandA", 4),
                        car(2L, "180000", "SUV", "插混", "BrandB", 4),
                        car(3L, "180000", "SUV", "插混", "BrandA", 7),
                        car(4L, "180000", "SUV", "插混", "BrandA", 6),
                        car(5L, "180000", "SUV", "插混", "BrandA", 8)));

        assertEquals(List.of(1L, 3L, 5L), carIds(groups.strictCandidates()));
        assertTrue(groups.recommendationCandidates().isEmpty());
    }

    private RecommendationCandidateGroups generate(UserDemand demand, List<CarModel> cars) {
        CarModelMapper carModelMapper = mock(CarModelMapper.class);
        CarFeatureScoreMapper scoreMapper = mock(CarFeatureScoreMapper.class);
        Map<Long, CarFeatureScore> scores = cars.stream()
                .collect(Collectors.toMap(CarModel::getId, car -> score(car.getId())));
        when(carModelMapper.findApprovedRecommendationCandidates()).thenReturn(cars);
        when(scoreMapper.findByCarId(anyLong())).thenAnswer(invocation -> {
            Long carId = invocation.getArgument(0);
            return Optional.ofNullable(scores.get(carId));
        });
        RecommendationCandidateService service =
                new RecommendationCandidateService(carModelMapper, scoreMapper, objectMapper);
        return service.generateCandidates(demand);
    }

    private UserDemand demand(BigDecimal budgetMin, BigDecimal budgetMax) {
        UserDemand demand = new UserDemand();
        demand.setBudgetMin(budgetMin);
        demand.setBudgetMax(budgetMax);
        demand.setBodyTypes("[\"SUV\"]");
        demand.setEnergyTypes("[\"插混\"]");
        demand.setMinSeats(5);
        demand.setExcludedBrands("[]");
        demand.setExcludedCarIds("[]");
        return demand;
    }

    private CarModel car(Long id, String guidePrice, String bodyType, String energyType) {
        return car(id, guidePrice, bodyType, energyType, "Brand" + id, 5);
    }

    private CarModel car(Long id, String guidePrice, String bodyType, String energyType, String brand, int seats) {
        CarModel car = new CarModel();
        car.setId(id);
        car.setBrand(brand);
        car.setSeries("Series" + id);
        car.setModelName("Model" + id);
        car.setGuidePrice(new BigDecimal(guidePrice));
        car.setBodyType(bodyType);
        car.setEnergyType(energyType);
        car.setSeats(seats);
        return car;
    }

    private CarFeatureScore score(Long carId) {
        CarFeatureScore score = new CarFeatureScore();
        score.setCarId(carId);
        score.setSpaceScore(new BigDecimal("80"));
        score.setSafetyScore(new BigDecimal("80"));
        score.setEnergyScore(new BigDecimal("80"));
        score.setIntelligenceScore(new BigDecimal("80"));
        score.setComfortScore(new BigDecimal("80"));
        score.setPowerScore(new BigDecimal("80"));
        score.setReputationScore(new BigDecimal("80"));
        score.setPopularityScore(new BigDecimal("80"));
        return score;
    }

    private List<Long> carIds(List<RecommendationCandidate> candidates) {
        return candidates.stream().map(candidate -> candidate.car().getId()).toList();
    }

    private Map<Long, MatchLevel> matchLevelsByCarId(List<RecommendationCandidate> candidates) {
        return candidates.stream()
                .collect(Collectors.toMap(candidate -> candidate.car().getId(), RecommendationCandidate::matchLevel));
    }

    private void assertAllStrictPricesInRange(
            List<RecommendationCandidate> candidates,
            BigDecimal budgetMin,
            BigDecimal budgetMax) {
        for (RecommendationCandidate candidate : candidates) {
            assertTrue(candidate.car().getGuidePrice().compareTo(budgetMin) >= 0);
            assertTrue(candidate.car().getGuidePrice().compareTo(budgetMax) <= 0);
        }
    }

    private void assertAllStrictPricesAtMost(List<RecommendationCandidate> candidates, BigDecimal budgetMax) {
        for (RecommendationCandidate candidate : candidates) {
            assertTrue(candidate.car().getGuidePrice().compareTo(budgetMax) <= 0);
        }
    }

    private void assertAllStrictPricesAtLeast(List<RecommendationCandidate> candidates, BigDecimal budgetMin) {
        for (RecommendationCandidate candidate : candidates) {
            assertTrue(candidate.car().getGuidePrice().compareTo(budgetMin) >= 0);
        }
    }
}
