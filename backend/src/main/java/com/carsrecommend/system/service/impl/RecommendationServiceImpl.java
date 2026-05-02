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
import com.carsrecommend.system.mapper.RecommendItemMapper;
import com.carsrecommend.system.mapper.RecommendRecordMapper;
import com.carsrecommend.system.mapper.UserDemandMapper;
import com.carsrecommend.system.service.RecommendationService;
import com.carsrecommend.system.vo.RecommendationItemVO;
import com.carsrecommend.system.vo.RecommendationResponseVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RecommendRecordMapper recommendRecordMapper;
    private final RecommendItemMapper recommendItemMapper;
    private final PriceScoreCalculator priceScoreCalculator;
    private final RecommendationCandidateService recommendationCandidateService;
    private final ObjectMapper objectMapper;

    public RecommendationServiceImpl(
            UserDemandMapper userDemandMapper,
            RecommendRecordMapper recommendRecordMapper,
            RecommendItemMapper recommendItemMapper,
            PriceScoreCalculator priceScoreCalculator,
            RecommendationCandidateService recommendationCandidateService,
            ObjectMapper objectMapper) {
        this.userDemandMapper = userDemandMapper;
        this.recommendRecordMapper = recommendRecordMapper;
        this.recommendItemMapper = recommendItemMapper;
        this.priceScoreCalculator = priceScoreCalculator;
        this.recommendationCandidateService = recommendationCandidateService;
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

        RecommendationCandidateGroups candidates = recommendationCandidateService.generateCandidates(demand);
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

    private List<ScoredRecommendation> generateRecommendationItems(
            RecommendationCandidateGroups candidates,
            UserDemand demand) {
        List<ScoredRecommendation> strictItems = new ArrayList<>();
        List<ScoredRecommendation> recommendationItems = new ArrayList<>();
        for (RecommendationCandidate candidate : candidates.strictCandidates()) {
            strictItems.add(toScoredRecommendation(candidate, demand));
        }
        strictItems.sort(recommendationComparator());

        for (RecommendationCandidate candidate : candidates.recommendationCandidates()) {
            recommendationItems.add(toScoredRecommendation(candidate, demand));
        }
        recommendationItems.sort(recommendationComparator());

        List<ScoredRecommendation> finalItems = new ArrayList<>(strictItems);
        finalItems.addAll(recommendationItems);
        return finalItems;
    }

    private ScoredRecommendation toScoredRecommendation(
            RecommendationCandidate candidate,
            UserDemand demand) {
        BigDecimal priceScore = priceScoreCalculator.calculate(candidate.car().getGuidePrice(), demand);
        BigDecimal totalScore = calculateTotalScore(priceScore, candidate.featureScore(), demand);
        List<String> tags = generateTags(priceScore, candidate.featureScore());
        String reasonText = generateReasonText(priceScore, candidate.featureScore(), demand);
        String weaknessText = generateWeaknessText(priceScore, candidate.featureScore(), demand);
        return new ScoredRecommendation(
                candidate.car(),
                candidate.featureScore(),
                priceScore,
                totalScore,
                candidate.matchLevel(),
                tags,
                reasonText,
                weaknessText);
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize recommendation json field", exception);
        }
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
