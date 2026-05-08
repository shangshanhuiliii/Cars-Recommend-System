package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.auth.AuthContext;
import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.common.enums.MatchLevel;
import com.carsrecommend.system.entity.CarFeatureScore;
import com.carsrecommend.system.entity.CarModel;
import com.carsrecommend.system.entity.CarParam;
import com.carsrecommend.system.entity.RecommendRecord;
import com.carsrecommend.system.mapper.CarFeatureScoreMapper;
import com.carsrecommend.system.mapper.CarModelMapper;
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
import com.carsrecommend.system.vo.AlgorithmVisualizationFeatureScoreExampleVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationFeatureScoreRuleVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationItemVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationMatrixRowVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationMatchedRuleVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationPipelineStepVO;
import com.carsrecommend.system.vo.AlgorithmVisualizationScoreBreakdownVO;
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
    private final CarModelMapper carModelMapper;
    private final CarParamMapper carParamMapper;
    private final CarFeatureScoreMapper carFeatureScoreMapper;
    private final ParetoAnalyzer paretoAnalyzer;
    private final ObjectMapper objectMapper;

    public AlgorithmVisualizationServiceImpl(
            UserDemandMapper userDemandMapper,
            UserProfileService userProfileService,
            RecommendRecordMapper recommendRecordMapper,
            RecommendItemMapper recommendItemMapper,
            CarModelMapper carModelMapper,
            CarParamMapper carParamMapper,
            CarFeatureScoreMapper carFeatureScoreMapper,
            ParetoAnalyzer paretoAnalyzer,
            ObjectMapper objectMapper) {
        this.userDemandMapper = userDemandMapper;
        this.userProfileService = userProfileService;
        this.recommendRecordMapper = recommendRecordMapper;
        this.recommendItemMapper = recommendItemMapper;
        this.carModelMapper = carModelMapper;
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
        return buildVisualization(record);
    }

    @Override
    @Transactional(readOnly = true)
    public AlgorithmVisualizationVO getVisualizationForAdmin(Long recordId) {
        RecommendRecord record = recommendRecordMapper.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "recommend record not found"));
        return buildVisualization(record);
    }

    private AlgorithmVisualizationVO buildVisualization(RecommendRecord record) {
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
                pipeline(record, demand, snapshots, weights),
                buildMatrixRows(snapshots),
                items,
                featureScoreRules(),
                buildFeatureScoreExample(snapshots),
                SNAPSHOT_NOTE,
                weights.compatibilityNote(),
                record.getCreateTime());
    }

    private Long resolveUserId(Long userId) {
        Long currentUserId = AuthContext.currentUserIdOrNull();
        Long resolvedUserId = currentUserId != null ? currentUserId : (userId == null ? DEFAULT_DEMO_USER_ID : userId);
        if (!userDemandMapper.existsActiveUser(resolvedUserId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "app user not found");
        }
        return resolvedUserId;
    }

    private AlgorithmVisualizationDemandVO buildDemand(UserDemandVO demand) {
        return new AlgorithmVisualizationDemandVO(
                demand.getBudgetMin(),
                demand.getBudgetMax(),
                safeList(demand.getBrands()),
                safeList(demand.getBodyTypes()),
                safeList(demand.getEnergyTypes()),
                safeList(demand.getSeatOptions()),
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
                "budgetMax 作为 STRICT 阶段预算区间上边界，高于该值的车型不能进入 STRICT。"));
        constraints.add(new AlgorithmVisualizationConstraintVO(
                "预算下限",
                "hard",
                demand.getBudgetMin() == null ? "未设置" : formatWan(demand.getBudgetMin()) + "万元",
                "budgetMin 作为 STRICT 阶段预算区间下边界，低于该值的车型不能进入 STRICT。"));
        constraints.add(new AlgorithmVisualizationConstraintVO(
                "品牌",
                "hard",
                joinOrAny(demand.getBrands(), "不限"),
                "brands 为空时不限制品牌；非空时只在指定品牌内生成候选。"));
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
                "座位数",
                "hard",
                joinOrAny(seatOptionLabels(demand.getSeatOptions()), "不限"),
                "seatOptions 为空时不限制座位数；非空时按选项并集过滤。"));
        constraints.add(new AlgorithmVisualizationConstraintVO(
                "最低座位数",
                "hard",
                demand.getMinSeats() == null ? "未设置" : demand.getMinSeats() + "座以上",
                "minSeats 作为旧字段兼容；当 seatOptions 为空时才用于座位硬约束。"));
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

    private List<AlgorithmVisualizationPipelineStepVO> pipeline(
            RecommendRecord record,
            UserDemandVO demand,
            List<RecommendItemSnapshot> snapshots,
            WeightSnapshot weights) {
        long strictCount = snapshots.stream()
                .filter(snapshot -> MatchLevel.STRICT.getCode().equals(snapshot.matchLevel()))
                .count();
        long fallbackCount = snapshots.size() - strictCount;
        String topCar = snapshots.isEmpty()
                ? "本次记录没有推荐明细"
                : "#" + snapshots.get(0).rankNo() + " " + snapshots.get(0).brand() + " " + snapshots.get(0).modelName();
        String budget = formatNullableWan(demand.getBudgetMin()) + " - " + formatNullableWan(demand.getBudgetMax());
        String alphaText = weights.alpha() == null ? "未记录" : weights.alpha().stripTrailingZeros().toPlainString();
        return List.of(
                new AlgorithmVisualizationPipelineStepVO(
                        1,
                        "读取用户需求",
                        "根据推荐记录中的 demandId 读取 user_demand，恢复用户预算、品牌、车型、动力、座位、场景和兼容排除项。",
                        "recommend_record.demand_id、user_demand",
                        "结构化用户需求 demand 与画像文本 profileText",
                        "demandId=" + record.getDemandId() + "，预算=" + budget + "万元，场景=" + joinOrAny(demand.getScenes(), "未设置"),
                        "RecommendationRecordServiceImpl / UserProfileServiceImpl"),
                new AlgorithmVisualizationPipelineStepVO(
                        2,
                        "解析硬性约束",
                        "把预算区间、品牌、车型类型、动力类型、座位选项和兼容排除字段拆成候选过滤条件。",
                        "budgetMin、budgetMax、brands、bodyTypes、energyTypes、seatOptions、minSeats、excludedBrands、excludedCarIds",
                        "STRICT 阶段硬约束和后续降级阶段保留约束",
                        "品牌=" + joinOrAny(demand.getBrands(), "不限")
                                + "，车型=" + joinOrAny(demand.getBodyTypes(), "不限")
                                + "，动力=" + joinOrAny(demand.getEnergyTypes(), "不限")
                                + "，座位=" + joinOrAny(seatOptionLabels(demand.getSeatOptions()), "不限"),
                        "RecommendationCandidateService"),
                new AlgorithmVisualizationPipelineStepVO(
                        3,
                        "生成用户主观权重 subjectiveWeight",
                        "根据用户显式 factorWeights 或使用场景模板生成九维用户主观权重，并归一化到总和为 1。",
                        "user_demand.factor_weights、user_demand.scenes",
                        "subjectiveWeight",
                        "本次 subjectiveWeight 已返回 " + weights.subjectiveWeight().size() + " 个维度，最高维度为 "
                                + topWeightLabel(weights.subjectiveWeight()),
                        "UserProfileServiceImpl / RecommendationWeightService"),
                new AlgorithmVisualizationPipelineStepVO(
                        4,
                        "加载候选车型和车型特征评分",
                        "读取审核通过车型、car_param 和 car_feature_score。八维车型特征评分由车型参数规则计算，priceScore 不在该表保存。",
                        "car_model、car_param、car_feature_score",
                        "候选车型基础信息与八维静态评分",
                        "本次可视化推荐明细共 " + snapshots.size() + " 条，rankNo=1 车型用于评分示例：" + topCar,
                        "CarFeatureScoreCalculator / CarFeatureScoreService"),
                new AlgorithmVisualizationPipelineStepVO(
                        5,
                        "生成 STRICT / 降级候选集",
                        "先生成满足完整预算区间、车型、动力等硬约束的 STRICT 候选；预算区间外但接近预算的车型只能进入预算放宽推荐组。",
                        "用户硬约束、审核通过车型、排除项",
                        "带 matchLevel 的候选集合",
                        "STRICT=" + strictCount + " 条，补充推荐=" + fallbackCount + " 条，推荐状态=" + record.getRecommendStatus(),
                        "RecommendationCandidateService"),
                new AlgorithmVisualizationPipelineStepVO(
                        6,
                        "计算动态价格分 priceScore",
                        "根据用户预算区间和车型指导价临时计算 priceScore，用于表示车型价格与用户预算区间的接近程度。",
                        "budgetMin、budgetMax、car_model.guide_price",
                        "每个候选车的 priceScore",
                        snapshots.isEmpty()
                                ? "本次记录无 priceScore"
                                : topCar + " 的 priceScore=" + formatDecimal(snapshots.get(0).priceScore()),
                        "PriceScoreCalculator"),
                new AlgorithmVisualizationPipelineStepVO(
                        7,
                        "构造九维评分矩阵",
                        "将动态 priceScore 与八维车型特征评分合并，形成 price、space、safety、energy、intelligence、comfort、power、reputation、popularity 九维矩阵。",
                        "recommend_item 快照中的九维分数",
                        "用于权重、Pareto 和 TOPSIS 的决策矩阵",
                        "本次矩阵行数=" + snapshots.size() + "，列数=" + RecommendationDimension.ORDERED.size() + "，按 rankNo 升序展示",
                        "RecommendationServiceImpl / TopsisRanker"),
                new AlgorithmVisualizationPipelineStepVO(
                        8,
                        "计算熵权法客观权重 objectiveWeight",
                        "熵权法根据候选车型九维指标差异计算客观权重；差异越能区分候选车，客观权重越高。",
                        "九维评分矩阵",
                        "objectiveWeight",
                        "本次 objectiveWeight 已返回 " + weights.objectiveWeight().size() + " 个维度，最高维度为 "
                                + topWeightLabel(weights.objectiveWeight()),
                        "RecommendationWeightService"),
                new AlgorithmVisualizationPipelineStepVO(
                        9,
                        "合成主客观组合权重 finalWeight",
                        "用 alpha 组合 subjectiveWeight 与 objectiveWeight，再归一化形成最终排序权重 finalWeight。",
                        "subjectiveWeight、objectiveWeight、alpha",
                        "finalWeight",
                        "alpha=" + alphaText + "，本次 finalWeight 最高维度为 " + topWeightLabel(weights.finalWeight()),
                        "RecommendationWeightService"),
                new AlgorithmVisualizationPipelineStepVO(
                        10,
                        "识别 Pareto 非支配车型",
                        "在 STRICT 组和补充推荐组内分别比较候选车高权重维度，标记被其他车型全面压制的车型。",
                        "分组候选矩阵、finalWeight",
                        "paretoDominated 布尔标记",
                        "本次 Pareto 标记基于快照矩阵临时重构，仅用于答辩展示。",
                        "ParetoAnalyzer"),
                new AlgorithmVisualizationPipelineStepVO(
                        11,
                        "计算 TOPSIS 推荐分 totalScore",
                        "TOPSIS 比较候选车与正理想解、负理想解的距离；越接近正理想解且越远离负理想解，推荐分越高。",
                        "归一化九维矩阵、finalWeight、正理想解、负理想解",
                        "closeness、positiveDistance、negativeDistance、totalScore",
                        snapshots.isEmpty()
                                ? "本次记录无 TOPSIS 结果"
                                : topCar + " 的快照 totalScore=" + formatDecimal(snapshots.get(0).totalScore()),
                        "TopsisRanker"),
                new AlgorithmVisualizationPipelineStepVO(
                        12,
                        "按 matchLevel 分组并写入 rankNo",
                        "推荐生成时先展示 STRICT 组，再展示补充推荐组；组内先按 TOPSIS totalScore 排序，同分时再使用 Pareto 标记、口碑分和热度分作为辅助排序。",
                        "matchLevel、totalScore、paretoDominated、reputationScore、popularityScore",
                        "按 rankNo 排序的推荐明细",
                        "本页面不重新排序，直接按 recommend_item.rank_no 展示；第一名为 " + topCar,
                        "RecommendationServiceImpl"),
                new AlgorithmVisualizationPipelineStepVO(
                        13,
                        "生成推荐解释",
                        "根据高贡献维度生成 tags 和 reasonText，根据高权重但差距较大的维度生成 weaknessText。",
                        "finalWeight、TOPSIS 贡献度、理想解差距、车型九维分数",
                        "tags、reasonText、weaknessText",
                        snapshots.isEmpty()
                                ? "本次记录无解释文本"
                                : "rankNo=1 已保存 tags、reasonText、weaknessText，页面按快照读取。",
                        "RecommendationExplanationService"),
                new AlgorithmVisualizationPipelineStepVO(
                        14,
                        "保存推荐快照",
                        "推荐生成阶段把需求、权重、分数、解释和匹配状态写入 recommend_record 与 recommend_item，供历史追溯。",
                        "推荐响应对象、权重快照、推荐明细",
                        "recommend_record 与 recommend_item 历史快照",
                        "本接口只读取已存在快照，不执行任何写入或删除。",
                        "RecommendationRecordServiceImpl / RecommendationServiceImpl"),
                new AlgorithmVisualizationPipelineStepVO(
                        15,
                        "返回推荐结果",
                        "普通用户端只展示易懂的推荐结果；管理端和答辩页可以展示算法细节和中间过程。",
                        "recommend_record、recommend_item、车型关联信息",
                        "推荐结果页、历史详情、算法可视化答辩页",
                        "本页面仅服务答辩展示，不影响 /recommend 普通用户推荐页。",
                        "RecommendationController / AlgorithmVisualizationController"));
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

    private AlgorithmVisualizationFeatureScoreExampleVO buildFeatureScoreExample(List<RecommendItemSnapshot> snapshots) {
        RecommendItemSnapshot snapshot = snapshots.stream()
                .filter(item -> item.rankNo() != null && item.rankNo() == 1)
                .findFirst()
                .orElse(snapshots.isEmpty() ? null : snapshots.get(0));
        if (snapshot == null) {
            return null;
        }
        CarModel carModel = carModelMapper.findById(snapshot.carId()).orElse(null);
        CarParam param = carParamMapper.findByCarId(snapshot.carId()).orElse(null);
        int maxSalesVolume = carModelMapper.findMaxSalesVolume();
        return new AlgorithmVisualizationFeatureScoreExampleVO(
                snapshot.carId(),
                snapshot.brand(),
                snapshot.modelName(),
                snapshot.guidePrice(),
                snapshot.bodyType(),
                snapshot.energyType(),
                snapshot.seats(),
                featureParams(carModel, param, maxSalesVolume),
                featureScores(snapshot),
                featureScoreBreakdown(snapshot, carModel, param, maxSalesVolume));
    }

    private Map<String, Object> featureParams(CarModel carModel, CarParam param, int maxSalesVolume) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("lengthMm", param == null ? null : param.getLengthMm());
        params.put("wheelbaseMm", param == null ? null : param.getWheelbaseMm());
        params.put("fuelConsumption", param == null ? null : param.getFuelConsumption());
        params.put("electricRangeKm", param == null ? null : param.getElectricRangeKm());
        params.put("totalRangeKm", param == null ? null : param.getTotalRangeKm());
        params.put("acceleration100", param == null ? null : param.getAcceleration100());
        params.put("airbagCount", param == null ? null : param.getAirbagCount());
        params.put("hasAbs", param == null ? null : param.getHasAbs());
        params.put("hasEsp", param == null ? null : param.getHasEsp());
        params.put("hasActiveBrake", param == null ? null : param.getHasActiveBrake());
        params.put("hasLaneKeep", param == null ? null : param.getHasLaneKeep());
        params.put("hasAdaptiveCruise", param == null ? null : param.getHasAdaptiveCruise());
        params.put("hasBlindSpot", param == null ? null : param.getHasBlindSpot());
        params.put("hasReverseCamera", param == null ? null : param.getHasReverseCamera());
        params.put("has360Camera", param == null ? null : param.getHas360Camera());
        params.put("hasOta", param == null ? null : param.getHasOta());
        params.put("hasVoiceControl", param == null ? null : param.getHasVoiceControl());
        params.put("hasAutoParking", param == null ? null : param.getHasAutoParking());
        params.put("screenSize", param == null ? null : param.getScreenSize());
        params.put("assistDriveLevel", param == null ? null : param.getAssistDriveLevel());
        params.put("salesVolume", carModel == null ? null : carModel.getSalesVolume());
        params.put("maxSalesVolume", maxSalesVolume);
        params.put("userRating", carModel == null ? null : carModel.getUserRating());
        return params;
    }

    private Map<String, BigDecimal> featureScores(RecommendItemSnapshot snapshot) {
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
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

    private List<AlgorithmVisualizationScoreBreakdownVO> featureScoreBreakdown(
            RecommendItemSnapshot snapshot,
            CarModel carModel,
            CarParam param,
            int maxSalesVolume) {
        return List.of(
                spaceBreakdown(snapshot, param),
                safetyBreakdown(snapshot, param),
                energyBreakdown(snapshot, param),
                intelligenceBreakdown(snapshot, param),
                comfortBreakdown(snapshot),
                powerBreakdown(snapshot, param),
                reputationBreakdown(snapshot, carModel),
                popularityBreakdown(snapshot, carModel, maxSalesVolume));
    }

    private AlgorithmVisualizationScoreBreakdownVO spaceBreakdown(RecommendItemSnapshot snapshot, CarParam param) {
        List<AlgorithmVisualizationMatchedRuleVO> rules = new ArrayList<>();
        Integer wheelbase = param == null ? null : param.getWheelbaseMm();
        if (wheelbase == null) {
            rules.add(rule("轴距缺失，使用默认空间基础分", 60, "没有 wheelbaseMm 时空间基础分按 60 处理。"));
        } else if (wheelbase < 2600) {
            rules.add(rule("轴距 < 2600mm", 50, "轴距较短，空间基础分为 50。"));
        } else if (wheelbase < 2700) {
            rules.add(rule("2600mm <= 轴距 < 2700mm", 65, "轴距进入紧凑级区间，空间基础分为 65。"));
        } else if (wheelbase < 2800) {
            rules.add(rule("2700mm <= 轴距 < 2800mm", 80, "轴距进入主流家用区间，空间基础分为 80。"));
        } else if (wheelbase < 2900) {
            rules.add(rule("2800mm <= 轴距 < 2900mm", 90, "轴距较长，空间基础分为 90。"));
        } else {
            rules.add(rule("轴距 >= 2900mm", 95, "轴距很长，空间基础分为 95。"));
        }
        if ("SUV".equals(snapshot.bodyType())) {
            rules.add(rule("SUV 车型加分", 3, "SUV 对家庭和通过性场景更友好，空间分增加 3。"));
        } else if ("MPV".equals(snapshot.bodyType())) {
            rules.add(rule("MPV 车型加分", 8, "MPV 对多人乘坐和载物更友好，空间分增加 8。"));
        }
        if (snapshot.seats() != null && snapshot.seats() >= 7) {
            rules.add(rule("座位数 >= 7", 5, "七座及以上满足多人出行，空间分增加 5。"));
        }
        Integer length = param == null ? null : param.getLengthMm();
        if (length != null && length > 4800) {
            rules.add(rule("车长 > 4800mm", 5, "车长较大，空间分增加 5。"));
        }
        return breakdown(
                "space",
                "空间分 spaceScore",
                snapshot.spaceScore(),
                "轴距决定基础分，SUV/MPV、七座和长车身继续加分，最后截断到 0-100。",
                rules,
                "rankNo=1 车型的空间分以 recommend_item.space_score 快照为准；上述规则用于展示该分数来源。");
    }

    private AlgorithmVisualizationScoreBreakdownVO safetyBreakdown(RecommendItemSnapshot snapshot, CarParam param) {
        List<AlgorithmVisualizationMatchedRuleVO> rules = new ArrayList<>();
        rules.add(rule("安全基础分", 30, "安全维度从基础分 30 开始累加配置。"));
        if (param == null) {
            rules.add(rule("参数缺失", 0, "没有 car_param 时只能展示推荐快照保存的 safetyScore。"));
        } else {
            if (Boolean.TRUE.equals(param.getHasAbs())) {
                rules.add(rule("ABS 防抱死", 10, "配置 ABS，安全分增加 10。"));
            }
            if (Boolean.TRUE.equals(param.getHasEsp())) {
                rules.add(rule("ESP 车身稳定系统", 15, "配置 ESP，安全分增加 15。"));
            }
            if (param.getAirbagCount() != null && param.getAirbagCount() >= 6) {
                rules.add(rule("气囊数量 >= 6", 20, "气囊数量达到 6 个及以上，安全分增加 20。"));
            }
            if (Boolean.TRUE.equals(param.getHasActiveBrake())) {
                rules.add(rule("主动刹车", 15, "配置主动刹车，安全分增加 15。"));
            }
            if (Boolean.TRUE.equals(param.getHasLaneKeep())) {
                rules.add(rule("车道保持", 10, "配置车道保持，安全分增加 10。"));
            }
            if (Boolean.TRUE.equals(param.getHasAdaptiveCruise())) {
                rules.add(rule("自适应巡航", 10, "配置自适应巡航，安全分增加 10。"));
            }
            if (Boolean.TRUE.equals(param.getHasBlindSpot())) {
                rules.add(rule("盲区监测", 5, "配置盲区监测，安全分增加 5。"));
            }
        }
        return breakdown(
                "safety",
                "安全分 safetyScore",
                snapshot.safetyScore(),
                "基础 30 分，按安全配置累加，最后截断到 0-100。",
                rules,
                "安全分展示 ABS、ESP、气囊和主动安全配置的累计贡献。");
    }

    private AlgorithmVisualizationScoreBreakdownVO energyBreakdown(RecommendItemSnapshot snapshot, CarParam param) {
        List<AlgorithmVisualizationMatchedRuleVO> rules = new ArrayList<>();
        String energyType = snapshot.energyType();
        if (param == null) {
            rules.add(rule("参数缺失兜底", 60, "没有 car_param 时能耗分按兜底值说明，推荐仍使用快照 energyScore。"));
        } else if ("燃油".equals(energyType)) {
            BigDecimal consumption = param.getFuelConsumption();
            rules.add(energyRuleByFuelConsumption(consumption));
        } else if ("纯电".equals(energyType)) {
            Integer range = param.getElectricRangeKm();
            rules.add(energyRuleByElectricRange(range));
        } else if ("插混".equals(energyType) || "增程".equals(energyType)) {
            Integer range = param.getTotalRangeKm();
            rules.add(energyRuleByTotalRange(range));
        } else {
            rules.add(rule("未知动力类型兜底", 60, "动力类型未命中燃油、纯电、插混或增程，能耗分按 60 说明。"));
        }
        return breakdown(
                "energy",
                "能耗分 energyScore",
                snapshot.energyScore(),
                "按动力类型分档：燃油看油耗，纯电看纯电续航，插混/增程看综合续航。",
                rules,
                "能耗分不是前端计算结果；本页只按当前参数解释快照中的 energyScore。");
    }

    private AlgorithmVisualizationScoreBreakdownVO intelligenceBreakdown(RecommendItemSnapshot snapshot, CarParam param) {
        List<AlgorithmVisualizationMatchedRuleVO> rules = new ArrayList<>();
        if (param == null) {
            rules.add(rule("参数缺失兜底", 50, "没有 car_param 时智能分按兜底值说明，推荐仍使用快照 intelligenceScore。"));
        } else {
            if (Boolean.TRUE.equals(param.getHasVoiceControl())) {
                rules.add(rule("语音控制", 10, "支持语音控制，智能分增加 10。"));
            }
            if (Boolean.TRUE.equals(param.getHasOta())) {
                rules.add(rule("OTA 升级", 10, "支持 OTA，智能分增加 10。"));
            }
            if (param.getScreenSize() != null && param.getScreenSize().doubleValue() >= 12) {
                rules.add(rule("屏幕尺寸 >= 12 英寸", 10, "中控屏尺寸达到 12 英寸及以上，智能分增加 10。"));
            }
            if (Boolean.TRUE.equals(param.getHasReverseCamera())) {
                rules.add(rule("倒车影像", 8, "配置倒车影像，智能分增加 8。"));
            }
            if (Boolean.TRUE.equals(param.getHas360Camera())) {
                rules.add(rule("360 全景影像", 12, "配置 360 全景影像，智能分增加 12。"));
            }
            if ("L2".equalsIgnoreCase(param.getAssistDriveLevel())) {
                rules.add(rule("L2 辅助驾驶", 20, "辅助驾驶等级为 L2，智能分增加 20。"));
            }
            if (Boolean.TRUE.equals(param.getHasAutoParking())) {
                rules.add(rule("自动泊车", 10, "配置自动泊车，智能分增加 10。"));
            }
            if (rules.isEmpty()) {
                rules.add(rule("未命中智能配置", 0, "没有命中语音、OTA、影像、辅助驾驶或自动泊车配置。"));
            }
        }
        return breakdown(
                "intelligence",
                "智能分 intelligenceScore",
                snapshot.intelligenceScore(),
                "从 0 分开始按智能座舱和辅助驾驶配置累加，最后截断到 0-100。",
                rules,
                "智能分展示车机能力、影像配置和辅助驾驶配置的累计贡献。");
    }

    private AlgorithmVisualizationScoreBreakdownVO comfortBreakdown(RecommendItemSnapshot snapshot) {
        List<AlgorithmVisualizationMatchedRuleVO> rules = List.of(
                rule("空间分贡献", value(snapshot.spaceScore()).doubleValue() * 0.5,
                        "comfortScore 中空间分占 50%，本车贡献 " + formatDecimal(value(snapshot.spaceScore()).multiply(new BigDecimal("0.5"))) + "。"),
                rule("智能分贡献", value(snapshot.intelligenceScore()).doubleValue() * 0.2,
                        "comfortScore 中智能分占 20%，本车贡献 " + formatDecimal(value(snapshot.intelligenceScore()).multiply(new BigDecimal("0.2"))) + "。"),
                rule("口碑分贡献", value(snapshot.reputationScore()).doubleValue() * 0.3,
                        "comfortScore 中口碑分占 30%，本车贡献 " + formatDecimal(value(snapshot.reputationScore()).multiply(new BigDecimal("0.3"))) + "。"));
        return breakdown(
                "comfort",
                "舒适分 comfortScore",
                snapshot.comfortScore(),
                "comfortScore = spaceScore * 0.5 + intelligenceScore * 0.2 + reputationScore * 0.3。",
                rules,
                "舒适分是组合指标，直接使用推荐快照中的空间、智能和口碑分进行解释。");
    }

    private AlgorithmVisualizationScoreBreakdownVO powerBreakdown(RecommendItemSnapshot snapshot, CarParam param) {
        List<AlgorithmVisualizationMatchedRuleVO> rules = new ArrayList<>();
        BigDecimal acceleration = param == null ? null : param.getAcceleration100();
        if (acceleration == null) {
            rules.add(rule("百公里加速缺失兜底", 60, "没有 acceleration100 时动力分按 60 说明。"));
        } else {
            double value = acceleration.doubleValue();
            if (value <= 4) {
                rules.add(rule("0-100km/h 加速 <= 4s", 100, "加速性能极强，动力分为 100。"));
            } else if (value <= 6) {
                rules.add(rule("0-100km/h 加速 <= 6s", 90, "加速性能优秀，动力分为 90。"));
            } else if (value <= 8) {
                rules.add(rule("0-100km/h 加速 <= 8s", 80, "加速性能较好，动力分为 80。"));
            } else if (value <= 10) {
                rules.add(rule("0-100km/h 加速 <= 10s", 70, "加速性能满足日常使用，动力分为 70。"));
            } else if (value <= 12) {
                rules.add(rule("0-100km/h 加速 <= 12s", 60, "加速偏保守，动力分为 60。"));
            } else {
                rules.add(rule("0-100km/h 加速 > 12s", 50, "加速较慢，动力分为 50。"));
            }
        }
        return breakdown(
                "power",
                "动力分 powerScore",
                snapshot.powerScore(),
                "按百公里加速时间分档，时间越短得分越高。",
                rules,
                "动力分展示车辆加速性能对推荐矩阵的贡献。");
    }

    private AlgorithmVisualizationScoreBreakdownVO reputationBreakdown(RecommendItemSnapshot snapshot, CarModel carModel) {
        List<AlgorithmVisualizationMatchedRuleVO> rules = new ArrayList<>();
        BigDecimal rating = carModel == null ? null : carModel.getUserRating();
        if (rating == null) {
            rules.add(rule("用户评分缺失兜底", 60, "没有 user_rating 时口碑分按 60 说明。"));
        } else {
            rules.add(rule("用户评分换算", rating.doubleValue() / 5.0 * 100,
                    "user_rating=" + rating.stripTrailingZeros().toPlainString() + "，按 5 分制换算到 0-100。"));
        }
        return breakdown(
                "reputation",
                "口碑分 reputationScore",
                snapshot.reputationScore(),
                "reputationScore = user_rating / 5 * 100，并截断到 0-100。",
                rules,
                "口碑分反映车型库中的用户评分，不由前端计算。");
    }

    private AlgorithmVisualizationScoreBreakdownVO popularityBreakdown(
            RecommendItemSnapshot snapshot,
            CarModel carModel,
            int maxSalesVolume) {
        List<AlgorithmVisualizationMatchedRuleVO> rules = new ArrayList<>();
        Integer salesVolume = carModel == null ? null : carModel.getSalesVolume();
        if (salesVolume == null || maxSalesVolume <= 0) {
            rules.add(rule("销量缺失兜底", 0, "没有 sales_volume 或最大销量为 0 时热度分按 0 说明。"));
        } else {
            rules.add(rule("销量相对最大值归一化", salesVolume * 100.0 / maxSalesVolume,
                    "sales_volume=" + salesVolume + "，当前最大销量=" + maxSalesVolume + "，按比例换算到 0-100。"));
        }
        return breakdown(
                "popularity",
                "热度分 popularityScore",
                snapshot.popularityScore(),
                "popularityScore = sales_volume / maxSalesVolume * 100，并截断到 0-100。",
                rules,
                "热度分反映当前车型库销量相对水平。");
    }

    private AlgorithmVisualizationMatchedRuleVO energyRuleByFuelConsumption(BigDecimal consumption) {
        if (consumption == null) {
            return rule("燃油油耗缺失兜底", 60, "燃油车缺少 fuelConsumption，能耗分按 60 说明。");
        }
        double value = consumption.doubleValue();
        if (value <= 5) {
            return rule("燃油油耗 <= 5L/100km", 95, "燃油经济性优秀，能耗分为 95。");
        } else if (value <= 6) {
            return rule("燃油油耗 <= 6L/100km", 85, "燃油经济性较好，能耗分为 85。");
        } else if (value <= 7) {
            return rule("燃油油耗 <= 7L/100km", 75, "燃油经济性中等，能耗分为 75。");
        } else if (value <= 8) {
            return rule("燃油油耗 <= 8L/100km", 65, "燃油经济性偏一般，能耗分为 65。");
        } else if (value <= 10) {
            return rule("燃油油耗 <= 10L/100km", 55, "燃油经济性偏弱，能耗分为 55。");
        }
        return rule("燃油油耗 > 10L/100km", 45, "油耗较高，能耗分为 45。");
    }

    private AlgorithmVisualizationMatchedRuleVO energyRuleByElectricRange(Integer range) {
        if (range == null) {
            return rule("纯电续航缺失兜底", 60, "纯电车缺少 electricRangeKm，能耗分按 60 说明。");
        }
        if (range >= 700) {
            return rule("纯电续航 >= 700km", 95, "纯电续航很强，能耗分为 95。");
        } else if (range >= 600) {
            return rule("纯电续航 >= 600km", 90, "纯电续航优秀，能耗分为 90。");
        } else if (range >= 500) {
            return rule("纯电续航 >= 500km", 80, "纯电续航较好，能耗分为 80。");
        } else if (range >= 400) {
            return rule("纯电续航 >= 400km", 70, "纯电续航满足日常，能耗分为 70。");
        } else if (range >= 300) {
            return rule("纯电续航 >= 300km", 60, "纯电续航偏入门，能耗分为 60。");
        }
        return rule("纯电续航 < 300km", 50, "纯电续航较短，能耗分为 50。");
    }

    private AlgorithmVisualizationMatchedRuleVO energyRuleByTotalRange(Integer range) {
        if (range == null) {
            return rule("综合续航缺失兜底", 60, "插混/增程车型缺少 totalRangeKm，能耗分按 60 说明。");
        }
        if (range >= 1000) {
            return rule("综合续航 >= 1000km", 95, "长途能力很强，能耗分为 95。");
        } else if (range >= 800) {
            return rule("综合续航 >= 800km", 85, "长途能力较好，能耗分为 85。");
        } else if (range >= 600) {
            return rule("综合续航 >= 600km", 75, "综合续航满足大部分场景，能耗分为 75。");
        }
        return rule("综合续航 < 600km", 65, "综合续航偏保守，能耗分为 65。");
    }

    private AlgorithmVisualizationScoreBreakdownVO breakdown(
            String dimension,
            String label,
            BigDecimal finalScore,
            String formulaText,
            List<AlgorithmVisualizationMatchedRuleVO> matchedRules,
            String explanation) {
        return new AlgorithmVisualizationScoreBreakdownVO(
                dimension,
                label,
                scoreValue(finalScore),
                formulaText,
                matchedRules,
                explanation);
    }

    private AlgorithmVisualizationMatchedRuleVO rule(String ruleName, double delta, String reason) {
        return new AlgorithmVisualizationMatchedRuleVO(ruleName, scoreValue(BigDecimal.valueOf(delta)), reason);
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

    private List<String> seatOptionLabels(List<String> seatOptions) {
        if (seatOptions == null || seatOptions.isEmpty()) {
            return List.of();
        }
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

    private String formatWan(BigDecimal value) {
        return value.divide(new BigDecimal("10000"), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private String formatNullableWan(BigDecimal value) {
        return value == null ? "未设置" : formatWan(value);
    }

    private String formatDecimal(BigDecimal value) {
        return scoreValue(value).stripTrailingZeros().toPlainString();
    }

    private String topWeightLabel(Map<String, BigDecimal> weights) {
        return weights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> dimensionLabel(entry.getKey()) + "=" + formatDecimal(entry.getValue()))
                .orElse("未记录");
    }

    private String dimensionLabel(String key) {
        return switch (key) {
            case "price" -> "价格";
            case "space" -> "空间";
            case "safety" -> "安全";
            case "energy" -> "能耗";
            case "intelligence" -> "智能";
            case "comfort" -> "舒适";
            case "power" -> "动力";
            case "reputation" -> "口碑";
            case "popularity" -> "热度";
            default -> key;
        };
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scoreValue(BigDecimal value) {
        return value(value).setScale(2, RoundingMode.HALF_UP);
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
