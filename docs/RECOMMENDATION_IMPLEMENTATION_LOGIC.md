# 推荐算法实现逻辑说明

本文档描述当前代码中推荐算法的实际实现逻辑，用于代码审计、论文实现章节和后续维护。它不是新的设计方案；详细算法设计仍以 `RECOMMENDATION_ALGORITHM_UPGRADE.md` 为准，升级前加权求和基线和车型特征评分规则见 `RECOMMENDATION_ALGORITHM.md`。

## 1. 文档目的

本文档回答“当前代码已经如何实现推荐算法”。重点说明后端从用户需求、候选生成、主客观组合权重、Pareto、TOPSIS、解释生成到快照保存的真实调用链，以及前端如何按后端快照展示结果。

约束：

- 不描述未实现功能为已完成能力。
- 不把当前算法包装成深度学习、协同过滤或在线学习。
- 不恢复 `topK`、默认 Top 10 或 `min(5, topK)`。
- 不把旧字段 `bodyType`、`energyType`、`scene`、`focusFactors` 作为当前用户需求 API 字段。

## 2. 算法版本

当前推荐算法名称：

```text
基于主客观组合权重与 Pareto-TOPSIS 的可解释汽车推荐算法
```

当前算法版本：

```text
pareto-topsis-v1
```

版本常量定义在 `RecommendationWeightService.ALGORITHM_VERSION`。推荐生成时该版本写入 `recommend_record.weight_snapshot.algorithmVersion`，并由 `RecommendationResponseVO.algorithmVersion` 和历史详情响应返回。

## 3. 推荐主流程

### Step 1：读取用户需求

- 做什么：根据 `RecommendationGenerateRequest.demandId` 读取 `user_demand`，并校验用户归属。
- 输入：`POST /api/recommend/generate` 请求中的 `demandId` 和可选 `userId`。
- 输出：`UserDemand` 实体。
- 对应代码：`RecommendationController.generate` -> `RecommendationServiceImpl.generate` -> `UserDemandMapper.findById`。
- 关键规则：`userId` 为空时使用默认演示用户 `1`；`UserDemandMapper.existsActiveUser` 校验用户存在；需求的 `userId` 必须等于当前用户。

### Step 2：解析硬约束

- 做什么：从 `UserDemand` 的 JSON 字段解析预算、车型集合、动力集合、最低座位数、排除品牌和排除车型。
- 输入：`UserDemand.bodyTypes`、`energyTypes`、`budgetMin`、`budgetMax`、`minSeats`、`excludedBrands`、`excludedCarIds`。
- 输出：各匹配阶段可使用的过滤条件。
- 对应代码：`RecommendationCandidateService.matchesCommonFilters`、`matchesStrictFilters`、`expandedDemandEnergyTypes`、`relaxedBodyTypes`、`relaxedEnergyTypes`。
- 关键规则：`budgetMax` 是严格预算上限；`budgetMin` 只影响价格分；`bodyTypes` 和 `energyTypes` 多选命中任意一个；`新能源` 展开为 `纯电 / 插混 / 增程`；`minSeats`、`excludedBrands`、`excludedCarIds` 是硬约束，降级阶段不放宽。

### Step 3：生成用户主观权重

- 做什么：保存需求时生成九维画像权重，推荐时读取这些权重作为 `subjectiveWeight`。
- 输入：`UserDemandSaveRequest.factorWeights`、`scenes`；推荐时读取 `UserDemand.weight_*` 字段。
- 输出：九维 `subjectiveWeight`。
- 对应代码：`UserProfileServiceImpl.buildWeights`、`applyWeights`；`RecommendationWeightService.subjectiveWeight`。
- 关键规则：`factorWeights` 至少一个值大于 0 时使用显式滑块并归一化；否则使用 `scenes` 模板平均值，`scenes` 为空时使用 `综合需求`；权重总和归一化为 1。

### Step 4：加载候选车型和特征评分

