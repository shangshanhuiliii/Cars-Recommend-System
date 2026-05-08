package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.common.enums.MatchLevel;
import com.carsrecommend.system.entity.CarFeatureScore;
import com.carsrecommend.system.entity.CarModel;
import com.carsrecommend.system.entity.UserDemand;
import com.carsrecommend.system.mapper.CarFeatureScoreMapper;
import com.carsrecommend.system.mapper.CarModelMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class RecommendationCandidateService {

    private static final BigDecimal BUDGET_RELAX_LOWER_RATIO = new BigDecimal("0.90");
    private static final BigDecimal BUDGET_RELAX_UPPER_RATIO = new BigDecimal("1.10");

    private final CarModelMapper carModelMapper;
    private final CarFeatureScoreMapper carFeatureScoreMapper;
    private final ObjectMapper objectMapper;

    public RecommendationCandidateService(
            CarModelMapper carModelMapper,
            CarFeatureScoreMapper carFeatureScoreMapper,
            ObjectMapper objectMapper) {
        this.carModelMapper = carModelMapper;
        this.carFeatureScoreMapper = carFeatureScoreMapper;
        this.objectMapper = objectMapper;
    }

    public RecommendationCandidateGroups generateCandidates(UserDemand demand) {
        List<CandidateCar> candidates = loadCandidatesWithScores();
        List<RecommendationCandidate> strictCandidates = new ArrayList<>();
        List<RecommendationCandidate> recommendationCandidates = new ArrayList<>();
        Set<Long> addedCarIds = new HashSet<>();

        addStageRecommendations(candidates, demand, MatchLevel.STRICT, strictCandidates, addedCarIds);
        for (MatchLevel matchLevel : List.of(
                MatchLevel.RELAX_BUDGET,
                MatchLevel.RELAX_BODY_TYPE,
                MatchLevel.RELAX_ENERGY_TYPE,
                MatchLevel.SIMILAR_RECOMMEND)) {
            addStageRecommendations(candidates, demand, matchLevel, recommendationCandidates, addedCarIds);
        }

        return new RecommendationCandidateGroups(strictCandidates, recommendationCandidates);
    }

    private List<CandidateCar> loadCandidatesWithScores() {
        List<CandidateCar> candidates = new ArrayList<>();
        for (CarModel car : carModelMapper.findApprovedRecommendationCandidates()) {
            carFeatureScoreMapper.findByCarId(car.getId())
                    .ifPresent(score -> candidates.add(new CandidateCar(car, score)));
        }
        return candidates;
    }

    private void addStageRecommendations(
            List<CandidateCar> candidates,
            UserDemand demand,
            MatchLevel matchLevel,
            List<RecommendationCandidate> resultItems,
            Set<Long> addedCarIds) {
        for (CandidateCar candidate : candidates) {
            Long carId = candidate.car().getId();
            if (addedCarIds.contains(carId) || !matchesDemand(candidate.car(), demand, matchLevel)) {
                continue;
            }
            resultItems.add(new RecommendationCandidate(candidate.car(), candidate.featureScore(), matchLevel));
            addedCarIds.add(carId);
        }
    }

    private boolean matchesDemand(CarModel car, UserDemand demand, MatchLevel matchLevel) {
        if (!matchesCommonFilters(car, demand)) {
            return false;
        }
        return switch (matchLevel) {
            case STRICT -> matchesStrictFilters(car, demand);
            case RELAX_BUDGET -> matchesRelaxBudgetFilters(car, demand);
            case RELAX_BODY_TYPE -> matchesRelaxBodyTypeFilters(car, demand);
            case RELAX_ENERGY_TYPE -> matchesRelaxEnergyTypeFilters(car, demand);
            case SIMILAR_RECOMMEND -> true;
        };
    }

    private boolean matchesCommonFilters(CarModel car, UserDemand demand) {
        Set<String> brands = new HashSet<>(readStringList(demand.getBrands()));
        if (!brands.isEmpty() && !brands.contains(car.getBrand())) {
            return false;
        }
        Set<String> excludedBrands = new HashSet<>(readStringList(demand.getExcludedBrands()));
        if (excludedBrands.contains(car.getBrand())) {
            return false;
        }
        Set<Long> excludedCarIds = new HashSet<>(readLongList(demand.getExcludedCarIds()));
        if (excludedCarIds.contains(car.getId())) {
            return false;
        }
        Set<String> seatOptions = new LinkedHashSet<>(readStringList(demand.getSeatOptions()));
        if (!seatOptions.isEmpty()) {
            return matchesSeatOptions(car.getSeats(), seatOptions);
        }
        if (demand.getMinSeats() != null && (car.getSeats() == null || car.getSeats() < demand.getMinSeats())) {
            return false;
        }
        return true;
    }

    private boolean matchesStrictFilters(CarModel car, UserDemand demand) {
        return matchesStrictBudget(car, demand)
                && matchesStrictBodyType(car, demand)
                && matchesStrictEnergyType(car, demand);
    }

    private boolean matchesRelaxBudgetFilters(CarModel car, UserDemand demand) {
        if (demand.getBudgetMin() == null && demand.getBudgetMax() == null) {
            return false;
        }
        BigDecimal guidePrice = car.getGuidePrice();
        if (guidePrice == null || matchesStrictBudget(car, demand)) {
            return false;
        }
        return matchesRelaxedBudgetRange(guidePrice, demand)
                && matchesStrictBodyType(car, demand)
                && matchesStrictEnergyType(car, demand);
    }

    private boolean matchesRelaxBodyTypeFilters(CarModel car, UserDemand demand) {
        Set<String> strictBodyTypes = demandBodyTypes(demand);
        if (strictBodyTypes.isEmpty()) {
            return false;
        }
        return matchesStrictBudget(car, demand)
                && relaxedBodyTypes(strictBodyTypes).contains(car.getBodyType())
                && matchesStrictEnergyType(car, demand);
    }

    private boolean matchesRelaxEnergyTypeFilters(CarModel car, UserDemand demand) {
        Set<String> strictEnergyTypes = expandedDemandEnergyTypes(demand);
        if (strictEnergyTypes.isEmpty()) {
            return false;
        }
        return matchesStrictBudget(car, demand)
                && matchesStrictBodyType(car, demand)
                && relaxedEnergyTypes(readStringList(demand.getEnergyTypes()), strictEnergyTypes).contains(car.getEnergyType());
    }

    private boolean matchesStrictBudget(CarModel car, UserDemand demand) {
        BigDecimal guidePrice = car.getGuidePrice();
        if (guidePrice == null) {
            return false;
        }
        if (demand.getBudgetMin() != null && guidePrice.compareTo(demand.getBudgetMin()) < 0) {
            return false;
        }
        if (demand.getBudgetMax() != null && guidePrice.compareTo(demand.getBudgetMax()) > 0) {
            return false;
        }
        return true;
    }

    private boolean matchesRelaxedBudgetRange(BigDecimal guidePrice, UserDemand demand) {
        if (demand.getBudgetMin() != null && guidePrice.compareTo(demand.getBudgetMin()) < 0) {
            BigDecimal relaxedBudgetMin = demand.getBudgetMin().multiply(BUDGET_RELAX_LOWER_RATIO);
            return guidePrice.compareTo(relaxedBudgetMin) >= 0;
        }
        if (demand.getBudgetMax() != null && guidePrice.compareTo(demand.getBudgetMax()) > 0) {
            BigDecimal relaxedBudgetMax = demand.getBudgetMax().multiply(BUDGET_RELAX_UPPER_RATIO);
            return guidePrice.compareTo(relaxedBudgetMax) <= 0;
        }
        return false;
    }

    private boolean matchesStrictBodyType(CarModel car, UserDemand demand) {
        Set<String> bodyTypes = demandBodyTypes(demand);
        return bodyTypes.isEmpty() || bodyTypes.contains(car.getBodyType());
    }

    private boolean matchesStrictEnergyType(CarModel car, UserDemand demand) {
        Set<String> energyTypes = expandedDemandEnergyTypes(demand);
        return energyTypes.isEmpty() || energyTypes.contains(car.getEnergyType());
    }

    private boolean matchesSeatOptions(Integer seats, Set<String> seatOptions) {
        if (seats == null) {
            return false;
        }
        for (String option : seatOptions) {
            if ("7_PLUS".equals(option) && seats >= 7) {
                return true;
            }
            if (String.valueOf(seats).equals(option)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> demandBodyTypes(UserDemand demand) {
        return new LinkedHashSet<>(readStringList(demand.getBodyTypes()));
    }

    private Set<String> relaxedBodyTypes(Set<String> bodyTypes) {
        Set<String> relaxed = new LinkedHashSet<>();
        for (String bodyType : bodyTypes) {
            switch (bodyType) {
                case "SUV" -> relaxed.add("MPV");
                case "MPV" -> relaxed.add("SUV");
                case "轿车" -> relaxed.add("SUV");
                default -> {
                }
            }
        }
        relaxed.removeAll(bodyTypes);
        return relaxed;
    }

    private Set<String> expandedDemandEnergyTypes(UserDemand demand) {
        Set<String> expanded = new LinkedHashSet<>();
        for (String energyType : readStringList(demand.getEnergyTypes())) {
            if ("新能源".equals(energyType)) {
                expanded.add("纯电");
                expanded.add("插混");
                expanded.add("增程");
            } else {
                expanded.add(energyType);
            }
        }
        return expanded;
    }

    private Set<String> relaxedEnergyTypes(List<String> energyTypes, Set<String> strictEnergyTypes) {
        Set<String> relaxed = new LinkedHashSet<>();
        for (String energyType : energyTypes) {
            switch (energyType) {
                case "纯电" -> {
                    relaxed.add("插混");
                    relaxed.add("增程");
                }
                case "插混" -> {
                    relaxed.add("增程");
                    relaxed.add("纯电");
                }
                case "增程" -> {
                    relaxed.add("插混");
                    relaxed.add("纯电");
                }
                case "燃油" -> relaxed.add("插混");
                case "新能源" -> {
                    relaxed.add("纯电");
                    relaxed.add("插混");
                    relaxed.add("增程");
                }
                default -> {
                }
            }
        }
        relaxed.removeAll(strictEnergyTypes);
        return relaxed;
    }

    private List<String> readStringList(String json) {
        JsonNode node = readJsonArray(json);
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return values;
    }

    private List<Long> readLongList(String json) {
        JsonNode node = readJsonArray(json);
        List<Long> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.canConvertToLong()) {
                values.add(item.longValue());
            } else if (StringUtils.hasText(item.asText())) {
                values.add(Long.parseLong(item.asText()));
            }
        }
        return values;
    }

    private JsonNode readJsonArray(String json) {
        if (!StringUtils.hasText(json)) {
            return objectMapper.createArrayNode();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isTextual()) {
                node = objectMapper.readTree(node.asText());
            }
            return node.isArray() ? node : objectMapper.createArrayNode();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to parse recommendation json field", exception);
        }
    }

    private record CandidateCar(
            CarModel car,
            CarFeatureScore featureScore) {
    }
}
