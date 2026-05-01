package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.common.enums.BodyType;
import com.carsrecommend.system.common.enums.EnergyType;
import com.carsrecommend.system.dto.UserDemandSaveRequest;
import com.carsrecommend.system.entity.UserDemand;
import com.carsrecommend.system.mapper.UserDemandMapper;
import com.carsrecommend.system.service.UserProfileService;
import com.carsrecommend.system.vo.DemandWeightsVO;
import com.carsrecommend.system.vo.UserDemandVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class UserProfileServiceImpl implements UserProfileService {

    public static final long DEFAULT_DEMO_USER_ID = 1L;

    private static final String DEFAULT_SCENE = "综合需求";
    private static final BigDecimal ONE = new BigDecimal("1.0000");
    private static final BigDecimal FOCUS_INCREMENT = new BigDecimal("0.08");
    private static final BigDecimal MAX_FOCUS_INCREMENT_PER_DIMENSION = new BigDecimal("0.16");
    private static final BigDecimal MAX_RAW_WEIGHT_PER_DIMENSION = new BigDecimal("0.35");

    private static final List<WeightDimension> WEIGHT_ORDER = List.of(
            WeightDimension.PRICE,
            WeightDimension.SPACE,
            WeightDimension.SAFETY,
            WeightDimension.ENERGY,
            WeightDimension.INTELLIGENCE,
            WeightDimension.COMFORT,
            WeightDimension.POWER,
            WeightDimension.REPUTATION,
            WeightDimension.POPULARITY);

    private static final Map<String, EnumMap<WeightDimension, BigDecimal>> SCENE_TEMPLATES = Map.of(
            "城市通勤", template("0.25", "0.08", "0.12", "0.25", "0.15", "0.07", "0.04", "0.03", "0.01"),
            "家庭出行", template("0.10", "0.25", "0.25", "0.10", "0.08", "0.14", "0.03", "0.04", "0.01"),
            "长途自驾", template("0.08", "0.14", "0.20", "0.20", "0.08", "0.20", "0.06", "0.03", "0.01"),
            "新手代步", template("0.25", "0.06", "0.25", "0.14", "0.20", "0.04", "0.03", "0.02", "0.01"),
            "商务接待", template("0.05", "0.15", "0.12", "0.05", "0.15", "0.25", "0.05", "0.15", "0.03"),
            DEFAULT_SCENE, template("0.15", "0.13", "0.15", "0.13", "0.12", "0.12", "0.08", "0.07", "0.05"));

    private static final Map<String, String> SCENE_PROFILE_NAMES = Map.of(
            "城市通勤", "城市通勤型用户",
            "家庭出行", "家庭实用型用户",
            "长途自驾", "长途自驾型用户",
            "新手代步", "新手代步型用户",
            "商务接待", "商务接待型用户",
            DEFAULT_SCENE, "综合均衡型用户");

    private static final Map<String, WeightDimension> FOCUS_DIMENSIONS = Map.ofEntries(
            Map.entry("价格", WeightDimension.PRICE),
            Map.entry("性价比", WeightDimension.PRICE),
            Map.entry("不贵", WeightDimension.PRICE),
            Map.entry("空间", WeightDimension.SPACE),
            Map.entry("空间大", WeightDimension.SPACE),
            Map.entry("安全", WeightDimension.SAFETY),
            Map.entry("能耗", WeightDimension.ENERGY),
            Map.entry("省油", WeightDimension.ENERGY),
            Map.entry("续航", WeightDimension.ENERGY),
            Map.entry("用车成本", WeightDimension.ENERGY),
            Map.entry("智能", WeightDimension.INTELLIGENCE),
            Map.entry("科技", WeightDimension.INTELLIGENCE),
            Map.entry("辅助驾驶", WeightDimension.INTELLIGENCE),
            Map.entry("舒适", WeightDimension.COMFORT),
            Map.entry("动力", WeightDimension.POWER),
            Map.entry("动力强", WeightDimension.POWER),
            Map.entry("口碑", WeightDimension.REPUTATION),
            Map.entry("品牌可靠", WeightDimension.REPUTATION),
            Map.entry("热度", WeightDimension.POPULARITY),
            Map.entry("热门", WeightDimension.POPULARITY),
            Map.entry("销量", WeightDimension.POPULARITY));

    private final UserDemandMapper userDemandMapper;
    private final ObjectMapper objectMapper;

    public UserProfileServiceImpl(UserDemandMapper userDemandMapper, ObjectMapper objectMapper) {
        this.userDemandMapper = userDemandMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public UserDemandVO saveDemand(UserDemandSaveRequest request) {
        Long userId = resolveUserId(request.getUserId());
        String bodyType = normalizeBodyType(request.getBodyType());
        String energyType = normalizeDemandEnergyType(request.getEnergyType());
        String scene = normalizeScene(request.getScene());
        List<String> focusFactors = normalizeFocusFactors(request.getFocusFactors());
        List<String> excludedBrands = normalizeTextList(request.getExcludedBrands());
        List<Long> excludedCarIds = normalizeLongList(request.getExcludedCarIds());
        validateBudget(request.getBudgetMin(), request.getBudgetMax());

        EnumMap<WeightDimension, BigDecimal> weights = buildWeights(scene, focusFactors);
        UserDemand demand = new UserDemand();
        demand.setUserId(userId);
        demand.setRawText(trimToNull(request.getRawText()));
        demand.setBudgetMin(request.getBudgetMin());
        demand.setBudgetMax(request.getBudgetMax());
        demand.setBodyType(bodyType);
        demand.setEnergyType(energyType);
        demand.setSeats(request.getSeats());
        demand.setScene(scene);
        demand.setFocusFactors(toJson(focusFactors));
        demand.setExcludedBrands(toJson(excludedBrands));
        demand.setExcludedCarIds(toJson(excludedCarIds));
        demand.setProfileText(buildProfileText(scene, request.getBudgetMin(), request.getBudgetMax(),
                bodyType, energyType, focusFactors));
        applyWeights(demand, weights);

        UserDemand created = userDemandMapper.insert(demand);
        return getDemandById(created.getId());
    }

    @Override
    public UserDemandVO getLatestDemand(Long userId) {
        Long resolvedUserId = resolveUserId(userId);
        return userDemandMapper.findLatestByUserId(resolvedUserId)
                .map(this::toVO)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "user demand not found"));
    }

    @Override
    public UserDemandVO getDemandById(Long id) {
        return userDemandMapper.findById(id)
                .map(this::toVO)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "user demand not found"));
    }

    private Long resolveUserId(Long userId) {
        Long resolvedUserId = userId == null ? DEFAULT_DEMO_USER_ID : userId;
        if (!userDemandMapper.existsActiveUser(resolvedUserId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "app user not found");
        }
        return resolvedUserId;
    }

    private String normalizeBodyType(String bodyType) {
        String value = trimToNull(bodyType);
        if (value == null) {
            return null;
        }
        return BodyType.fromCode(value).getCode();
    }

    private String normalizeDemandEnergyType(String energyType) {
        String value = trimToNull(energyType);
        if (value == null) {
            return null;
        }
        return EnergyType.fromCode(value).getCode();
    }

    private String normalizeScene(String scene) {
        String value = trimToNull(scene);
        if (value == null) {
            return DEFAULT_SCENE;
        }
        if (!SCENE_TEMPLATES.containsKey(value)) {
            throw new BusinessException("unsupported scene: " + value);
        }
        return value;
    }

    private List<String> normalizeFocusFactors(List<String> focusFactors) {
        List<String> values = normalizeTextList(focusFactors);
        for (String value : values) {
            if (!FOCUS_DIMENSIONS.containsKey(value)) {
                throw new BusinessException("unsupported focusFactor: " + value);
            }
        }
        return values;
    }

    private List<String> normalizeTextList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                result.add(value.trim());
            }
        }
        return List.copyOf(result);
    }

    private List<Long> normalizeLongList(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (Long value : values) {
            if (value != null) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    private void validateBudget(BigDecimal budgetMin, BigDecimal budgetMax) {
        if (budgetMin != null && budgetMax != null && budgetMin.compareTo(budgetMax) > 0) {
            throw new BusinessException("budgetMin must not be greater than budgetMax");
        }
    }

    private EnumMap<WeightDimension, BigDecimal> buildWeights(String scene, List<String> focusFactors) {
        EnumMap<WeightDimension, BigDecimal> rawWeights = new EnumMap<>(SCENE_TEMPLATES.get(scene));
        EnumMap<WeightDimension, BigDecimal> increments = new EnumMap<>(WeightDimension.class);
        for (String focusFactor : focusFactors) {
            WeightDimension dimension = FOCUS_DIMENSIONS.get(focusFactor);
            BigDecimal current = increments.getOrDefault(dimension, BigDecimal.ZERO);
            BigDecimal next = current.add(FOCUS_INCREMENT).min(MAX_FOCUS_INCREMENT_PER_DIMENSION);
            increments.put(dimension, next);
        }
        for (Map.Entry<WeightDimension, BigDecimal> entry : increments.entrySet()) {
            BigDecimal adjusted = rawWeights.get(entry.getKey()).add(entry.getValue())
                    .min(MAX_RAW_WEIGHT_PER_DIMENSION);
            rawWeights.put(entry.getKey(), adjusted);
        }
        return normalizeWeights(rawWeights);
    }

    private EnumMap<WeightDimension, BigDecimal> normalizeWeights(EnumMap<WeightDimension, BigDecimal> rawWeights) {
        BigDecimal rawSum = rawWeights.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (rawSum.compareTo(BigDecimal.ZERO) <= 0) {
            rawWeights = new EnumMap<>(SCENE_TEMPLATES.get(DEFAULT_SCENE));
            rawSum = BigDecimal.ONE;
        }

        EnumMap<WeightDimension, BigDecimal> normalized = new EnumMap<>(WeightDimension.class);
        BigDecimal roundedSum = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        for (WeightDimension dimension : WEIGHT_ORDER) {
            BigDecimal value = rawWeights.get(dimension)
                    .divide(rawSum, 4, RoundingMode.HALF_UP);
            normalized.put(dimension, value);
            roundedSum = roundedSum.add(value);
        }

        BigDecimal diff = ONE.subtract(roundedSum);
        if (diff.signum() != 0) {
            WeightDimension target = WeightDimension.POPULARITY;
            normalized.put(target, normalized.get(target).add(diff).setScale(4, RoundingMode.HALF_UP));
        }
        return normalized;
    }

    private void applyWeights(UserDemand demand, EnumMap<WeightDimension, BigDecimal> weights) {
        demand.setWeightPrice(weights.get(WeightDimension.PRICE));
        demand.setWeightSpace(weights.get(WeightDimension.SPACE));
        demand.setWeightSafety(weights.get(WeightDimension.SAFETY));
        demand.setWeightEnergy(weights.get(WeightDimension.ENERGY));
        demand.setWeightIntelligence(weights.get(WeightDimension.INTELLIGENCE));
        demand.setWeightComfort(weights.get(WeightDimension.COMFORT));
        demand.setWeightPower(weights.get(WeightDimension.POWER));
        demand.setWeightReputation(weights.get(WeightDimension.REPUTATION));
        demand.setWeightPopularity(weights.get(WeightDimension.POPULARITY));
    }

    private String buildProfileText(
            String scene,
            BigDecimal budgetMin,
            BigDecimal budgetMax,
            String bodyType,
            String energyType,
            List<String> focusFactors) {
        String preferenceText;
        if (energyType != null && bodyType != null) {
            preferenceText = "偏好" + energyType + bodyType;
        } else if (energyType != null) {
            preferenceText = "偏好" + energyType + "动力";
        } else if (bodyType != null) {
            preferenceText = "偏好" + bodyType;
        } else {
            preferenceText = "车型动力不限";
        }

        String focusText = focusFactors.isEmpty()
                ? "关注因素较均衡"
                : "关注" + joinChinese(focusFactors);
        return SCENE_PROFILE_NAMES.get(scene) + "，"
                + buildBudgetText(budgetMin, budgetMax) + "，"
                + preferenceText + "，"
                + focusText + "。";
    }

    private String buildBudgetText(BigDecimal budgetMin, BigDecimal budgetMax) {
        if (budgetMin != null && budgetMax != null) {
            return "预算" + formatWan(budgetMin) + "-" + formatWan(budgetMax) + "万";
        }
        if (budgetMax != null) {
            return "预算" + formatWan(budgetMax) + "万以内";
        }
        if (budgetMin != null) {
            return "预算" + formatWan(budgetMin) + "万以上";
        }
        return "预算未限定";
    }

    private String formatWan(BigDecimal price) {
        return price.divide(new BigDecimal("10000"), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private String joinChinese(List<String> values) {
        if (values.size() == 1) {
            return values.get(0);
        }
        if (values.size() == 2) {
            return values.get(0) + "和" + values.get(1);
        }
        List<String> head = values.subList(0, values.size() - 1);
        return String.join("、", head) + "和" + values.get(values.size() - 1);
    }

    private UserDemandVO toVO(UserDemand demand) {
        UserDemandVO vo = new UserDemandVO();
        vo.setId(demand.getId());
        vo.setUserId(demand.getUserId());
        vo.setRawText(demand.getRawText());
        vo.setBudgetMin(demand.getBudgetMin());
        vo.setBudgetMax(demand.getBudgetMax());
        vo.setBodyType(demand.getBodyType());
        vo.setEnergyType(demand.getEnergyType());
        vo.setSeats(demand.getSeats());
        vo.setScene(demand.getScene());
        vo.setFocusFactors(readStringList(demand.getFocusFactors()));
        vo.setExcludedBrands(readStringList(demand.getExcludedBrands()));
        vo.setExcludedCarIds(readLongList(demand.getExcludedCarIds()));
        vo.setProfileText(demand.getProfileText());
        vo.setWeights(toWeightsVO(demand));
        vo.setCreateTime(demand.getCreateTime());
        vo.setUpdateTime(demand.getUpdateTime());
        return vo;
    }

    private DemandWeightsVO toWeightsVO(UserDemand demand) {
        DemandWeightsVO vo = new DemandWeightsVO();
        vo.setPrice(demand.getWeightPrice());
        vo.setSpace(demand.getWeightSpace());
        vo.setSafety(demand.getWeightSafety());
        vo.setEnergy(demand.getWeightEnergy());
        vo.setIntelligence(demand.getWeightIntelligence());
        vo.setComfort(demand.getWeightComfort());
        vo.setPower(demand.getWeightPower());
        vo.setReputation(demand.getWeightReputation());
        vo.setPopularity(demand.getWeightPopularity());
        return vo;
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
            throw new IllegalStateException("failed to parse user demand json field", exception);
        }
    }

    private String toJson(List<?> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize user demand json field", exception);
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static EnumMap<WeightDimension, BigDecimal> template(
            String price,
            String space,
            String safety,
            String energy,
            String intelligence,
            String comfort,
            String power,
            String reputation,
            String popularity) {
        EnumMap<WeightDimension, BigDecimal> values = new EnumMap<>(WeightDimension.class);
        values.put(WeightDimension.PRICE, new BigDecimal(price));
        values.put(WeightDimension.SPACE, new BigDecimal(space));
        values.put(WeightDimension.SAFETY, new BigDecimal(safety));
        values.put(WeightDimension.ENERGY, new BigDecimal(energy));
        values.put(WeightDimension.INTELLIGENCE, new BigDecimal(intelligence));
        values.put(WeightDimension.COMFORT, new BigDecimal(comfort));
        values.put(WeightDimension.POWER, new BigDecimal(power));
        values.put(WeightDimension.REPUTATION, new BigDecimal(reputation));
        values.put(WeightDimension.POPULARITY, new BigDecimal(popularity));
        return values;
    }

    private enum WeightDimension {
        PRICE,
        SPACE,
        SAFETY,
        ENERGY,
        INTELLIGENCE,
        COMFORT,
        POWER,
        REPUTATION,
        POPULARITY
    }
}