- 做什么：加载可进入推荐的车型，并补齐对应的车型特征评分。
- 输入：`car_model`、`car_feature_score`。
- 输出：`CandidateCar` 列表。
- 对应代码：`RecommendationCandidateService.loadCandidatesWithScores`、`CarModelMapper.findApprovedRecommendationCandidates`、`CarFeatureScoreMapper.findByCarId`。
- 关键规则：只加载 `audit_status = APPROVED` 且 `deleted = FALSE` 的车型；没有 `car_feature_score` 的车型不进入候选；`car_model.energy_type` 只保存 `燃油 / 纯电 / 插混 / 增程`。

### Step 5：按 STRICT / 降级阶段生成候选集

- 做什么：按阶段生成候选，并按 `carId` 去重。
- 输入：候选车型、用户硬约束。
- 输出：`RecommendationCandidateGroups.strictCandidates` 和 `recommendationCandidates`。
- 对应代码：`RecommendationCandidateService.generateCandidates`、`addStageRecommendations`、`matchesDemand`。
- 关键规则：阶段顺序为 `STRICT -> RELAX_BUDGET -> RELAX_BODY_TYPE -> RELAX_ENERGY_TYPE -> SIMILAR_RECOMMEND`；同一车型只进入一次；`matchLevel` 保留首次进入阶段；不使用 `topK` 或默认数量截断。

### Step 6：为候选车计算 priceScore

- 做什么：根据用户预算动态计算价格匹配分。
- 输入：车型 `guidePrice` 和 `UserDemand.budgetMin / budgetMax`。
- 输出：0-100 的 `priceScore`。
- 对应代码：`RecommendationServiceImpl.toScoredRecommendation`、`PriceScoreCalculator.calculate`。
- 关键规则：`priceScore` 只在推荐阶段计算，不写入 `car_feature_score`；预算缺失时默认 75；仅有预算下限时低于下限按软惩罚，满足下限返回 90；超过预算上限的候选在降级阶段仍会得到超预算惩罚分。

### Step 7：构造 9 维决策矩阵

- 做什么：将候选车转换为九维评分向量，作为熵权法、Pareto 和 TOPSIS 输入。
- 输入：`priceScore` 和 `CarFeatureScore` 的八个静态维度。
- 输出：`List<RecommendationScoreVector>`。
- 对应代码：`RecommendationServiceImpl.scoreVectors`、`RecommendationScoreVector`、`RecommendationDimension`。
- 关键规则：九维为 `price / space / safety / energy / intelligence / comfort / power / reputation / popularity`；所有指标为 0-100 且越高越好；价格已经通过 `priceScore` 转换为正向指标。

### Step 8：计算熵权 objectiveWeight

- 做什么：基于当前候选集九维矩阵计算客观权重。
- 输入：全部候选的 `RecommendationScoreVector` 和 `subjectiveWeight` 兜底值。
- 输出：九维 `objectiveWeight`。
- 对应代码：`RecommendationWeightService.objectiveWeight`。
- 关键规则：实现熵权法，使用 `epsilon = 0.0001` 避免 0 值对数问题；候选数 `n <= 1` 时退化为 `subjectiveWeight`；所有维度差异系数和接近 0 时退化为 `subjectiveWeight`；输出再归一化，总和为 1。

### Step 9：合成 finalWeight

- 做什么：按 `alpha` 将主观权重和客观权重合成为最终排序权重。
- 输入：`subjectiveWeight`、`objectiveWeight`、`factorWeights` 是否显式设置。
- 输出：`RecommendationWeightSnapshot`。
- 对应代码：`RecommendationWeightService.calculate`、`finalWeight`、`RecommendationWeightSnapshot`。
- 关键规则：显式 `factorWeights` 时 `alpha = 0.75`；仅场景模板时 `alpha = 0.60`；`finalWeight = alpha * subjectiveWeight + (1 - alpha) * objectiveWeight`；最终归一化为 1；快照保存 `algorithmVersion / alpha / subjectiveWeight / objectiveWeight / finalWeight`。

### Step 10：识别 Pareto 非支配车型

