package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.entity.RecommendRecord;
import com.carsrecommend.system.mapper.RecommendItemMapper;
import com.carsrecommend.system.mapper.RecommendItemSnapshot;
import com.carsrecommend.system.mapper.RecommendRecordMapper;
import com.carsrecommend.system.mapper.UserDemandMapper;
import com.carsrecommend.system.service.RecommendationRecordService;
import com.carsrecommend.system.service.UserProfileService;
import com.carsrecommend.system.vo.DemandWeightsVO;
import com.carsrecommend.system.vo.RecommendationHistoryDetailVO;
import com.carsrecommend.system.vo.RecommendationHistoryItemVO;
import com.carsrecommend.system.vo.RecommendationItemVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class RecommendationRecordServiceImpl implements RecommendationRecordService {

    private static final long DEFAULT_DEMO_USER_ID = 1L;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final int TOP_CAR_NAME_LIMIT = 3;

    private final UserDemandMapper userDemandMapper;
    private final UserProfileService userProfileService;
    private final RecommendRecordMapper recommendRecordMapper;
    private final RecommendItemMapper recommendItemMapper;
    private final ObjectMapper objectMapper;

    public RecommendationRecordServiceImpl(
            UserDemandMapper userDemandMapper,
            UserProfileService userProfileService,
            RecommendRecordMapper recommendRecordMapper,
            RecommendItemMapper recommendItemMapper,
            ObjectMapper objectMapper) {
        this.userDemandMapper = userDemandMapper;
        this.userProfileService = userProfileService;
        this.recommendRecordMapper = recommendRecordMapper;
        this.recommendItemMapper = recommendItemMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RecommendationHistoryItemVO> history(Long userId, Integer page, Integer size) {
        Long resolvedUserId = resolveUserId(userId);
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        long total = recommendRecordMapper.countByUserId(resolvedUserId);
        long offset = (long) (pageNo - 1) * pageSize;
        List<RecommendationHistoryItemVO> records = recommendRecordMapper
                .findPageByUserId(resolvedUserId, pageSize, offset)
                .stream()
                .map(this::toHistoryItemVO)
                .toList();
        return PageResult.of(records, total, pageNo, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationHistoryDetailVO detail(Long recordId, Long userId) {
        Long resolvedUserId = resolveUserId(userId);
        RecommendRecord record = recommendRecordMapper.findByIdAndUserId(recordId, resolvedUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "recommend record not found"));

        RecommendationHistoryDetailVO vo = new RecommendationHistoryDetailVO();
        vo.setRecordId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setDemandId(record.getDemandId());
        vo.setProfileText(record.getProfileText());
        JsonNode weightSnapshot = readJsonNode(record.getWeightSnapshot());
        vo.setAlgorithmVersion(readAlgorithmVersion(weightSnapshot));
        vo.setAlpha(readAlpha(weightSnapshot));
        vo.setFallbackMessage(record.getFallbackMessage());
        vo.setRecommendStatus(record.getRecommendStatus());
        vo.setCreateTime(record.getCreateTime());
        vo.setWeights(readWeights(weightSnapshot));
        vo.setDemand(userProfileService.getDemandById(record.getDemandId()));
        vo.setItems(recommendItemMapper.findSnapshotsByRecordId(record.getId()).stream()
                .map(this::toRecommendationItemVO)
                .toList());
        return vo;
    }

    private Long resolveUserId(Long userId) {
        Long resolvedUserId = userId == null ? DEFAULT_DEMO_USER_ID : userId;
        if (!userDemandMapper.existsActiveUser(resolvedUserId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "app user not found");
        }
        return resolvedUserId;
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 1) {
            throw new BusinessException("page must be greater than or equal to 1");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException("size must be between 1 and 100");
        }
        return size;
    }

    private RecommendationHistoryItemVO toHistoryItemVO(RecommendRecord record) {
        RecommendationHistoryItemVO vo = new RecommendationHistoryItemVO();
        vo.setRecordId(record.getId());
        vo.setCreateTime(record.getCreateTime());
        vo.setProfileText(record.getProfileText());
        vo.setRecommendStatus(record.getRecommendStatus());
        vo.setFallbackMessage(record.getFallbackMessage());
        vo.setTopCarNames(recommendItemMapper.findTopCarNamesByRecordId(record.getId(), TOP_CAR_NAME_LIMIT));
        vo.setItemCount(recommendItemMapper.countByRecordId(record.getId()));
        return vo;
    }

    private RecommendationItemVO toRecommendationItemVO(RecommendItemSnapshot snapshot) {
        RecommendationItemVO vo = new RecommendationItemVO();
        vo.setRankNo(snapshot.rankNo());
        vo.setCarId(snapshot.carId());
        vo.setBrand(snapshot.brand());
        vo.setSeries(snapshot.series());
        vo.setModelName(snapshot.modelName());
        vo.setGuidePrice(snapshot.guidePrice());
        vo.setBodyType(snapshot.bodyType());
        vo.setEnergyType(snapshot.energyType());
        vo.setSeats(snapshot.seats());
        vo.setTotalScore(snapshot.totalScore());
        vo.setPriceScore(snapshot.priceScore());
        vo.setSpaceScore(snapshot.spaceScore());
        vo.setSafetyScore(snapshot.safetyScore());
        vo.setEnergyScore(snapshot.energyScore());
        vo.setIntelligenceScore(snapshot.intelligenceScore());
        vo.setComfortScore(snapshot.comfortScore());
        vo.setPowerScore(snapshot.powerScore());
        vo.setReputationScore(snapshot.reputationScore());
        vo.setPopularityScore(snapshot.popularityScore());
        vo.setMatchLevel(snapshot.matchLevel());
        vo.setTags(readStringList(snapshot.tags()));
        vo.setReasonText(snapshot.reasonText());
        vo.setWeaknessText(snapshot.weaknessText());
        return vo;
    }

    private DemandWeightsVO readWeights(JsonNode node) {
        JsonNode weightNode = node.path("finalWeight");
        if (!weightNode.isObject()) {
            weightNode = node;
        }
        DemandWeightsVO vo = new DemandWeightsVO();
        vo.setPrice(readDecimal(weightNode, "price"));
        vo.setSpace(readDecimal(weightNode, "space"));
        vo.setSafety(readDecimal(weightNode, "safety"));
        vo.setEnergy(readDecimal(weightNode, "energy"));
        vo.setIntelligence(readDecimal(weightNode, "intelligence"));
        vo.setComfort(readDecimal(weightNode, "comfort"));
        vo.setPower(readDecimal(weightNode, "power"));
        vo.setReputation(readDecimal(weightNode, "reputation"));
        vo.setPopularity(readDecimal(weightNode, "popularity"));
        return vo;
    }

    private String readAlgorithmVersion(JsonNode node) {
        JsonNode value = node.path("algorithmVersion");
        if (value.isTextual() && StringUtils.hasText(value.asText())) {
            return value.asText();
        }
        return "weighted-sum-v1";
    }

    private BigDecimal readAlpha(JsonNode node) {
        return readDecimal(node, "alpha");
    }

    private BigDecimal readDecimal(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || !StringUtils.hasText(value.asText())) {
            return null;
        }
        return value.isNumber() ? value.decimalValue() : new BigDecimal(value.asText());
    }

    private List<String> readStringList(String json) {
        JsonNode node = readJsonNode(json);
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return values;
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
            throw new IllegalStateException("failed to parse recommendation snapshot json field", exception);
        }
    }
}
