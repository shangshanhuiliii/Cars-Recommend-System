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
import java.util.List;
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

    private final UserDemandMapper userDemandMapper;
    private final RecommendRecordMapper recommendRecordMapper;
    private final RecommendItemMapper recommendItemMapper;
    private final PriceScoreCalculator priceScoreCalculator;
    private final RecommendationCandidateService recommendationCandidateService;
    private final RecommendationWeightService recommendationWeightService;
    private final ParetoAnalyzer paretoAnalyzer;
    private final TopsisRanker topsisRanker;
    private final RecommendationExplanationService recommendationExplanationService;
    private final ObjectMapper objectMapper;

    public RecommendationServiceImpl(
            UserDemandMapper userDemandMapper,
            RecommendRecordMapper recommendRecordMapper,
            RecommendItemMapper recommendItemMapper,
            PriceScoreCalculator priceScoreCalculator,
            RecommendationCandidateService recommendationCandidateService,
            RecommendationWeightService recommendationWeightService,
            ParetoAnalyzer paretoAnalyzer,
            TopsisRanker topsisRanker,
            RecommendationExplanationService recommendationExplanationService,
            ObjectMapper objectMapper) {
        this.userDemandMapper = userDemandMapper;
        this.recommendRecordMapper = recommendRecordMapper;
        this.recommendItemMapper = recommendItemMapper;
        this.priceScoreCalculator = priceScoreCalculator;
        this.recommendationCandidateService = recommendationCandidateService;
        this.recommendationWeightService = recommendationWeightService;
        this.paretoAnalyzer = paretoAnalyzer;
        this.topsisRanker = topsisRanker;
        this.recommendationExplanationService = recommendationExplanationService;
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
        List<ScoredRecommendation> strictItems = scoreCandidates(candidates.strictCandidates(), demand);
        List<ScoredRecommendation> recommendationItems = scoreCandidates(candidates.recommendationCandidates(), demand);
        RecommendationWeightSnapshot weightSnapshot = recommendationWeightService.calculate(
                demand,
                scoreVectors(combine(strictItems, recommendationItems)));
        List<ScoredRecommendation> topsisScoredItems = applyTopsisScore(
                combine(strictItems, recommendationItems),
                weightSnapshot);
        strictItems = new ArrayList<>(topsisScoredItems.subList(0, strictItems.size()));
        recommendationItems = new ArrayList<>(topsisScoredItems.subList(strictItems.size(), topsisScoredItems.size()));
        List<ScoredRecommendation> scoredItems = sortRecommendationItems(
                strictItems,
                recommendationItems,
                weightSnapshot);
        String recommendStatus = buildRecommendStatus(scoredItems);
        String fallbackMessage = buildFallbackMessage(scoredItems, recommendStatus);

        RecommendRecord record = new RecommendRecord();
        record.setUserId(userId);
        record.setDemandId(demand.getId());
        record.setProfileText(demand.getProfileText());
        record.setWeightSnapshot(toJson(weightSnapshot));
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
        response.setAlgorithmVersion(weightSnapshot.algorithmVersion());
        response.setAlpha(weightSnapshot.alpha());
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

    private List<ScoredRecommendation> scoreCandidates(
            List<RecommendationCandidate> candidates,
            UserDemand demand) {
        List<ScoredRecommendation> scoredItems = new ArrayList<>();
        for (RecommendationCandidate candidate : candidates) {
            scoredItems.add(toScoredRecommendation(candidate, demand));
        }
        return scoredItems;
    }

    private List<ScoredRecommendation> sortRecommendationItems(
            List<ScoredRecommendation> strictItems,
            List<ScoredRecommendation> recommendationItems,
            RecommendationWeightSnapshot weightSnapshot) {
        strictItems = markParetoDominated(strictItems, weightSnapshot);
        recommendationItems = markParetoDominated(recommendationItems, weightSnapshot);

        strictItems.sort(recommendationComparator());
        recommendationItems.sort(recommendationComparator());

        List<ScoredRecommendation> finalItems = new ArrayList<>(strictItems);
        finalItems.addAll(recommendationItems);
        return finalItems;
    }

    private List<ScoredRecommendation> applyTopsisScore(
            List<ScoredRecommendation> items,
            RecommendationWeightSnapshot weightSnapshot) {
        List<RecommendationScoreVector> scoreVectors = scoreVectors(items);
        TopsisRanker.TopsisRankingResult rankingResult = topsisRanker.analyze(
                scoreVectors,
                weightSnapshot.finalWeight(),
                items.stream().map(ScoredRecommendation::fallbackScore).toList());
        List<ScoredRecommendation> topsisScoredItems = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            TopsisRanker.TopsisItemResult topsisResult = rankingResult.items().get(index);
            RecommendationExplanationService.Explanation explanation = recommendationExplanationService.generate(
                    scoreVectors.get(index),
                    topsisResult,
                    weightSnapshot.finalWeight());
            topsisScoredItems.add(items.get(index)
                    .withTotalScore(topsisResult.totalScore())
                    .withExplanation(explanation));
        }
        return topsisScoredItems;
    }

    private List<ScoredRecommendation> markParetoDominated(
            List<ScoredRecommendation> items,
            RecommendationWeightSnapshot weightSnapshot) {
        ParetoAnalyzer.ParetoResult paretoResult = paretoAnalyzer.analyze(
                scoreVectors(items),
                weightSnapshot.finalWeight());
        List<ScoredRecommendation> markedItems = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            markedItems.add(items.get(index).withParetoDominated(paretoResult.dominatedFlags().get(index)));
        }
        return markedItems;
    }

    private List<ScoredRecommendation> combine(
            List<ScoredRecommendation> strictItems,
            List<ScoredRecommendation> recommendationItems) {
        List<ScoredRecommendation> combined = new ArrayList<>(strictItems);
        combined.addAll(recommendationItems);
        return combined;
    }

    private List<RecommendationScoreVector> scoreVectors(List<ScoredRecommendation> scoredItems) {
        return scoredItems.stream()
                .map(item -> new RecommendationScoreVector(
                        item.priceScore(),
                        item.featureScore().getSpaceScore(),
                        item.featureScore().getSafetyScore(),
                        item.featureScore().getEnergyScore(),
                        item.featureScore().getIntelligenceScore(),
                        item.featureScore().getComfortScore(),
                        item.featureScore().getPowerScore(),
                        item.featureScore().getReputationScore(),
                        item.featureScore().getPopularityScore()))
                .toList();
    }

    private ScoredRecommendation toScoredRecommendation(
            RecommendationCandidate candidate,
            UserDemand demand) {
        BigDecimal priceScore = priceScoreCalculator.calculate(candidate.car().getGuidePrice(), demand);
        BigDecimal fallbackScore = calculateFallbackScore(priceScore, candidate.featureScore(), demand);
        return new ScoredRecommendation(
                candidate.car(),
                candidate.featureScore(),
                priceScore,
                fallbackScore,
                fallbackScore,
                candidate.matchLevel(),
                List.of(),
                "",
                "",
                false);
    }

    private BigDecimal calculateFallbackScore(BigDecimal priceScore, CarFeatureScore score, UserDemand demand) {
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

    private Comparator<ScoredRecommendation> recommendationComparator() {
        return Comparator.comparing(ScoredRecommendation::paretoDominated)
                .thenComparing(ScoredRecommendation::totalScore, Comparator.reverseOrder())
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
            BigDecimal fallbackScore,
            BigDecimal totalScore,
            MatchLevel matchLevel,
            List<String> tags,
            String reasonText,
            String weaknessText,
            boolean paretoDominated) {

        private ScoredRecommendation withTotalScore(BigDecimal totalScore) {
            return new ScoredRecommendation(
                    car,
                    featureScore,
                    priceScore,
                    fallbackScore,
                    totalScore,
                    matchLevel,
                    tags,
                    reasonText,
                    weaknessText,
                    paretoDominated);
        }

        private ScoredRecommendation withParetoDominated(boolean paretoDominated) {
            return new ScoredRecommendation(
                    car,
                    featureScore,
                    priceScore,
                    fallbackScore,
                    totalScore,
                    matchLevel,
                    tags,
                    reasonText,
                    weaknessText,
                    paretoDominated);
        }

        private ScoredRecommendation withExplanation(RecommendationExplanationService.Explanation explanation) {
            return new ScoredRecommendation(
                    car,
                    featureScore,
                    priceScore,
                    fallbackScore,
                    totalScore,
                    matchLevel,
                    explanation.tags(),
                    explanation.reasonText(),
                    explanation.weaknessText(),
                    paretoDominated);
        }
    }
}