- 做什么：在每个展示组内部计算 Pareto 被支配标记。
- 输入：组内候选的九维评分向量和 `finalWeight`。
- 输出：每个候选的 `paretoDominated` 内部标记。
- 对应代码：`ParetoAnalyzer.analyze`、`keyDimensions`、`dominates`；`RecommendationServiceImpl.markParetoDominated`。
- 关键规则：取 `finalWeight` 最高的前 4 个维度作为集合 `K`；如果 A 在所有 `K` 维度上不低于 B，且至少一维高于 B，则 A 支配 B；第一版只标记、不删除候选、不写入数据库字段、不写入 `tags`。

### Step 11：使用 TOPSIS 计算 recommendScore

- 做什么：对全部候选统一执行 TOPSIS，得到最终 `totalScore`。
- 输入：全部候选的九维矩阵、`finalWeight` 和旧加权效用 `fallbackScore`。
- 输出：每个候选的 TOPSIS 推荐分和解释所需中间结果。
- 对应代码：`RecommendationServiceImpl.applyTopsisScore`、`TopsisRanker.analyze`。
- 关键规则：TOPSIS 在 `STRICT + 推荐组` 全部候选上统一计算；使用向量归一化、加权归一化、正理想解、负理想解、距离和相对接近度；`totalScore = C_i * 100`，保留 2 位小数并限制在 0-100；候选数为 1 或 `D+ + D-` 无差异时使用旧加权效用 `fallbackScore`。

### Step 12：按 matchLevel 分组并排序

- 做什么：先分组，再按后端规则排序并生成全局 `rankNo`。
- 输入：已计算 `totalScore`、`paretoDominated` 和维度分的推荐项。
- 输出：最终有序 `ScoredRecommendation` 列表。
- 对应代码：`RecommendationServiceImpl.sortRecommendationItems`、`recommendationComparator`；前端 `rankOrderedItems`。
- 关键规则：`STRICT` 组永远在推荐组前；每组内部先按 `paretoDominated = false`，再按 `totalScore desc`、`reputationScore desc`、`popularityScore desc`；后端写入 `rankNo`；前端每组内部按 `rankNo` 升序展示，不按分数二次排序。

### Step 13：生成 tags、reasonText、weaknessText

- 做什么：根据 TOPSIS 中间结果和最终权重生成用户可读解释。
- 输入：`RecommendationScoreVector`、`TopsisItemResult`、`finalWeight`。
- 输出：`tags`、`reasonText`、`weaknessText`。
- 对应代码：`RecommendationExplanationService.generate`、`generateTags`、`generateReasonText`、`generateWeaknessText`。
- 关键规则：`reasonText` 选择贡献度较高且原始维度表现不差的 2-3 个维度；贡献度直接使用 TOPSIS 加权归一化表现 `weightedNormalizedScore_j`；`weaknessText` 在高权重维度中选择与正理想解差距较大的 1-2 个维度；代码中的差距为 `positiveIdeal_j - weightedNormalizedScore_j`，其中两者已经包含 `finalWeight`；无明显短板时返回“该车型整体匹配较均衡，暂无明显短板。”；`tags` 只表示亮点，不包含 `matchLevel`、Pareto、TOPSIS 或降级技术状态。

### Step 14：保存 recommend_record 和 recommend_item 快照

- 做什么：保存推荐记录和推荐明细，保证历史可追溯。
- 输入：最终排序结果、画像文本、状态、权重快照、解释文本。
- 输出：`recommend_record` 和 `recommend_item` 数据。
- 对应代码：`RecommendationServiceImpl.generate`、`toRecommendItem`；`RecommendRecordMapper.insert`、`RecommendItemMapper.insert`。
- 关键规则：`recommend_record` 保存 `profileText`、`weightSnapshot`、`fallbackMessage`、`recommendStatus`；`recommend_item` 保存 `rankNo`、`totalScore`、`priceScore`、八个静态维度分、`tags`、`matchLevel`、`reasonText`、`weaknessText`；不新增数据库字段；`paretoDominated` 和 TOPSIS 距离第一版不入库。

### Step 15：返回推荐结果

