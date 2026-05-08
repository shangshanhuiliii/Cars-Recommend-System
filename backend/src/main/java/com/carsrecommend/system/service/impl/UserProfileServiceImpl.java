package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.auth.AuthContext;
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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
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
    private static final Set<String> SEAT_OPTION_CODES = Set.of("2", "4", "5", "6", "7", "7_PLUS");

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

    private static final Map<String, EnumMap<WeightDimension, BigDecimal>> SCENE_TEMPLATES = Map.ofEntries(
            Map.entry(DEFAULT_SCENE, template("0.15", "0.13", "0.15", "0.13", "0.12", "0.12", "0.08", "0.07", "0.05")),
            Map.entry("城市通勤", template("0.22", "0.08", "0.12", "0.25", "0.14", "0.07", "0.05", "0.04", "0.03")),
            Map.entry("家庭出行", template("0.10", "0.24", "0.24", "0.10", "0.08", "0.14", "0.03", "0.05", "0.02")),
            Map.entry("长途自驾", template("0.08", "0.14", "0.20", "0.18", "0.08", "0.20", "0.06", "0.04", "0.02")),
            Map.entry("新手代步", template("0.24", "0.06", "0.24", "0.14", "0.20", "0.04", "0.03", "0.03", "0.02")),
            Map.entry("商务接待", template("0.05", "0.15", "0.12", "0.05", "0.14", "0.24", "0.06", "0.16", "0.03")),
            Map.entry("接送孩子", template("0.10", "0.18", "0.28", "0.12", "0.10", "0.12", "0.03", "0.05", "0.02")),
            Map.entry("露营旅行", template("0.08", "0.22", "0.16", "0.15", "0.07", "0.13", "0.10", "0.06", "0.03")),
            Map.entry("年轻运动", template("0.10", "0.06", "0.12", "0.10", "0.14", "0.08", "0.25", "0.07", "0.08")),
            Map.entry("豪华舒适", template("0.04", "0.16", "0.14", "0.06", "0.13", "0.28", "0.07", "0.10", "0.02")),
            Map.entry("低成本通勤", template("0.32", "0.06", "0.10", "0.28", "0.08", "0.04", "0.03", "0.05", "0.04")),
            Map.entry("科技智能", template("0.08", "0.08", "0.14", "0.12", "0.30", "0.08", "0.07", "0.06", "0.07")));

    private final UserDemandMapper userDemandMapper;
    private final ObjectMapper objectMapper;

    public UserProfileServiceImpl(UserDemandMapper userDemandMapper, ObjectMapper objectMapper) {
        this.userDemandMapper = userDemandMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public UserDemandVO saveDemand(UserDemandSaveRequest request) {
        Long userId = resolveUserId(request.getUserId());
        List<String> brands = normalizeTextList(request.getBrands());
        List<String> bodyTypes = normalizeBodyTypes(request.getBodyTypes());
        List<String> energyTypes = normalizeDemandEnergyTypes(request.getEnergyTypes());
        List<String> seatOptions = normalizeSeatOptions(request.getSeatOptions());
        List<String> scenes = normalizeScenes(request.getScenes());
        Map<String, Integer> factorWeights = normalizeFactorWeights(request.getFactorWeights());
        List<String> excludedBrands = normalizeTextList(request.getExcludedBrands());
        List<Long> excludedCarIds = normalizeLongList(request.getExcludedCarIds());
        validateBudget(request.getBudgetMin(), request.getBudgetMax());

        EnumMap<WeightDimension, BigDecimal> weights = buildWeights(scenes, factorWeights);
        UserDemand demand = new UserDemand();
        demand.setUserId(userId);
        demand.setRawText(trimToNull(request.getRawText()));
        demand.setBudgetMin(request.getBudgetMin());
        demand.setBudgetMax(request.getBudgetMax());
        demand.setBrands(toJson(brands));
        demand.setBodyTypes(toJson(bodyTypes));
        demand.setEnergyTypes(toJson(energyTypes));
        demand.setSeatOptions(toJson(seatOptions));
        demand.setMinSeats(request.getMinSeats());
        demand.setScenes(toJson(scenes));
        demand.setFactorWeights(toJson(factorWeights));
        demand.setExcludedBrands(toJson(excludedBrands));
        demand.setExcludedCarIds(toJson(excludedCarIds));
        demand.setProfileText(buildProfileText(
                request.getBudgetMin(),
                request.getBudgetMax(),
                brands,
                bodyTypes,
                energyTypes,
                seatOptions,
                scenes,
                factorWeights,
                excludedBrands,
                excludedCarIds));
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

    @Override
    public UserDemandVO getDemandById(Long id, Long userId) {
        Long resolvedUserId = resolveUserId(userId);
        return userDemandMapper.findByIdAndUserId(id, resolvedUserId)
                .map(this::toVO)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "user demand not found"));
    }

    private Long resolveUserId(Long userId) {
        Long currentUserId = AuthContext.currentUserIdOrNull();
        Long resolvedUserId = currentUserId != null ? currentUserId : (userId == null ? DEFAULT_DEMO_USER_ID : userId);
        if (!userDemandMapper.existsActiveUser(resolvedUserId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "app user not found");
        }
        return resolvedUserId;
    }

    private List<String> normalizeBodyTypes(List<String> bodyTypes) {
        List<String> values = normalizeTextList(bodyTypes);
        for (String value : values) {
            BodyType.fromCode(value);
        }
        return values;
    }

    private List<String> normalizeDemandEnergyTypes(List<String> energyTypes) {
        List<String> values = normalizeTextList(energyTypes);
        for (String value : values) {
            EnergyType.fromCode(value);
        }
        return values;
    }

    private List<String> normalizeSeatOptions(List<String> seatOptions) {
        List<String> values = normalizeTextList(seatOptions);
        for (String value : values) {
            if (!SEAT_OPTION_CODES.contains(value)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "unsupported seatOption: " + value);
            }
        }
        return values;
    }

    private List<String> normalizeScenes(List<String> scenes) {
        List<String> values = normalizeTextList(scenes);
        if (values.isEmpty()) {
            return List.of(DEFAULT_SCENE);
        }
        for (String value : values) {
            if (!SCENE_TEMPLATES.containsKey(value)) {
                throw new BusinessException("unsupported scene: " + value);
            }
        }
        return values;
    }

    private Map<String, Integer> normalizeFactorWeights(Map<String, Integer> factorWeights) {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        Map<String, Integer> source = factorWeights == null ? Map.of() : factorWeights;
        for (WeightDimension dimension : WEIGHT_ORDER) {
            Integer value = source.get(dimension.key());
            normalized.put(dimension.key(), value == null ? 0 : value);
        }
        for (String key : source.keySet()) {
            if (WEIGHT_ORDER.stream().noneMatch(dimension -> dimension.key().equals(key))) {
                throw new BusinessException("unsupported factorWeight: " + key);
            }
        }
        return normalized;
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

    private EnumMap<WeightDimension, BigDecimal> buildWeights(List<String> scenes, Map<String, Integer> factorWeights) {
        boolean hasExplicitWeight = factorWeights.values().stream().anyMatch(value -> value != null && value > 0);
        if (hasExplicitWeight) {
            EnumMap<WeightDimension, BigDecimal> rawWeights = new EnumMap<>(WeightDimension.class);
            for (WeightDimension dimension : WEIGHT_ORDER) {
                rawWeights.put(dimension, BigDecimal.valueOf(factorWeights.getOrDefault(dimension.key(), 0)));
            }
            return normalizeWeights(rawWeights);
        }

        EnumMap<WeightDimension, BigDecimal> averaged = new EnumMap<>(WeightDimension.class);
        for (WeightDimension dimension : WEIGHT_ORDER) {
            averaged.put(dimension, BigDecimal.ZERO);
        }
        List<String> sceneValues = scenes == null || scenes.isEmpty() ? List.of(DEFAULT_SCENE) : scenes;
        for (String scene : sceneValues) {
            EnumMap<WeightDimension, BigDecimal> template = SCENE_TEMPLATES.get(scene);
            for (WeightDimension dimension : WEIGHT_ORDER) {
                averaged.put(dimension, averaged.get(dimension).add(template.get(dimension)));
            }
        }
        BigDecimal divisor = BigDecimal.valueOf(sceneValues.size());
        for (WeightDimension dimension : WEIGHT_ORDER) {
            averaged.put(dimension, averaged.get(dimension).divide(divisor, 8, RoundingMode.HALF_UP));
        }
        return normalizeWeights(averaged);
    }

    private EnumMap<WeightDimension, BigDecimal> normalizeWeights(EnumMap<WeightDimension, BigDecimal> rawWeights) {
        BigDecimal rawSum = rawWeights.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (rawSum.compareTo(BigDecimal.ZERO) <= 0) {
            rawWeights = new EnumMap<>(SCENE_TEMPLATES.get(DEFAULT_SCENE));
            rawSum = rawWeights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
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
            BigDecimal budgetMin,
            BigDecimal budgetMax,
            List<String> brands,
            List<String> bodyTypes,
            List<String> energyTypes,
            List<String> seatOptions,
            List<String> scenes,
            Map<String, Integer> factorWeights,
            List<String> excludedBrands,
            List<Long> excludedCarIds) {
        List<String> parts = new ArrayList<>();
        parts.add(buildBudgetText(budgetMin, budgetMax));
        parts.add(brands.isEmpty() ? "品牌不限" : "优先在" + joinChinese(brands) + "中推荐");
        parts.add(bodyTypes.isEmpty() ? "可接受车型类型不限" : "可接受" + joinChinese(bodyTypes));
        parts.add(energyTypes.isEmpty() ? "可接受动力类型不限" : "可接受" + joinChinese(energyTypes) + "动力");
        parts.add(seatOptions.isEmpty() ? "座位数不限" : "座位偏好为" + joinChinese(seatOptionLabels(seatOptions)));
        parts.add("使用场景为" + joinChinese(scenes));

        List<String> highFactors = highFactorLabels(factorWeights);
        parts.add(highFactors.isEmpty() ? "偏好权重较均衡" : "重点关注" + joinChinese(highFactors));
        if (!excludedBrands.isEmpty()) {
            parts.add("排除品牌：" + joinChinese(excludedBrands));
        }
        if (!excludedCarIds.isEmpty()) {
            parts.add("已排除" + excludedCarIds.size() + "款车型");
        }
        return String.join("，", parts) + "。";
    }

    private List<String> seatOptionLabels(List<String> seatOptions) {
        return seatOptions.stream()
                .map(value -> switch (value) {
                    case "2" -> "2座";
                    case "4" -> "4座";
                    case "5" -> "5座";
                    case "6" -> "6座";
                    case "7" -> "7座";
                    case "7_PLUS" -> "7座以上";
                    default -> value;
                })
                .toList();
    }

    private List<String> highFactorLabels(Map<String, Integer> factorWeights) {
        return WEIGHT_ORDER.stream()
                .filter(dimension -> factorWeights.getOrDefault(dimension.key(), 0) > 0)
                .sorted(Comparator.comparing(
                        (WeightDimension dimension) -> factorWeights.getOrDefault(dimension.key(), 0))
                        .reversed())
                .limit(4)
                .map(WeightDimension::label)
                .toList();
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
        if (values.isEmpty()) {
            return "";
        }
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
        vo.setBrands(readStringList(demand.getBrands()));
        vo.setBodyTypes(readStringList(demand.getBodyTypes()));
        vo.setEnergyTypes(readStringList(demand.getEnergyTypes()));
        vo.setSeatOptions(readStringList(demand.getSeatOptions()));
        vo.setMinSeats(demand.getMinSeats());
        vo.setScenes(readStringList(demand.getScenes()));
        vo.setFactorWeights(readIntegerMap(demand.getFactorWeights()));
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

    private List<Long> readLongList(String json) {
        JsonNode node = readJsonNode(json);
        if (!node.isArray()) {
            return List.of();
        }
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

    private Map<String, Integer> readIntegerMap(String json) {
        JsonNode node = readJsonNode(json);
        Map<String, Integer> values = new LinkedHashMap<>();
        for (WeightDimension dimension : WEIGHT_ORDER) {
            JsonNode value = node.path(dimension.key());
            values.put(dimension.key(), value.isNumber() ? value.intValue() : 0);
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
            throw new IllegalStateException("failed to parse user demand json field", exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
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
        PRICE("price", "价格"),
        SPACE("space", "空间"),
        SAFETY("safety", "安全"),
        ENERGY("energy", "能耗"),
        INTELLIGENCE("intelligence", "智能"),
        COMFORT("comfort", "舒适"),
        POWER("power", "动力"),
        REPUTATION("reputation", "口碑"),
        POPULARITY("popularity", "热度");

        private final String key;
        private final String label;

        WeightDimension(String key, String label) {
            this.key = key;
            this.label = label;
        }

        String key() {
            return key;
        }

        String label() {
            return label;
        }
    }
}
