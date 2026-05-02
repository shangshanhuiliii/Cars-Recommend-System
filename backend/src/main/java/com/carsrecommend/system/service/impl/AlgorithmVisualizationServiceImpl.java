package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.common.enums.MatchLevel;
import com.carsrecommend.system.entity.CarFeatureScore;
import com.carsrecommend.system.entity.CarParam;
import com.carsrecommend.system.entity.RecommendRecord;
import com.carsrecommend.system.mapper.CarFeatureScoreMapper;
import com.carsrecommend.system.mapper.CarParamMapper;
import com.carsrecommend.system.mapper.RecommendItemMapper;
import com.carsrecommend.system.mapper.RecommendItemSnapshot;
import com.carsrecommend.system.mapper.RecommendRecordMapper;
import com.carsrecommend.system.mapper.UserDemandMapper;
import com.carsrecommend.system.service.AlgorithmVisualizationService;
import com.carsrecommend.system.service.UserProfileService;
import com.carsrecommend.system.vo.AlgorithmVisualizationConstraintVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationDemandVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationDimensionVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationFeatureScoreRuleVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationItemVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationMatrixRowVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationPipelineStepVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationStageStatVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationTopsisVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationWeightVO;
import com.carsrecommend.system.vo.UserDemandVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AlgorithmVisualizationServiceImpl implements AlgorithmVisualizationService {

    private static final long DEFAULT_DEMO_USER_ID = 1L;
    private static final int DISTANCE_SCALE = 8;
    private static final double ZERO_TOLERANCE = 0.00000001d;
    private static final String NEW_ALGORITHM_VERSION = "pareto-topsis-v1";
    private static final String LEGACY_ALGORITHM_VERSION = "weighted-sum-v1";
    private static final String SNAPSHOT_NOTE =
            "本页面数据来自推荐记录和推荐明细快照。算法可视化用于答辩展示，不会重新生成推荐，也不会写入数据库。";
    private static final String SNAPSHOT_SOURCE_NOTE =
            "当前车型参数和车型评分可用于说明评分来源，推荐排序、分数和解释仍以历史快照为准。";
    private static final String SNAPSHOT_MISMATCH_NOTE =
            "当前车型评分与推荐明细快照存在差异，页面展示的推荐依据以历史快照为准。";
    private static final String SNAPSHOT_MISSING_SOURCE_NOTE =
            "当前车型参数或车型评分缺失，页面仍按推荐明细快照展示历史推荐依据。";

    private final UserDemandMapper userDemandMapper;
    private final UserProfileService userProfileService;
    private final RecommendRecordMapper recommendRecordMapper;
    private final RecommendItemMapper recommendItemMapper;
    private final CarParamMapper carParamMapper;
    private final CarFeatureScoreMapper carFeatureScoreMapper;
    private final ParetoAnalyzer paretoAnalyzer;
    private final ObjectMapper objectMapper;

    public AlgorithmVisualizationServiceImpl(
            UserDemandMapper userDemandMapper,
            UserProfileService userProfileService,
            RecommendRecordMapper recommendRecordMapper,
            RecommendItemMapper recommendItemMapper,
            CarParamMapper carParamMapper,
            CarFeatureScoreMapper carFeatureScoreMapper,
            ParetoAnalyzer paretoAnalyzer,
            ObjectMapper objectMapper) {
        this.userDemandMapper = userDemandMapper;
        this.userProfileService = userProfileService;
        this.recommendRecordMapper = recommendRecordMapper;
        this.recommendItemMapper = recommendItemMapper;
        this.carParamMapper = carParamMapper;
        this.carFeatureScoreMapper = carFeatureScoreMapper;
        this.paretoAnalyzer = paretoAnalyzer;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AlgorithmVisualizationVO getVisualization(Long recordId, Long userId) {
        Long resolvedUserId = resolveUserId(userId);
        RecommendRecord record = recommendRecordMapper.findByIdAndUserId(recordId, resolvedUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "recommend record not found"));
        UserDemandVO demand = userProfileService.getDemandById(record.getDemandId());
        List<RecommendItemSnapshot> snapshots = recommendItemMapper.findSnapshotsByRecordId(record.getId());
        WeightSnapshot weights = readWeightSnapshot(record.getWeightSnapshot());
        ReconstructionResult reconstruction = reconstruct(snapshots, weights.finalWeight());
        Map<Integer, Boolean> paretoFlags = paretoFlags(snapshots, weights.finalWeight());
        List<AlgorithmVisualizationItemVO> items = buildItems(snapshots, reconstruction, paretoFlags);

        return new AlgorithmVisualizationVO(
                record.getId(),
                record.getDemandId(),
                record.getUserId(),
                weights.algorithmVersion(),
                weights.alpha(),
                record.getRecommendStatus(),
                record.getFallbackMessage(),
                record.getProfileText(),
                buildDemand(demand),
                buildConstraints(demand),
                dimensions(),
                new AlgorithmVisualizationWeightVO(
                        weights.subjectiveWeight(),
                        weights.objectiveWeight(),
                        weights.finalWeight()),
                buildStageStats(snapshots),
                pipeline(),
                buildMatrixRows(snapshots),
                items,
                featureScoreRules(),
                SNAPSHOT_NOTE,
                weights.compatibilityNote(),
                record.getCreateTime());
    }

    private Long resolveUserId(Long userId) {
        Long resolvedUserId = userId == null ? DEFAULT_DEMO_USER_ID : userId;
        if (!userDemandMapper.existsActiveUser(resolvedUserId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "app user not found");
        }
        return resolvedUserId;
    }

    private AlgorithmVisualizationDemandVO buildDemand(UserDemandVO demand) {
        return new AlgorithmVisualizationDemandVO(
                demand.getBudgetMin(),
                demand.getBudgetMax(),
                safeList(demand.getBodyTypes()),
                safeList(demand.getEnergyTypes()),
                safeList(demand.getScenes()),
                demand.getFactorWeights() == null ? Map.of() : demand.getFactorWeights(),
                demand.getMinSeats(),
                safeList(demand.getExcludedBrands()),
                safeList(demand.getExcludedCarIds()),
                demand.getProfileText());
    }

    private List<AlgorithmVisualizationConstraintVO> buildConstraints(UserDemandVO demand) {
        List<AlgorithmVisualizationConstraintVO> constraints = new ArrayList<>();
        constraints.add(new AlgorithmVisualizationConstraintVO(
                "预算上限",
                "hard",
                demand.getBudgetMax() == null ? "未设置" : formatWan(demand.getBudgetMax()) + "万元以内",
                "budgetMax 作为 STRICT 阶段预算硬约束，超出后只能进入预算放宽或相似推荐阶段。"));
        constraints.add(new AlgorithmVisualizationConstraintVO(
                "预算下限",
                "soft",
                demand.getBudgetMin() == null ? "未设置" : formatWan(demand.getBudgetMin()) + "万元",
                "budgetMin 不过滤候选，只影响推荐阶段动态 priceScore。"));
        constraints.add(new AlgorithmVisualizationConstraintVO(
                "车型类型",
                "hard",
                joinOrAny(demand.getBodyTypes(), "不限"),
                "bodyTypes 多选命中任一车型即可，车型放宽阶段按映射补充相近候选。"));
        constraints.add(new AlgorithmVisualizationConstraintVO(
                "动力类型",
                "hard",
                joinOrAny(demand.getEnergyTypes(), "不限"),
                "energyTypes 多选命中任一动力即可，新能源会展开为纯电、插混和增程。"));
        constraints.add(new AlgorithmVisualizationConstraintVO(
                "最低座位数",
                "hard",
                demand.getMinSeats() == null ? "未设置" : demand.getMinSeats() + "座以上",
                "minSeats 在 STRICT 和所有补充阶段都不放宽。"));
        constraints.add(new AlgorithmVisualizationConstraintVO(
                "排除品牌",
                "hard",
                joinOrAny(demand.getExcludedBrands(), "无"),
                "excludedBrands 始终作为硬约束，不参与降级放宽。"));
        constraints.add(new AlgorithmVisualizationConstraintVO(
                "排除车型",
                "hard",
                demand.getExcludedCarIds() == null || demand.getExcludedCarIds().isEmpty()
                        ? "无"
                        : demand.getExcludedCarIds().size() + "款",
                "excludedCarIds 始终作为硬约束，不参与降级放宽。"));
        constraints.add(new AlgorithmVisualizationConstraintVO(
                "使用场景和偏好权重",
                "soft",
                joinOrAny(demand.getScenes(), "综合需求"),
                "scenes 和 factorWeights 用于形成用户画像主观权重，不作为 SQL 筛选条件。"));
        return constraints;
    }

    private List<AlgorithmVisualizationDimensionVO> dimensions() {
        return List.of(
                new AlgorithmVisualizationDimensionVO("price", "价格", "根据用户预算动态计算 priceScore，不存入 car_feature_score。"),
                new AlgorithmVisualizationDimensionVO("space", "空间", "来自 recommend_item 快照中的 spaceScore，车型评分来源为轴距、车长、座位数和车型类型。"),
                new AlgorithmVisualizationDimensionVO("safety", "安全", "来自 recommend_item 快照中的 safetyScore，车型评分来源为 ABS、ESP、气囊和主动安全配置。"),
                new AlgorithmVisualizationDimensionVO("energy", "能耗", "来自 recommend_item 快照中的 energyScore，按燃油、纯电、插混和增程分别计算。"),
                new AlgorithmVisualizationDimensionVO("intelligence", "智能", "来自 recommend_item 快照中的 intelligenceScore，来源于车机和辅助驾驶配置。"),
                new AlgorithmVisualizationDimensionVO("comfort", "舒适", "来自 recommend_item 快照中的 comfortScore，当前由空间、智能和口碑组合估算。"),
                new AlgorithmVisualizationDimensionVO("power", "动力", "来自 recommend_item 快照中的 powerScore，主要来源于百公里加速。"),
                new AlgorithmVisualizationDimensionVO("reputation", "口碑", "来自 recommend_item 快照中的 reputationScore，来源于用户评分换算。"),
                new AlgorithmVisualizationDimensionVO("popularity", "热度", "来自 recommend_item 快照中的 popularityScore，来源于销量归一化。"));
    }

    private List<AlgorithmVisualizationStageStatVO> buildStageStats(List<RecommendItemSnapshot> snapshots) {
        Map<String, Long> countByLevel = new LinkedHashMap<>();
        for (RecommendItemSnapshot snapshot : snapshots) {
            countByLevel.merge(snapshot.matchLevel(), 1L, Long::sum);
        }
        return List.of(
                stageStat(MatchLevel.STRICT.getCode(), countByLevel),
                stageStat(MatchLevel.RELAX_BUDGET.getCode(), countByLevel),
                stageStat(MatchLevel.RELAX_BODY_TYPE.getCode(), countByLevel),
                stageStat(MatchLevel.RELAX_ENERGY_TYPE.getCode(), countByLevel),
                stageStat(MatchLevel.SIMILAR_RECOMMEND.getCode(), countByLevel));
    }

    private AlgorithmVisualizationStageStatVO stageStat(String matchLevel, Map<String, Long> countByLevel) {
        return new AlgorithmVisualizationStageStatVO(matchLevel, matchLabel(matchLevel), countByLevel.getOrDefault(matchLevel, 0L));
    }

    private List<AlgorithmVisualizationPipelineStepVO> pipeline() {
        return List.of(
                new AlgorithmVisualizationPipelineStepVO(1, "读取用户需求", "按 demandId 读取 user_demand，取得预算、车型、动力、场景和排除项。"),
                new AlgorithmVisualizationPipelineStepVO(2, "解析硬约束", "解析预算上限、车型集合、动力集合、最低座位数、排除品牌和排除车型。"),
                new AlgorithmVisualizationPipelineStepVO(3, "形成用户主观权重", "根据 factorWeights 或 scenes 模板得到 subjectiveWeight。"),
                new AlgorithmVisualizationPipelineStepVO(4, "加载候选车型和特征评分", "读取审核通过车型及 car_feature_score，车型评分由 car_param 规则计算得到。"),
                new AlgorithmVisualizationPipelineStepVO(5, "STRICT / 降级候选集", "按 STRICT、RELAX_BUDGET、RELAX_BODY_TYPE、RELAX_ENERGY_TYPE、SIMILAR_RECOMMEND 分阶段补充。"),
                new AlgorithmVisualizationPipelineStepVO(6, "动态价格分", "为每个候选按预算区间计算 priceScore，价格分只存在于推荐阶段。"),
                new AlgorithmVisualizationPipelineStepVO(7, "九维决策矩阵", "用 priceScore 加八个静态评分构造 price 到 popularity 的 9 维矩阵。"),
                new AlgorithmVisualizationPipelineStepVO(8, "熵权 objectiveWeight", "根据候选矩阵差异度计算 objectiveWeight，候选过少时退化。"),
                new AlgorithmVisualizationPipelineStepVO(9, "合成 finalWeight", "按 alpha 组合 subjectiveWeight 和 objectiveWeight 后再次归一化。"),
                new AlgorithmVisualizationPipelineStepVO(10, "Pareto 非支配识别", "在展示组内使用 finalWeight 最高的前 4 个维度标记被支配车型。"),
                new AlgorithmVisualizationPipelineStepVO(11, "TOPSIS 推荐分", "计算候选接近正理想解、远离负理想解的程度，得到 totalScore。"),
                new AlgorithmVisualizationPipelineStepVO(12, "分组与 rankNo", "STRICT 组在前，推荐组在后，组内按 Pareto 和 TOPSIS 排序并写入 rankNo。"),
                new AlgorithmVisualizationPipelineStepVO(13, "推荐解释", "依据贡献度和理想解差距形成 tags、reasonText 和 weaknessText。"),
                new AlgorithmVisualizationPipelineStepVO(14, "推荐快照持久化", "recommend_record 与 recommend_item 记录当次权重、分数、解释和匹配状态。"),
                new AlgorithmVisualizationPipelineStepVO(15, "返回推荐结果", "用户端按 rankNo 展示，历史详情和本页面均读取保存快照。"));
    }

    private List<AlgorithmVisualizationMatrixRowVO> buildMatrixRows(List<RecommendItemSnapshot> snapshots) {
        return snapshots.stream()
                .map(snapshot -> new AlgorithmVisualizationMatrixRowVO(
                        snapshot.rankNo(),
                        snapshot.carId(),
                        snapshot.brand() + " " + snapshot.modelName(),
                        snapshot.matchLevel(),
                        scores(snapshot)))
                .toList();
    }

    private List<AlgorithmVisualizationItemVO> buildItems(
            List<RecommendItemSnapshot> snapshots,
            ReconstructionResult reconstruction,
            Map<Integer, Boolean> paretoFlags) {
        List<AlgorithmVisualizationItemVO> items = new ArrayList<>();
        for (int index = 0; index < snapshots.size(); index++) {
            RecommendItemSnapshot snapshot = snapshots.get(index);
            SourceState sourceState = sourceState(snapshot);
            items.add(new AlgorithmVisualizationItemVO(
                    snapshot.rankNo(),
                    "STRICT".equals(snapshot.matchLevel()) ? "STRICT" : "RECOMMEND",
                    snapshot.carId(),
                    snapshot.brand(),
                    snapshot.series(),
                    snapshot.modelName(),
                    snapshot.guidePrice(),
                    snapshot.bodyType(),
                    snapshot.energyType(),
                    snapshot.matchLevel(),
                    matchLabel(snapshot.matchLevel()),
                    snapshot.totalScore(),
                    snapshot.priceScore(),
                    scores(snapshot),
                    paretoFlags.getOrDefault(snapshot.rankNo(), false),
                    reconstruction.topsisByRankNo().get(snapshot.rankNo()),
                    reconstruction.contributionByRankNo().getOrDefault(snapshot.rankNo(), Map.of()),
                    reconstruction.gapByRankNo().getOrDefault(snapshot.rankNo(), Map.of()),
                    readStringList(snapshot.tags()),
                    snapshot.reasonText(),
                    snapshot.weaknessText(),
                    sourceState.mismatch(),
                    sourceState.available(),
                    sourceState.note()));
        }
        return items;
    }

    private SourceState sourceState(RecommendItemSnapshot snapshot) {
        CarParam param = carParamMapper.findByCarId(snapshot.carId()).orElse(null);
        CarFeatureScore currentScore = carFeatureScoreMapper.findByCarId(snapshot.carId()).orElse(null);
        if (param == null || currentScore == null) {
            return new SourceState(false, false, SNAPSHOT_MISSING_SOURCE_NOTE);
        }
        boolean mismatch = !scoreEquals(snapshot.spaceScore(), currentScore.getSpaceScore())
                || !scoreEquals(snapshot.safetyScore(), currentScore.getSafetyScore())
                || !scoreEquals(snapshot.energyScore(), currentScore.getEnergyScore())
                || !scoreEquals(snapshot.intelligenceScore(), currentScore.getIntelligenceScore())
                || !scoreEquals(snapshot.comfortScore(), currentScore.getComfortScore())
                || !scoreEquals(snapshot.powerScore(), currentScore.getPowerScore())
                || !scoreEquals(snapshot.reputationScore(), currentScore.getReputationScore())
                || !scoreEquals(snapshot.popularityScore(), currentScore.getPopularityScore());
        return new SourceState(true, mismatch, mismatch ? SNAPSHOT_MISMATCH_NOTE : SNAPSHOT_SOURCE_NOTE);
    }

    private boolean scoreEquals(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.setScale(2, RoundingMode.HALF_UP)
                .compareTo(right.setScale(2, RoundingMode.HALF_UP)) == 0;
    }

    private Map<Integer, Boolean> paretoFlags(
            List<RecommendItemSnapshot> snapshots,
            Map<String, BigDecimal> finalWeight) {
        Map<Integer, Boolean> flags = new LinkedHashMap<>();
        fillParetoFlags(
                snapshots.stream().filter(snapshot -> "STRICT".equals(snapshot.matchLevel())).toList(),
                finalWeight,
                flags);
        fillParetoFlags(
                snapshots.stream().filter(snapshot -> !"STRICT".equals(snapshot.matchLevel())).toList(),
                finalWeight,
                flags);
        return flags;
    }

    private void fillParetoFlags(
            List<RecommendItemSnapshot> snapshots,
            Map<String, BigDecimal> finalWeight,
            Map<Integer, Boolean> flags) {
        ParetoAnalyzer.ParetoResult result = paretoAnalyzer.analyze(
                snapshots.stream().map(this::scoreVector).toList(),
                finalWeight);
        for (int index = 0; index < snapshots.size(); index++) {
            flags.put(snapshots.get(index).rankNo(), result.dominatedFlags().get(index));
        }
    }

    private ReconstructionResult reconstruct(
            List<RecommendItemSnapshot> snapshots,
            Map<String, BigDecimal> finalWeight) {
        if (snapshots.isEmpty()) {
            return new ReconstructionResult(Map.of(), Map.of(), Map.of());
        }
        double[][] normalizedMatrix = normalizedMatrix(snapshots);
        double[][] weightedMatrix = weightedMatrix(normalizedMatrix, finalWeight);
        double[] positiveIdeal = positiveIdeal(weightedMatrix);
        double[] negativeIdeal = negativeIdeal(weightedMatrix);

        Map<Integer, AlgorithmVisualizationTopsisVO> topsisByRankNo = new LinkedHashMap<>();
        Map<Integer, Map<String, BigDecimal>> contributionByRankNo = new LinkedHashMap<>();
        Map<Integer, Map<String, BigDecimal>> gapByRankNo = new LinkedHashMap<>();
        for (int row = 0; row < snapshots.size(); row++) {
            double positiveDistance = distance(weightedMatrix[row], positiveIdeal);
            double negativeDistance = distance(weightedMatrix[row], negativeIdeal);
            double sum = positiveDistance + negativeDistance;
            BigDecimal closeness = sum <= ZERO_TOLERANCE
                    ? divideByHundred(snapshots.get(row).totalScore())
                    : decimal(negativeDistance / sum, DISTANCE_SCALE);
            topsisByRankNo.put(snapshots.get(row).rankNo(), new AlgorithmVisualizationTopsisVO(
                    closeness,
                    decimal(positiveDistance, DISTANCE_SCALE),
                    decimal(negativeDistance, DISTANCE_SCALE)));
            Map<String, BigDecimal> contribution = toScoreMap(weightedMatrix[row], DISTANCE_SCALE);
            contributionByRankNo.put(snapshots.get(row).rankNo(), contribution);
            gapByRankNo.put(snapshots.get(row).rankNo(), gapMap(positiveIdeal, weightedMatrix[row]));
        }
        return new ReconstructionResult(topsisByRankNo, contributionByRankNo, gapByRankNo);
    }

    private double[][] normalizedMatrix(List<RecommendItemSnapshot> snapshots) {
        int rowCount = snapshots.size();
        int columnCount = RecommendationDimension.ORDERED.size();
        double[][] matrix = new double[rowCount][columnCount];
        for (int column = 0; column < columnCount; column++) {
            RecommendationDimension dimension = RecommendationDimension.ORDERED.get(column);
            double squareSum = 0d;
            for (RecommendItemSnapshot snapshot : snapshots) {
                double score = score(dimension, snapshot);
                squareSum += score * score;
            }
            double denominator = Math.sqrt(squareSum);
            for (int row = 0; row < rowCount; row++) {
                matrix[row][column] = denominator <= ZERO_TOLERANCE ? 0d : score(dimension, snapshots.get(row)) / denominator;
            }
        }
        return matrix;
    }

    private double[][] weightedMatrix(double[][] normalizedMatrix, Map<String, BigDecimal> finalWeight) {
        int rowCount = normalizedMatrix.length;
        int columnCount = RecommendationDimension.ORDERED.size();
        double[][] matrix = new double[rowCount][columnCount];
        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                RecommendationDimension dimension = RecommendationDimension.ORDERED.get(column);
                matrix[row][column] = normalizedMatrix[row][column]
                        * value(finalWeight.get(dimension.key())).doubleValue();
            }
        }
        return matrix;
    }

    private double[] positiveIdeal(double[][] weightedMatrix) {
        double[] values = new double[RecommendationDimension.ORDERED.size()];
        for (int column = 0; column < values.length; column++) {
            values[column] = Double.NEGATIVE_INFINITY;
            for (double[] row : weightedMatrix) {
                values[column] = Math.max(values[column], row[column]);
            }
        }
        return values;
    }

    private double[] negativeIdeal(double[][] weightedMatrix) {
        double[] values = new double[RecommendationDimension.ORDERED.size()];
        for (int column = 0; column < values.length; column++) {
            values[column] = Double.POSITIVE_INFINITY;
            for (double[] row : weightedMatrix) {
                values[column] = Math.min(values[column], row[column]);
            }
        }
        return values;
    }

    private double distance(double[] values, double[] ideal) {
        double squareSum = 0d;
        for (int index = 0; index < values.length; index++) {
            double diff = values[index] - ideal[index];
            squareSum += diff * diff;
        }
        return Math.sqrt(squareSum);
    }

    private Map<String, BigDecimal> gapMap(double[] positiveIdeal, double[] weightedValues) {
        Map<String, BigDecimal> gaps = new LinkedHashMap<>();
        for (int index = 0; index < RecommendationDimension.ORDERED.size(); index++) {
            gaps.put(
                    RecommendationDimension.ORDERED.get(index).key(),
                    decimal(Math.max(0d, positiveIdeal[index] - weightedValues[index]), DISTANCE_SCALE));
        }
        return gaps;
    }

    private Map<String, BigDecimal> toScoreMap(double[] values, int scale) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int index = 0; index < RecommendationDimension.ORDERED.size(); index++) {
            result.put(RecommendationDimension.ORDERED.get(index).key(), decimal(values[index], scale));
        }
        return result;
    }

    private RecommendationScoreVector scoreVector(RecommendItemSnapshot snapshot) {
        return new RecommendationScoreVector(
                snapshot.priceScore(),
                snapshot.spaceScore(),
                snapshot.safetyScore(),
                snapshot.energyScore(),
                snapshot.intelligenceScore(),
                snapshot.comfortScore(),
                snapshot.powerScore(),
                snapshot.reputationScore(),
                snapshot.popularityScore());
    }

    private Map<String, BigDecimal> scores(RecommendItemSnapshot snapshot) {
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        scores.put("price", snapshot.priceScore());
        scores.put("space", snapshot.spaceScore());
        scores.put("safety", snapshot.safetyScore());
        scores.put("energy", snapshot.energyScore());
        scores.put("intelligence", snapshot.intelligenceScore());
        scores.put("comfort", snapshot.comfortScore());
        scores.put("power", snapshot.powerScore());
        scores.put("reputation", snapshot.reputationScore());
        scores.put("popularity", snapshot.popularityScore());
        return scores;
    }

    private double score(RecommendationDimension dimension, RecommendItemSnapshot snapshot) {
        return value(scores(snapshot).get(dimension.key())).doubleValue();
    }

    private WeightSnapshot readWeightSnapshot(String json) {
        JsonNode node = readJsonNode(json);
        String algorithmVersion = readText(node, "algorithmVersion", LEGACY_ALGORITHM_VERSION);
        BigDecimal alpha = readDecimal(node, "alpha");
        Map<String, BigDecimal> flatWeights = readWeights(node);
        Map<String, BigDecimal> finalWeight = readWeights(node.path("finalWeight"));
        if (finalWeight.values().stream().allMatch(value -> value.compareTo(BigDecimal.ZERO) == 0)) {
            finalWeight = flatWeights;
        }
        Map<String, BigDecimal> subjectiveWeight = readWeights(node.path("subjectiveWeight"));
        if (subjectiveWeight.values().stream().allMatch(value -> value.compareTo(BigDecimal.ZERO) == 0)) {
            subjectiveWeight = finalWeight;
        }
        Map<String, BigDecimal> objectiveWeight = readWeights(node.path("objectiveWeight"));
        if (objectiveWeight.values().stream().allMatch(value -> value.compareTo(BigDecimal.ZERO) == 0)) {
            objectiveWeight = finalWeight;
        }
        String compatibilityNote = NEW_ALGORITHM_VERSION.equals(algorithmVersion)
                ? ""
                : "旧推荐记录未保存主客观组合权重和完整中间过程，本页面使用历史扁平权重作为展示兜底。";
        return new WeightSnapshot(
                algorithmVersion,
                alpha,
                subjectiveWeight,
                objectiveWeight,
                finalWeight,
                compatibilityNote);
    }

    private Map<String, BigDecimal> readWeights(JsonNode node) {
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        for (RecommendationDimension dimension : RecommendationDimension.ORDERED) {
            weights.put(dimension.key(), readDecimal(node, dimension.key(), BigDecimal.ZERO));
        }
        return weights;
    }

    private String readText(JsonNode node, String fieldName, String fallback) {
        JsonNode value = node.path(fieldName);
        return value.isTextual() && StringUtils.hasText(value.asText()) ? value.asText() : fallback;
    }

    private BigDecimal readDecimal(JsonNode node, String fieldName) {
        return readDecimal(node, fieldName, null);
    }

    private BigDecimal readDecimal(JsonNode node, String fieldName, BigDecimal fallback) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || !StringUtils.hasText(value.asText())) {
            return fallback;
        }
        return value.isNumber() ? value.decimalValue() : new BigDecimal(value.asText());
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
            throw new IllegalStateException("failed to parse algorithm visualization json field", exception);
        }
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

    private List<AlgorithmVisualizationFeatureScoreRuleVO> featureScoreRules() {
        return List.of(
                new AlgorithmVisualizationFeatureScoreRuleVO("price", "价格匹配分", "根据 budgetMin、budgetMax 与指导价动态计算，不进入 car_feature_score。"),
                new AlgorithmVisualizationFeatureScoreRuleVO("space", "空间分", "由轴距、车长、座位数和车型类型综合计算。"),
                new AlgorithmVisualizationFeatureScoreRuleVO("safety", "安全分", "由 ABS、ESP、气囊数量、主动刹车、车道保持等配置累加计算。"),
                new AlgorithmVisualizationFeatureScoreRuleVO("energy", "能耗分", "燃油车按油耗，纯电按纯电续航，插混和增程按综合续航计算。"),
                new AlgorithmVisualizationFeatureScoreRuleVO("intelligence", "智能分", "由语音控制、OTA、屏幕尺寸、影像、辅助驾驶和自动泊车配置计算。"),
                new AlgorithmVisualizationFeatureScoreRuleVO("comfort", "舒适分", "当前由空间分、智能分和口碑分按固定比例组合估算。"),
                new AlgorithmVisualizationFeatureScoreRuleVO("power", "动力分", "主要根据百公里加速时间换算，越快得分越高。"),
                new AlgorithmVisualizationFeatureScoreRuleVO("reputation", "口碑分", "由车型用户评分按 5 分制换算为 0-100。"),
                new AlgorithmVisualizationFeatureScoreRuleVO("popularity", "热度分", "由车型销量相对当前车型库最大销量归一化得到。"));
    }

    private String matchLabel(String matchLevel) {
        return switch (matchLevel) {
            case "STRICT" -> "完全匹配";
            case "RELAX_BUDGET" -> "放宽预算";
            case "RELAX_BODY_TYPE" -> "放宽车型";
            case "RELAX_ENERGY_TYPE" -> "放宽动力";
            case "SIMILAR_RECOMMEND" -> "相似推荐";
            default -> matchLevel == null ? "未知" : matchLevel;
        };
    }

    private String joinOrAny(List<?> values, String fallback) {
        return values == null || values.isEmpty() ? fallback : String.join(" / ", values.stream().map(String::valueOf).toList());
    }

    private String formatWan(BigDecimal value) {
        return value.divide(new BigDecimal("10000"), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private BigDecimal divideByHundred(BigDecimal value) {
        return value(value).divide(new BigDecimal("100"), DISTANCE_SCALE, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .min(BigDecimal.ONE);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record WeightSnapshot(
            String algorithmVersion,
            BigDecimal alpha,
            Map<String, BigDecimal> subjectiveWeight,
            Map<String, BigDecimal> objectiveWeight,
            Map<String, BigDecimal> finalWeight,
            String compatibilityNote) {
    }

    private record ReconstructionResult(
            Map<Integer, AlgorithmVisualizationTopsisVO> topsisByRankNo,
            Map<Integer, Map<String, BigDecimal>> contributionByRankNo,
            Map<Integer, Map<String, BigDecimal>> gapByRankNo) {
    }

    private record SourceState(
            boolean available,
            boolean mismatch,
            String note) {
    }
}