- 做什么：构造接口响应，并由前端按快照展示。
- 输入：保存后的 `recordId`、最终推荐项和算法快照。
- 输出：`RecommendationResponseVO` 或 `RecommendationHistoryDetailVO`。
- 对应代码：`RecommendationServiceImpl.toItemVO`、`RecommendationResponseVO`、`RecommendationItemVO`；历史详情 `RecommendationRecordServiceImpl.detail`。
- 关键规则：响应包含 `algorithmVersion`、`alpha`、`fallbackMessage`、`recommendStatus`；条目包含 `rankNo`、`totalScore`、`tags`、`reasonText`、`weaknessText`、`matchLevel`；历史详情读取 `RecommendItemMapper.findSnapshotsByRecordId`，按 `rank_no ASC` 返回，不重新计算。

## 4. 主客观组合权重实现

### subjectiveWeight 来源

保存需求时，`UserProfileServiceImpl` 将用户输入转换为九维画像权重：

- `factorWeights` 至少一个维度大于 0：按滑块值归一化。
- `factorWeights` 全部为 0 或未传：按 `scenes` 场景模板生成。
- `scenes` 为空：使用 `综合需求` 模板。

推荐生成时，`RecommendationWeightService.subjectiveWeight` 从 `UserDemand.weightPrice`、`weightSpace`、`weightSafety`、`weightEnergy`、`weightIntelligence`、`weightComfort`、`weightPower`、`weightReputation`、`weightPopularity` 读取并归一化。

### objectiveWeight 熵权法

`RecommendationWeightService.objectiveWeight` 对候选集九维矩阵计算客观权重：

1. 将每个指标分数转为正值，最低为 `epsilon = 0.0001`。
2. 计算同一指标下各候选的占比。
3. 计算信息熵和差异系数。
4. 按差异系数归一化得到 `objectiveWeight`。

边界处理：

- 候选数 `<= 1`：返回 `subjectiveWeight`。
- 所有维度差异系数和接近 0：返回 `subjectiveWeight`。
- 任一权重归一化时总和异常：退化为九维均匀权重。

### alpha 规则

`RecommendationWeightService.hasExplicitFactorWeight` 解析 `UserDemand.factorWeights`：

- 任一九维滑块值 `> 0`：`alpha = 0.75`。
- 否则：`alpha = 0.60`。

### finalWeight 公式

实现公式：

```text
finalWeight_j = alpha * subjectiveWeight_j + (1 - alpha) * objectiveWeight_j
```

计算后再次归一化，保证九维总和为 1。

### weight_snapshot 结构

当前 `recommend_record.weight_snapshot` 保存 JSON：

```json
{
  "algorithmVersion": "pareto-topsis-v1",
  "alpha": 0.75,
  "subjectiveWeight": {
    "price": 0.000000,
    "space": 0.363636,
    "safety": 0.363636,
    "energy": 0.000000,
    "intelligence": 0.000000,
    "comfort": 0.272728,
    "power": 0.000000,
    "reputation": 0.000000,
    "popularity": 0.000000
  },
  "objectiveWeight": {
    "price": 0.102100,
    "space": 0.160200,
    "safety": 0.088500,
    "energy": 0.145000,
    "intelligence": 0.120000,
    "comfort": 0.095000,
    "power": 0.110000,
    "reputation": 0.092000,
    "popularity": 0.087200
  },
  "finalWeight": {
    "price": 0.025525,
    "space": 0.312777,
    "safety": 0.294852,
    "energy": 0.036250,
    "intelligence": 0.030000,
    "comfort": 0.228296,
    "power": 0.027500,
    "reputation": 0.023000,
    "popularity": 0.021800
  }
}
```

历史详情读取时，`RecommendationRecordServiceImpl.readWeights` 优先读取 `finalWeight`；旧记录如果只有扁平九维权重，则直接按旧结构读取，并将 `algorithmVersion` 兜底为 `weighted-sum-v1`。

## 5. Pareto 实现

`ParetoAnalyzer` 只做第一版布尔标记，不删除候选。

K 的选择：

- `ParetoAnalyzer.keyDimensions` 按 `finalWeight` 从高到低排序。
- 取前 4 个维度作为高权重维度集合 `K`。

支配关系：

```text
如果 A 在所有 j in K 上 score_Aj >= score_Bj
并且至少一个 j in K 满足 score_Aj > score_Bj
则 A 支配 B，B 标记为 paretoDominated = true
```

