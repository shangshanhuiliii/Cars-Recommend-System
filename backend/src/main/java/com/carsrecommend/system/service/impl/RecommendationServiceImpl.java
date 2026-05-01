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
import java.util.LinkedHashMap;
import java.util.HashSet;
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
    private static final String BASIC_REASON_TEXT = "该车型符合当前严格筛选条件，综合匹配度由车型评分和用户权重计算得出。";
    private static final String BASIC_WEAKNESS_TEXT = "当前结果为严格匹配推荐，可结合维度评分继续对比车型差异。";

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

        List<ScoredRecommendation> scoredItems = carModelMapper.findApprovedRecommendationCandidates().stream()
                .map(car -> toScoredRecommendation(car, demand))
                .flatMap(List::stream)
                .sorted(recommendationComparator())
                .limit(request.getTopK())
                .toList();

        String recommendStatus = scoredItems.isEmpty()
                ? RecommendStatus.EMPTY.getCode()
                : RecommendStatus.SUCCESS.getCode();
        String fallbackMessage = scoredItems.isEmpty() ? "" : STRICT_SUCCESS_MESSAGE;

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

    private List<ScoredRecommendation> toScoredRecommendation(CarModel car, UserDemand demand) {
        if (!matchesStrictDemand(car, demand)) {
            return List.of();
        }
        return carFeatureScoreMapper.findByCarId(car.getId())
                .map(score -> {
                    BigDecimal priceScore = calculatePriceScore(car.getGuidePrice(), demand);
                    BigDecimal totalScore = calculateTotalScore(priceScore, score, demand);
                    List<String> tags = generateTags(priceScore, score);
                    return List.of(new ScoredRecommendation(car, score, priceScore, totalScore, tags));
                })
                .orElseGet(List::of);
    }

    private boolean matchesStrictDemand(CarModel car, UserDemand demand) {
        Set<String> excludedBrands = new HashSet<>(readStringList(demand.getExcludedBrands()));
        if (excludedBrands.contains(car.getBrand())) {
            return false;
        }
        Set<Long> excludedCarIds = new HashSet<>(readLongList(demand.getExcludedCarIds()));
        if (excludedCarIds.contains(car.getId())) {
            return false;
        }
        if (demand.getSeats() != null && (car.getSeats() == null || car.getSeats() < demand.getSeats())) {
            return false;
        }
        if (demand.getBudgetMax() != null && car.getGuidePrice().compareTo(demand.getBudgetMax()) > 0) {
            return false;
        }
        if (StringUtils.hasText(demand.getBodyType()) && !demand.getBodyType().equals(car.getBodyType())) {
            return false;
        }
        return matchesDemandEnergyType(car.getEnergyType(), demand.getEnergyType());
    }

    private boolean matchesDemandEnergyType(String carEnergyType, String demandEnergyType) {
        if (!StringUtils.hasText(demandEnergyType)) {
            return true;
        }
        if ("新能源".equals(demandEnergyType)) {
            return "纯电".equals(carEnergyType) || "插混".equals(carEnergyType) || "增程".equals(carEnergyType);
        }
        return demandEnergyType.equals(carEnergyType);
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
        item.setMatchLevel(MatchLevel.STRICT.getCode());
        item.setReasonText(BASIC_REASON_TEXT);
        item.setWeaknessText(BASIC_WEAKNESS_TEXT);
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
        vo.setMatchLevel(MatchLevel.STRICT.getCode());
        vo.setTags(scoredItem.tags());
        vo.setReasonText(BASIC_REASON_TEXT);
        vo.setWeaknessText(BASIC_WEAKNESS_TEXT);
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

    private record ScoredRecommendation(
            CarModel car,
            CarFeatureScore featureScore,
            BigDecimal priceScore,
            BigDecimal totalScore,
            List<String> tags) {
    }

    private record TagCandidate(String label, BigDecimal score) {
    }
}
