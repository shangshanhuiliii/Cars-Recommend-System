package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.common.enums.MatchLevel;
import com.carsrecommend.system.common.enums.RecommendStatus;
import com.carsrecommend.system.dto.RecommendationGenerateRequest;
import com.carsrecommend.system.entity.CarFeatureScore;
import com.carsrecommend.system.entity.CarModel;
import com.carsrecommend.system.entity.RecommendItem;
import com.carsrecommend.system.entity.RecommendRecord;
import com.carsrecommend.system.entity.UserDemand;
import com.carsrecommend.system.mapper.CarFeatureScoreMapper;
import com.carsrecommend.system.mapper.CarModelMapper;
import com.carsrecommend.system.mapper.RecommendItemMapper;
import com.carsrecommend.system.mapper.RecommendRecordMapper;
import com.carsrecommend.system.mapper.UserDemandMapper;
import com.carsrecommend.system.service.RecommendationService;
import com.carsrecommend.system.vo.RecommendationItemVO;
import com.carsrecommend.system.vo.RecommendationResponseVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class RecommendationServiceImpl implements RecommendationService {

    private static final long DEFAULT_DEMO_USER_ID = 1L;
    private static final String STRICT_SUCCESS_MESSAGE = "已为您找到完全匹配车型";
    private static final String PARTIAL_FALLBACK_MESSAGE =
            "完全匹配车型数量不足，系统已补充部分推荐车型，并在每条结果中标明匹配状态。";
    private static final String NO_STRICT_FALLBACK_MESSAGE =
            "未找到完全匹配车型，系统已根据您的核心偏好提供相近推荐。";
    private static final String EMPTY_RECOMMEND_MESSAGE = "暂未找到合适车型，请调整预算、车型类型或动力类型后重试。";
    private static final String DEFAULT_REASON_TEXT = "该车型在多个维度上与您的需求较为接近，可作为备选车型进一步对比。";
    private static final String DEFAULT_WEAKNESS_TEXT = "该车型整体匹配较均衡，暂无明显短板。";

    private final UserDemandMapper userDemandMapper;
    private final CarModelMapper carModelMapper;
    private final CarFeatureScoreMapper carFeatureScoreMapper;
    private final RecommendRecordMapper recommendRecordMapper;
    private final RecommendItemMapper recommendItemMapper;
    private final ObjectMapper objectMapper;

    public RecommendationServiceImpl(
            UserDemandMapper userDemandMapper,
            CarModelMapper carModelMapper,
            CarFeatureScoreMapper carFeatureScoreMapper,
            RecommendRecordMapper recommendRecordMapper,
            RecommendItemMapper recommendItemMapper,
            ObjectMapper objectMapper) {
        this.userDemandMapper = userDemandMapper;
        this.carModelMapper = carModelMapper;
        this.carFeatureScoreMapper = carFeatureScoreMapper;
        this.recommendRecordMapper = recommendRecordMapper;
        this.recommendItemMapper = recommendItemMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public RecommendationResponseVO generate(RecommendationGenerateRequest request) {
        Long userId = resolveUserId(request.getUserId());
        UserDemand demand = userDemandMapper.findById(request.getDemandId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "user demand not found"));
        if (!userId.equals(demand.getUserId())) {
            throw new BusinessException("demand does not belong to current user");
        }

        List<CandidateCar> candidates = loadCandidatesWithScores();
        List<ScoredRecommendation> scoredItems = generateRecommendationItems(candidates, demand);
        String recommendStatus = buildRecommendStatus(scoredItems);
        String fallbackMessage = buildFallbackMessage(scoredItems, recommendStatus);

        RecommendRecord record = new RecommendRecord();
        record.setUserId(userId);
        record.setDemandId(demand.getId());
        record.setProfileText(demand.getProfileText());
        record.setWeightSnapshot(toJson(weightSnapshot(demand)));
        record.setFallbackMessage(fallbackMessage);
        record.setRecommendStatus(recommendStatus);
        recommendRecordMapper.insert(record);

        List<RecommendationItemVO> itemVOs = new ArrayList<>();
        for (int index = 0; index < scoredItems.size(); index++) {
            ScoredRecommendation scoredItem = scoredItems.get(index);
            int rankNo = index + 1;
            recommendItemMapper.insert(toRecommendItem(record.getId(), rankNo, scoredItem));
            itemVOs.add(toItemVO(rankNo, scoredItem));
        }

        RecommendationResponseVO response = new RecommendationResponseVO();
        response.setRecordId(record.getId());
        response.setDemandId(demand.getId());
        response.setUserId(userId);
        response.setProfileText(demand.getProfileText());
        response.setFallbackMessage(fallbackMessage);
        response.setRecommendStatus(recommendStatus);
        response.setItems(itemVOs);
        response.setCreateTime(LocalDateTime.now());
        return response;
    }

    private Long resolveUserId(Long userId) {
        Long resolvedUserId = userId == null ? DEFAULT_DEMO_USER_ID : userId;
        if (!userDemandMapper.existsActiveUser(resolvedUserId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "app user not found");
        }
        return resolvedUserId;
    }

    private List<CandidateCar> loadCandidatesWithScores() {
        List<CandidateCar> candidates = new ArrayList<>();
        for (CarModel car : carModelMapper.findApprovedRecommendationCandidates()) {
            carFeatureScoreMapper.findByCarId(car.getId())
                    .ifPresent(score -> candidates.add(new CandidateCar(car, score)));
        }
        return candidates;
    }

    private List<ScoredRecommendation> generateRecommendationItems(
            List<CandidateCar> candidates,
            UserDemand demand) {
        List<ScoredRecommendation> strictItems = new ArrayList<>();
        List<ScoredRecommendation> recommendationItems = new ArrayList<>();
        Set<Long> addedCarIds = new HashSet<>();

        addStageRecommendations(candidates, demand, MatchLevel.STRICT, strictItems, addedCarIds);
        strictItems.sort(recommendationComparator());

        for (MatchLevel matchLevel : List.of(
                MatchLevel.RELAX_BUDGET,
                MatchLevel.RELAX_BODY_TYPE,
                MatchLevel.RELAX_ENERGY_TYPE,
                MatchLevel.SIMILAR_RECOMMEND)) {
            addStageRecommendations(candidates, demand, matchLevel, recommendationItems, addedCarIds);
        }

        recommendationItems.sort(recommendationComparator());
        List<ScoredRecommendation> finalItems = new ArrayList<>(strictItems);
        finalItems.addAll(recommendationItems);
        return finalItems;
    }

    private void addStageRecommendations(
            List<CandidateCar> candidates,
            UserDemand demand,
            MatchLevel matchLevel,
            List<ScoredRecommendation> resultItems,
            Set<Long> addedCarIds) {
        for (CandidateCar candidate : candidates) {
            Long carId = candidate.car().getId();
            if (addedCarIds.contains(carId) || !matchesDemand(candidate.car(), demand, matchLevel)) {
                continue;
            }
            resultItems.add(toScoredRecommendation(candidate, demand, matchLevel));
            addedCarIds.add(carId);
        }
    }

    private ScoredRecommendation toScoredRecommendation(
            CandidateCar candidate,
            UserDemand demand,
            MatchLevel matchLevel) {
        BigDecimal priceScore = calculatePriceScore(candidate.car().getGuidePrice(), demand);
        BigDecimal totalScore = calculateTotalScore(priceScore, candidate.featureScore(), demand);
        List<String> tags = generateTags(priceScore, candidate.featureScore());
        String reasonText = generateReasonText(priceScore, candidate.featureScore(), demand);
        String weaknessText = generateWeaknessText(priceScore, candidate.featureScore(), demand);
        return new ScoredRecommendation(
                candidate.car(),
                candidate.featureScore(),
                priceScore,
                totalScore,
                matchLevel,
                tags,
                reasonText,
                weaknessText);
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
        Set<String> excludedBrands = new HashSet<>(readStringList(demand.getExcludedBrands()));
        if (excludedBrands.contains(car.getBrand())) {
            return false;
        }
        Set<Long> excludedCarIds = new HashSet<>(readLongList(demand.getExcludedCarIds()));
        if (excludedCarIds.contains(car.getId())) {
            return false;
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
        if (demand.getBudgetMax() == null) {
            return false;
        }
        BigDecimal relaxedBudgetMax = demand.getBudgetMax().multiply(new BigDecimal("1.10"));
        return car.getGuidePrice().compareTo(demand.getBudgetMax()) > 0
                && car.getGuidePrice().compareTo(relaxedBudgetMax) <= 0
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
        return demand.getBudgetMax() == null || car.getGuidePrice().compareTo(demand.getBudgetMax()) <= 0;
    }

    private boolean matchesStrictBodyType(CarModel car, UserDemand demand) {
        Set<String> bodyTypes = demandBodyTypes(demand);
        return bodyTypes.isEmpty() || bodyTypes.contains(car.getBodyType());
    }

    private boolean matchesStrictEnergyType(CarModel car, UserDemand demand) {
        Set<String> energyTypes = expandedDemandEnergyTypes(demand);
        return energyTypes.isEmpty() || energyTypes.contains(car.getEnergyType());
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

    private BigDecimal calculatePriceScore(BigDecimal price, UserDemand demand) {
        BigDecimal budgetMin = demand.getBudgetMin();
        BigDecimal budgetMax = demand.getBudgetMax();
        if (budgetMin == null && budgetMax == null) {
            return score(75);
        }
        if (budgetMax == null) {
            if (price.compareTo(budgetMin) < 0) {
                return calculateBelowBudgetMinScore(price, budgetMin);
            }
            return score(90);
        }
        if (price.compareTo(budgetMax) > 0) {
            return calculateAboveBudgetMaxScore(price, budgetMax);
        }
        if (budgetMin == null) {
            budgetMin = BigDecimal.ZERO;
        }
        if (price.compareTo(budgetMin) < 0) {
            return calculateBelowBudgetMinScore(price, budgetMin);
        }
        BigDecimal budgetMid = budgetMin.add(budgetMax).divide(new BigDecimal("2"), 8, RoundingMode.HALF_UP);
        BigDecimal budgetRange = budgetMax.subtract(budgetMin);
        BigDecimal halfRange = budgetRange.divide(new BigDecimal("2"), 8, RoundingMode.HALF_UP)
                .max(BigDecimal.ONE);
        BigDecimal distanceRatio = price.subtract(budgetMid).abs()
                .divide(halfRange, 8, RoundingMode.HALF_UP);
        BigDecimal value = new BigDecimal("100").subtract(distanceRatio.multiply(new BigDecimal("10")));
        return score(value.max(new BigDecimal("90")));
    }

    private BigDecimal calculateBelowBudgetMinScore(BigDecimal price, BigDecimal budgetMin) {
        BigDecimal denominator = budgetMin.max(BigDecimal.ONE);
        BigDecimal lowerRatio = budgetMin.subtract(price).divide(denominator, 8, RoundingMode.HALF_UP);
        BigDecimal value = new BigDecimal("90").subtract(lowerRatio.multiply(new BigDecimal("50")));
        return score(value.max(new BigDecimal("75")));
    }

    private BigDecimal calculateAboveBudgetMaxScore(BigDecimal price, BigDecimal budgetMax) {
        BigDecimal denominator = budgetMax.max(BigDecimal.ONE);
        BigDecimal overRatio = price.subtract(budgetMax).divide(denominator, 8, RoundingMode.HALF_UP);
        BigDecimal value = new BigDecimal("80").subtract(overRatio.multiply(new BigDecimal("100")));
        return score(value.max(new BigDecimal("50")));
    }

    private BigDecimal calculateTotalScore(BigDecimal priceScore, CarFeatureScore score, UserDemand demand) {
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(priceScore.multiply(weight(demand.getWeightPrice())));
        total = total.add(score.getSpaceScore().multiply(weight(demand.getWeightSpace())));
        total = total.add(score.getSafetyScore().multiply(weight(demand.getWeightSafety())));
        total = total.add(score.getEnergyScore().multiply(weight(demand.getWeightEnergy())));
        total = total.add(score.getIntelligenceScore().multiply(weight(demand.getWeightIntelligence())));
        total = total.add(score.getComfortScore().multiply(weight(demand.getWeightComfort())));
        total = total.add(score.getPowerScore().multiply(weight(demand.getWeightPower())));
        total = total.add(score.getReputationScore().multiply(weight(demand.getWeightReputation())));
        total = total.add(score.getPopularityScore().multiply(weight(demand.getWeightPopularity())));
        return score(total);
    }

    private BigDecimal weight(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<String> generateTags(BigDecimal priceScore, CarFeatureScore score) {
        List<TagCandidate> candidates = new ArrayList<>();
        addTag(candidates, score.getSpaceScore(), "空间优秀", new BigDecimal("85"));
        addTag(candidates, score.getSafetyScore(), "安全配置高", new BigDecimal("85"));
        addTag(candidates, score.getEnergyScore(), "能耗表现好", new BigDecimal("85"));
        addTag(candidates, score.getIntelligenceScore(), "智能配置丰富", new BigDecimal("85"));
        addTag(candidates, score.getComfortScore(), "舒适性较好", new BigDecimal("85"));
        addTag(candidates, score.getPowerScore(), "动力表现强", new BigDecimal("85"));
        addTag(candidates, score.getReputationScore(), "口碑较好", new BigDecimal("85"));
        addTag(candidates, score.getPopularityScore(), "热门车型", new BigDecimal("85"));
        addTag(candidates, priceScore, "价格匹配度高", new BigDecimal("90"));

        if (candidates.isEmpty()) {
            return List.of("表现均衡", "接近需求");
        }
        candidates.sort(Comparator.comparing(TagCandidate::score).reversed());
        return candidates.stream()
                .limit(3)
                .map(TagCandidate::label)
                .toList();
    }

    private String generateReasonText(BigDecimal priceScore, CarFeatureScore score, UserDemand demand) {
        List<DimensionScore> dimensions = dimensionScores(priceScore, score, demand);
        List<DimensionScore> topWeightedDimensions = dimensions.stream()
                .sorted(Comparator.comparing(DimensionScore::weight, Comparator.reverseOrder()))
                .limit(4)
                .toList();
        List<String> reasons = new ArrayList<>();
        Set<String> usedKeys = new LinkedHashSet<>();

        for (DimensionScore dimension : topWeightedDimensions) {
            if (dimension.score().compareTo(new BigDecimal("80")) >= 0) {
                addReason(reasons, usedKeys, dimension);
            }
            if (reasons.size() >= 3) {
                break;
            }
        }

        if (reasons.size() < 2) {
            for (DimensionScore dimension : dimensionsByScoreDesc(dimensions)) {
                if (dimension.score().compareTo(new BigDecimal("80")) >= 0) {
                    addReason(reasons, usedKeys, dimension);
                }
                if (reasons.size() >= 3) {
                    break;
                }
            }
        }

        if (reasons.size() < 2) {
            for (DimensionScore dimension : dimensionsByScoreDesc(dimensions)) {
                addReason(reasons, usedKeys, dimension);
                if (reasons.size() >= 2) {
                    break;
                }
            }
        }

        if (reasons.isEmpty()) {
            return DEFAULT_REASON_TEXT;
        }
        return String.join("；", reasons);
    }

    private String generateWeaknessText(BigDecimal priceScore, CarFeatureScore score, UserDemand demand) {
        List<DimensionScore> topWeightedDimensions = dimensionScores(priceScore, score, demand).stream()
                .sorted(Comparator.comparing(DimensionScore::weight, Comparator.reverseOrder()))
                .limit(4)
                .toList();
        List<String> weaknesses = new ArrayList<>();
        for (DimensionScore dimension : topWeightedDimensions) {
            if (dimension.score().compareTo(new BigDecimal("65")) < 0) {
                weaknesses.add(dimension.weaknessText());
            }
            if (weaknesses.size() >= 2) {
                break;
            }
        }
        return weaknesses.isEmpty() ? DEFAULT_WEAKNESS_TEXT : String.join("；", weaknesses);
    }

    private void addReason(List<String> reasons, Set<String> usedKeys, DimensionScore dimension) {
        if (usedKeys.add(dimension.key())) {
            reasons.add(dimension.reasonText());
        }
    }

    private List<DimensionScore> dimensionsByScoreDesc(List<DimensionScore> dimensions) {
        return dimensions.stream()
                .sorted(Comparator.comparing(DimensionScore::score, Comparator.reverseOrder()))
                .toList();
    }

    private List<DimensionScore> dimensionScores(BigDecimal priceScore, CarFeatureScore score, UserDemand demand) {
        return List.of(
                new DimensionScore(
                        "price",
                        weight(demand.getWeightPrice()),
                        priceScore,
                        "该车型价格与您的预算匹配度较高，有助于控制购车成本。",
                        "该车型价格匹配度偏低，可能与您的预算区间存在一定偏差。"),
                new DimensionScore(
                        "space",
                        weight(demand.getWeightSpace()),
                        score.getSpaceScore(),
                        "该车型空间表现较好，适合家庭出行和多人乘坐场景。",
                        "该车型空间表现相对一般，如果您经常满载或重视乘坐宽敞度，需要重点对比。"),
                new DimensionScore(
                        "safety",
                        weight(demand.getWeightSafety()),
                        score.getSafetyScore(),
                        "该车型安全配置得分较高，符合您对安全性的关注。",
                        "该车型安全配置得分偏低，如果您重视主动安全和安全气囊配置，需要谨慎比较。"),
                new DimensionScore(
                        "energy",
                        weight(demand.getWeightEnergy()),
                        score.getEnergyScore(),
                        "该车型能耗表现较好，日常用车成本和续航表现更有优势。",
                        "该车型能耗或续航表现偏弱，如果您关注长期用车成本，需要继续对比。"),
                new DimensionScore(
                        "intelligence",
                        weight(demand.getWeightIntelligence()),
                        score.getIntelligenceScore(),
                        "该车型智能配置较丰富，能提升车机体验和辅助驾驶便利性。",
                        "该车型智能配置表现一般，如果您更看重车机体验和辅助驾驶，可继续对比其他车型。"),
                new DimensionScore(
                        "comfort",
                        weight(demand.getWeightComfort()),
                        score.getComfortScore(),
                        "该车型舒适性表现较好，适合日常通勤和长途乘坐。",
                        "该车型舒适性得分偏低，如果您重视乘坐体验，需要关注空间、配置和口碑差异。"),
                new DimensionScore(
                        "power",
                        weight(demand.getWeightPower()),
                        score.getPowerScore(),
                        "该车型动力表现较强，适合看重加速和驾驶响应的用户。",
                        "该车型动力表现一般，如果您经常高速或满载出行，需要重点试驾确认。"),
                new DimensionScore(
                        "reputation",
                        weight(demand.getWeightReputation()),
                        score.getReputationScore(),
                        "该车型口碑评分较高，用户评价和可靠性表现更稳。",
                        "该车型口碑得分一般，建议结合真实车主评价和售后表现继续判断。"),
                new DimensionScore(
                        "popularity",
                        weight(demand.getWeightPopularity()),
                        score.getPopularityScore(),
                        "该车型市场热度较高，销量基础和关注度表现较好。",
                        "该车型市场热度一般，可结合保有量和后续用车便利性继续判断。"));
    }

    private void addTag(List<TagCandidate> candidates, BigDecimal score, String label, BigDecimal threshold) {
        if (score != null && score.compareTo(threshold) >= 0) {
            candidates.add(new TagCandidate(label, score));
        }
    }

    private Comparator<ScoredRecommendation> recommendationComparator() {
        return Comparator.comparing(ScoredRecommendation::totalScore, Comparator.reverseOrder())
                .thenComparing(item -> item.featureScore().getReputationScore(), Comparator.reverseOrder())
                .thenComparing(item -> item.featureScore().getPopularityScore(), Comparator.reverseOrder());
    }

    private String buildRecommendStatus(List<ScoredRecommendation> items) {
        if (items.isEmpty()) {
            return RecommendStatus.EMPTY.getCode();
        }
        boolean hasFallbackItem = items.stream()
                .anyMatch(item -> item.matchLevel() != MatchLevel.STRICT);
        return hasFallbackItem ? RecommendStatus.FALLBACK.getCode() : RecommendStatus.SUCCESS.getCode();
    }

    private String buildFallbackMessage(List<ScoredRecommendation> items, String recommendStatus) {
        if (RecommendStatus.EMPTY.getCode().equals(recommendStatus)) {
            return EMPTY_RECOMMEND_MESSAGE;
        }
        if (RecommendStatus.SUCCESS.getCode().equals(recommendStatus)) {
            return STRICT_SUCCESS_MESSAGE;
        }
        long strictCount = items.stream()
                .filter(item -> item.matchLevel() == MatchLevel.STRICT)
                .count();
        return strictCount > 0 ? PARTIAL_FALLBACK_MESSAGE : NO_STRICT_FALLBACK_MESSAGE;
    }

    private RecommendItem toRecommendItem(Long recordId, int rankNo, ScoredRecommendation scoredItem) {
        RecommendItem item = new RecommendItem();
        item.setRecordId(recordId);
        item.setCarId(scoredItem.car().getId());
        item.setRankNo(rankNo);
        item.setTotalScore(scoredItem.totalScore());
        item.setPriceScore(scoredItem.priceScore());
        item.setSpaceScore(scoredItem.featureScore().getSpaceScore());
        item.setSafetyScore(scoredItem.featureScore().getSafetyScore());
        item.setEnergyScore(scoredItem.featureScore().getEnergyScore());
        item.setIntelligenceScore(scoredItem.featureScore().getIntelligenceScore());
        item.setComfortScore(scoredItem.featureScore().getComfortScore());
        item.setPowerScore(scoredItem.featureScore().getPowerScore());
        item.setReputationScore(scoredItem.featureScore().getReputationScore());
        item.setPopularityScore(scoredItem.featureScore().getPopularityScore());
        item.setTags(toJson(scoredItem.tags()));
        item.setMatchLevel(scoredItem.matchLevel().getCode());
        item.setReasonText(scoredItem.reasonText());
        item.setWeaknessText(scoredItem.weaknessText());
        return item;
    }

    private RecommendationItemVO toItemVO(int rankNo, ScoredRecommendation scoredItem) {
        RecommendationItemVO vo = new RecommendationItemVO();
        vo.setRankNo(rankNo);
        vo.setCarId(scoredItem.car().getId());
        vo.setBrand(scoredItem.car().getBrand());
        vo.setSeries(scoredItem.car().getSeries());
        vo.setModelName(scoredItem.car().getModelName());
        vo.setGuidePrice(scoredItem.car().getGuidePrice());
        vo.setBodyType(scoredItem.car().getBodyType());
        vo.setEnergyType(scoredItem.car().getEnergyType());
        vo.setSeats(scoredItem.car().getSeats());
        vo.setTotalScore(scoredItem.totalScore());
        vo.setPriceScore(scoredItem.priceScore());
        vo.setSpaceScore(scoredItem.featureScore().getSpaceScore());
        vo.setSafetyScore(scoredItem.featureScore().getSafetyScore());
        vo.setEnergyScore(scoredItem.featureScore().getEnergyScore());
        vo.setIntelligenceScore(scoredItem.featureScore().getIntelligenceScore());
        vo.setComfortScore(scoredItem.featureScore().getComfortScore());
        vo.setPowerScore(scoredItem.featureScore().getPowerScore());
        vo.setReputationScore(scoredItem.featureScore().getReputationScore());
        vo.setPopularityScore(scoredItem.featureScore().getPopularityScore());
        vo.setMatchLevel(scoredItem.matchLevel().getCode());
        vo.setTags(scoredItem.tags());
        vo.setReasonText(scoredItem.reasonText());
        vo.setWeaknessText(scoredItem.weaknessText());
        return vo;
    }

    private Map<String, BigDecimal> weightSnapshot(UserDemand demand) {
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        weights.put("price", demand.getWeightPrice());
        weights.put("space", demand.getWeightSpace());
        weights.put("safety", demand.getWeightSafety());
        weights.put("energy", demand.getWeightEnergy());
        weights.put("intelligence", demand.getWeightIntelligence());
        weights.put("comfort", demand.getWeightComfort());
        weights.put("power", demand.getWeightPower());
        weights.put("reputation", demand.getWeightReputation());
        weights.put("popularity", demand.getWeightPopularity());
        return weights;
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize recommendation json field", exception);
        }
    }

    private BigDecimal score(double value) {
        return score(BigDecimal.valueOf(value));
    }

    private BigDecimal score(BigDecimal value) {
        return value.max(BigDecimal.ZERO)
                .min(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private record CandidateCar(
            CarModel car,
            CarFeatureScore featureScore) {
    }

    private record ScoredRecommendation(
            CarModel car,
            CarFeatureScore featureScore,
            BigDecimal priceScore,
            BigDecimal totalScore,
            MatchLevel matchLevel,
            List<String> tags,
            String reasonText,
            String weaknessText) {
    }

    private record DimensionScore(
            String key,
            BigDecimal weight,
            BigDecimal score,
            String reasonText,
            String weaknessText) {
    }

    private record TagCandidate(String label, BigDecimal score) {
    }
}