排序用途：

- `RecommendationServiceImpl.markParetoDominated` 分别对 `STRICT` 组和推荐组计算标记。
- `RecommendationServiceImpl.recommendationComparator` 让 `paretoDominated = false` 的候选排在同组前面。
- 标记不写入 `recommend_item`，不返回用户端，不写入 `tags`。

不删除候选的原因：

- 降级推荐需要保留可解释备选。
- Pareto 删除可能导致结果过少。
- 当前方案 A 不新增数据库字段，用户可见排序由 `rankNo` 快照追溯。

## 6. TOPSIS 实现

`TopsisRanker.analyze` 对全部候选统一计算 TOPSIS，不按 `STRICT` 和推荐组分别计算分数。

### 九维矩阵

输入为 `List<RecommendationScoreVector>`：

```text
price, space, safety, energy, intelligence, comfort, power, reputation, popularity
```

所有指标为正向指标。`price` 使用动态 `priceScore`，不再进行成本型转换。

### 归一化

`TopsisRanker.normalizedMatrix` 对每一列执行向量归一化：

```text
r_ij = x_ij / sqrt(sum_i(x_ij^2))
```

如果某列平方和接近 0，则该列归一化结果置为 0。

### 加权归一化

`TopsisRanker.weightedMatrix` 使用 `finalWeight`：

```text
v_ij = r_ij * finalWeight_j
```

### 正理想解和负理想解

`positiveIdeal` 取每列最大值，`negativeIdeal` 取每列最小值：

```text
A+_j = max_i(v_ij)
A-_j = min_i(v_ij)
```

### D+ / D-

`distance` 计算候选到正理想解和负理想解的欧氏距离：

```text
D_i+ = sqrt(sum_j((v_ij - A+_j)^2))
D_i- = sqrt(sum_j((v_ij - A-_j)^2))
```

### C_i 和 totalScore

当距离和可区分时：

```text
C_i = D_i- / (D_i+ + D_i-)
totalScore = C_i * 100
```

`totalScore` 保留 2 位小数，限制在 0-100。阶段 9.6-D 起，`recommend_item.total_score` 和 API `items[].totalScore` 的当前语义为 TOPSIS 推荐分 / 综合匹配度。

### fallbackScore

`RecommendationServiceImpl.calculateFallbackScore` 仍计算旧加权效用分，但仅用于 TOPSIS 边界兜底：

- 候选数为 1 时，正负理想解相同，距离和为 0，使用 `fallbackScore`。
- 候选在加权矩阵上无差异时，使用 `fallbackScore`。
- `fallbackScore` 不作为正常主排序分。

## 7. 分组与排序

后端排序由 `RecommendationServiceImpl.sortRecommendationItems` 统一完成：

1. 将候选分为 `STRICT` 组和非 `STRICT` 推荐组。
2. 每组内部计算 Pareto 被支配标记。
3. 每组内部排序：
   - `paretoDominated = false` 优先。
   - `totalScore desc`。
   - `reputationScore desc`。
   - `popularityScore desc`。
4. 拼接顺序固定为 `STRICT` 组在前，推荐组在后。
5. 按最终展示顺序写入全局连续 `rankNo`。

前端展示：

- `RecommendResultView.vue` 按 `matchLevel === 'STRICT'` 分成“完全匹配车型”和“推荐”。
- 每组内部调用 `rankOrderedItems`，仅按 `rankNo` 升序展示。
- 前端不再按 `totalScore`、口碑分或热度分二次排序。
- 管理端推荐记录明细使用后端历史详情返回顺序，后端 SQL 已按 `rank_no ASC` 返回。

## 8. 推荐解释实现

### tags

`RecommendationExplanationService.generateTags` 按维度高分生成亮点标签：

- `priceScore >= 90`：价格匹配度高。
- 其他维度分 `>= 85`：生成对应亮点标签。
- `totalScore >= 80`：可加入“接近理想车型”。
- 至少 5 个维度分 `>= 75`：可加入“多维表现均衡”。
- 最多保留 3 个标签。

`tags` 不包含 `完全匹配`、`降级推荐`、`放宽预算`、`放宽车型`、`放宽动力`、`相似推荐`、`STRICT`、`RELAX_*`、`SIMILAR_RECOMMEND`、`TOPSIS`、`Pareto` 或 `熵权`。

### contribution reasonText

`reasonText` 来源于贡献度高且原始维度分不低的维度：

- 贡献度使用 `TopsisItemResult.weightedNormalizedScore[key]`。
- 原始维度分需 `>= 70`。
- 按贡献度降序取最多 3 个维度。
- 少于 2 条具体理由时使用保底文案。

### gap weaknessText

`weaknessText` 来源于高权重维度中的正理想解差距：

- 先按 `finalWeight` 取前 4 个高权重维度。
- 差距为 `positiveIdeal[key] - weightedNormalizedScore[key]`。
- 原始维度分 `< 80` 且差距大于阈值时生成不足提醒。
- 最多取 2 条。
- 没有明显短板时返回：

```text
该车型整体匹配较均衡，暂无明显短板。
```

### 快照保存

推荐生成时，`RecommendationServiceImpl.toRecommendItem` 将 `tags`、`reasonText` 和 `weaknessText` 写入 `recommend_item`。历史详情只读取这些快照，不重新生成解释。

## 9. 推荐记录追溯

### recommend_record 保存内容

`RecommendRecord` 当前保存：

- `userId`
- `demandId`
- `profileText`
- `weightSnapshot`
- `fallbackMessage`
- `recommendStatus`

其中 `weightSnapshot` 新版保存 `algorithmVersion`、`alpha`、`subjectiveWeight`、`objectiveWeight`、`finalWeight`。

### recommend_item 保存内容

`RecommendItem` 当前保存：

- `recordId`
- `carId`
- `rankNo`
- `totalScore`
- `priceScore`
- `spaceScore`
- `safetyScore`
- `energyScore`
- `intelligenceScore`
- `comfortScore`
- `powerScore`
- `reputationScore`
- `popularityScore`
- `tags`
- `matchLevel`
- `reasonText`
- `weaknessText`

### 历史详情不重新计算

`RecommendationRecordServiceImpl.detail` 通过 `RecommendRecordMapper.findByIdAndUserId` 读取推荐记录，再通过 `RecommendItemMapper.findSnapshotsByRecordId` 按 `rank_no ASC` 读取推荐明细快照。返回过程中不调用候选生成、权重计算、Pareto、TOPSIS 或解释生成组件。

### 旧 weight_snapshot 兼容

旧记录如果没有 `algorithmVersion`，`RecommendationRecordServiceImpl.readAlgorithmVersion` 返回 `weighted-sum-v1`。旧扁平九维权重没有 `finalWeight` 节点时，`readWeights` 直接读取顶层九维字段。

## 10. 与普通筛选的区别

普通筛选只回答“哪些车型满足条件”。当前实现先按硬性约束生成候选，再把车型转成九维评分矩阵，结合用户主观权重和候选集客观差异得到 `finalWeight`，通过 Pareto 非支配优先和 TOPSIS 相对接近度计算综合匹配度，并保存推荐理由、不足提醒、标签、匹配状态和权重快照。

因此系统回答的是：

- 哪些车型符合或接近需求。
- 哪些车型在多指标决策下更适合。
- 为什么推荐。
- 哪里可能不足。
- 如果没有完全匹配，系统如何补充推荐。
- 历史推荐能否按当时快照复现。

## 11. 已知边界与不足

- TOPSIS 是相对候选集排序，候选集变化会影响 `totalScore`，不能解释为绝对市场评分。
- 熵权法在候选很少或候选无差异时不稳定，当前代码已退化为 `subjectiveWeight`。
- 车型特征评分仍由规则引擎生成，舒适性等维度存在组合估算成分。
- Pareto 第一版只做内部标记和排序，不持久化 `paretoDominated`，历史主要通过 `rankNo`、`totalScore`、维度分和权重快照追溯。
- 当前没有使用用户行为在线学习。
- 深度学习、协同过滤、在线学习只属于后续展望，不是当前系统实现。
