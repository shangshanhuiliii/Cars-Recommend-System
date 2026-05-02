# 推荐算法基线与特征评分说明

本文档保留升级前加权求和算法基线、车型特征评分规则、降级阶段边界和历史对比说明。阶段 9.6 后当前主算法为 `pareto-topsis-v1`，详细公式、流程和伪代码维护在 `RECOMMENDATION_ALGORITHM_UPGRADE.md`。本文中的加权求和仅作为历史算法基线、TOPSIS 边界兜底 `fallbackScore` 和升级前后对比使用。

## 1. 算法定位

本项目采用的是面向汽车购买场景的可解释内容推荐算法。当前主算法是“基于主客观组合权重与 Pareto-TOPSIS 的可解释汽车推荐算法”；升级前基线算法具备以下共同特征：

- 基于内容特征的可解释推荐。
- 多维偏好权重建模。
- 规则化分级补充推荐。
- 非协同过滤。
- 非深度学习模型。
- 非在线学习模型。

算法目标不是简单筛选“满足条件的车”，而是在候选车型中计算“更适合当前用户需求的车”，并保存推荐理由、不足提醒、分数和匹配状态，支持历史追溯。

## 2. 推荐输入

推荐算法依赖以下数据：

| 数据来源 | 作用 |
| --- | --- |
| `car_model` | 车型基础信息、价格、车型类型、动力类型、座位数、审核状态、软删除状态 |
| `car_param` | 车型参数和配置，用于特征评分来源追溯 |
| `car_feature_score` | 车型空间、安全、能耗、智能、舒适、动力、口碑、热度评分 |
| `user_demand` | 用户预算、可接受车型/动力集合、场景、显式偏好、排除条件 |
| 用户画像权重 | 九维推荐权重；升级前用于加权综合分，当前作为 `subjectiveWeight` 来源 |
| 排除品牌 | 硬约束，降级阶段不放宽 |
| 排除车型 | 硬约束，降级阶段不放宽 |
| 默认演示用户 | `userId` 为空时使用 `app_user.id = 1` |

当前 `user_demand` 使用的新字段：

- `body_types`
- `energy_types`
- `scenes`
- `factor_weights`
- `min_seats`
- `budget_min`
- `budget_max`
- `excluded_brands`
- `excluded_car_ids`

旧字段 `body_type`、`energy_type`、`scene`、`focus_factors` 已废弃，不作为当前 API 字段或算法输入。

## 3. 车型特征评分

车型特征评分将车型参数转换为统一的 0-100 分向量，保存在 `car_feature_score`。

| 评分 | 含义 | 主要来源 |
| --- | --- | --- |
| `spaceScore` | 空间表现 | 轴距、车长、座位数、车型类型 |
| `safetyScore` | 安全配置水平 | 气囊、ABS、ESP、主动刹车、车道保持、自适应巡航、并线辅助 |
| `energyScore` | 能耗或续航表现 | 燃油车油耗、纯电续航、插混/增程综合续航 |
| `intelligenceScore` | 智能配置水平 | 语音、OTA、中控屏、倒车影像、360 影像、辅助驾驶、自动泊车 |
| `comfortScore` | 舒适性估算 | 当前由空间、智能、口碑组合估算 |
| `powerScore` | 动力表现 | 百公里加速 |
| `reputationScore` | 口碑表现 | 用户评分换算 |
| `popularityScore` | 热度表现 | 销量按全局最大销量归一化 |

必须明确：

- `priceScore` 不存入 `car_feature_score`。
- `priceScore` 只在推荐阶段根据用户预算动态计算。
- 当前舒适分公式：

```text
comfortScore = spaceScore * 0.5 + intelligenceScore * 0.2 + reputationScore * 0.3
```

- 热度分公式：

```text
popularityScore = salesVolume / maxSales * 100
```

由于 `popularityScore` 依赖全局最大销量，销量变化后建议执行全部车型评分重算。

## 4. 用户画像与权重生成

### 4.1 多场景默认权重

`scenes` 支持多选。选择多个场景时，后端对多个场景模板逐维求平均，再统一归一化。

```text
rawDefaultWeight[dimension] =
  average(sceneTemplate[dimension] for scene in scenes)

defaultWeight = normalize(rawDefaultWeight)
```

如果 `scenes` 为空，则使用“综合需求”默认模板。

### 4.2 显式偏好权重覆盖

`factorWeights` 是用户在前端通过 0-10 滑块表达的显式偏好，不允许前端传最终归一化权重。

规则：

```text
如果 factorWeights 至少一个维度 > 0：
  profileWeights = normalize(factorWeights)
否则：
  profileWeights = scenes 生成的默认权重
```

如果 `factorWeights` 全部为 0 或未传，并且 `scenes` 也为空，则使用“综合需求”默认模板。

### 4.3 九个权重维度

- `price`
- `space`
- `safety`
- `energy`
- `intelligence`
- `comfort`
- `power`
- `reputation`
- `popularity`

最终九个权重总和必须为 1。

### 4.4 画像文本

`profileText` 应反映：

- 预算。
- 可接受车型类型。
- 可接受动力类型。
- 使用场景。
- 高权重关注项。
- 排除品牌和排除车型。

画像文本用于展示和追溯，不替代结构化字段。

## 5. 硬性过滤规则

`STRICT` 阶段必须满足：

- `car_model.audit_status = APPROVED`
- `car_model.deleted = 0`
- 必须存在 `car_feature_score`
- `budgetMax` 是预算硬约束，`guidePrice > budgetMax` 时过滤。
- `budgetMin` 是软偏好，不作为严格过滤条件。
- `bodyTypes` 命中任意一个即可；为空则不按车型类型过滤。
- `energyTypes` 命中任意一个即可；为空则不按动力类型过滤。
- `energyTypes` 包含“新能源”时展开为：
  - `纯电`
  - `插混`
  - `增程`
- `minSeats` 是硬约束。
- `excludedBrands` 是硬约束。
- `excludedCarIds` 是硬约束。
- `scenes` 不作为硬过滤条件，只影响权重。

`car_model.energy_type` 只保存 `燃油 / 纯电 / 插混 / 增程`，不保存“新能源”。

## 6. 动态价格分公式

所有价格单位均为元。`priceScore` 最终限制在 0-100。

### 6.1 预算上下限都存在

```text
budgetMid = (budgetMin + budgetMax) / 2
budgetRange = budgetMax - budgetMin
```

1. `price` 在 `[budgetMin, budgetMax]` 内：

```text
distanceRatio = abs(price - budgetMid) / max(budgetRange / 2, 1)
priceScore = 100 - distanceRatio * 10
priceScore = max(priceScore, 90)
```

2. `price < budgetMin`：

```text
lowerRatio = (budgetMin - price) / budgetMin
priceScore = 90 - lowerRatio * 50
priceScore = max(priceScore, 75)
```

3. `price > budgetMax`：

严格阶段过滤。降级阶段使用：

```text
overRatio = (price - budgetMax) / budgetMax
priceScore = 80 - overRatio * 100
priceScore = max(priceScore, 50)
```

### 6.2 仅填写预算上限

```text
budgetMin = 0
budgetMax = 用户填写值
```

严格阶段仍过滤 `price > budgetMax`。区间内按预算上下限都存在的公式计算。

### 6.3 仅填写预算下限

预算上限为空时不执行超预算过滤。

```text
如果 price < budgetMin：
  lowerRatio = (budgetMin - price) / budgetMin
  priceScore = max(90 - lowerRatio * 50, 75)
否则：
  priceScore = 90
```

### 6.4 未填写预算

```text
priceScore = 75
```

预算语义总结：

- `budgetMax` 是硬约束。
- `budgetMin` 只影响价格匹配分。
- `priceScore` 不写入车型评分表。

## 7. 历史基线综合分与 TOPSIS 兜底

阶段 9.6 前，推荐排序使用线性加权求和得到旧版 `totalScore`。阶段 9.6-D 起，`totalScore` 当前语义已升级为 TOPSIS 推荐分 / 综合推荐分，不再是主流程的简单加权求和分。

以下公式仅用于三类场景：

- 历史算法基线说明。
- TOPSIS 候选数为 1 或加权矩阵无差异时的边界兜底 `fallbackScore`。
- 论文中算法升级前后对比。

```text
weightedSumScore =
  priceScore * weightPrice
  + spaceScore * weightSpace
  + safetyScore * weightSafety
  + energyScore * weightEnergy
  + intelligenceScore * weightIntelligence
  + comfortScore * weightComfort
  + powerScore * weightPower
  + reputationScore * weightReputation
  + popularityScore * weightPopularity
```

说明：

- 所有维度分值为 0-100。
- 所有权重总和为 1。
- `weightedSumScore` 保留合理小数位即可。
- 历史基线排序规则：
  1. `weightedSumScore desc`
  2. `reputationScore desc`
  3. `popularityScore desc`

## 8. 降级推荐策略

推荐阶段：

1. `STRICT`
2. `RELAX_BUDGET`
3. `RELAX_BODY_TYPE`
4. `RELAX_ENERGY_TYPE`
5. `SIMILAR_RECOMMEND`

核心规则：

- 降级推荐是候选集补充，不是覆盖。
- 同一车型只能进入一次推荐集。
- `matchLevel` 保留其首次进入推荐集的阶段。
- `minSeats`、`excludedBrands`、`excludedCarIds` 不参与降级放宽。
- 用户端不展示技术性降级提示，但后端和管理端保留追溯字段。
- 阶段 9.5 后不再使用 `topK` 截断推荐结果。
- `STRICT` 组返回全部完全匹配车型。
- 非 `STRICT` 组返回全部补充推荐车型。
- 历史基线最终展示顺序：
  - `STRICT` 组在前。
  - 推荐组在后。
  - 每组内部按加权分、`reputationScore desc`、`popularityScore desc` 排序。

当前主算法的组内排序先按 TOPSIS `totalScore`，再用 Pareto 非支配标记、口碑分、热度分做同分辅助排序，并由后端写入 `rankNo`；详见 `RECOMMENDATION_ALGORITHM_UPGRADE.md`。

### 8.1 各阶段边界

`STRICT`：

- 使用全部硬性条件。
- `guidePrice > budgetMax` 时过滤。
- `budgetMin` 不过滤，只影响价格分。

`RELAX_BUDGET`：

- 只放宽预算上限。
- 如果存在 `budgetMax`，允许 `guidePrice <= budgetMax * 1.10`。
- 如果没有 `budgetMax`，该阶段不产生额外预算放宽候选。

`RELAX_BODY_TYPE`：

- 只放宽车型类型。
- 车型类型映射：

```text
SUV -> MPV
MPV -> SUV
轿车 -> SUV
```

- 当用户选择多个 `bodyTypes` 时，取映射结果并集，并排除严格阶段已覆盖的车型类型。

`RELAX_ENERGY_TYPE`：

- 只放宽动力类型。
- 动力类型映射：

```text
纯电 -> 插混 / 增程
插混 -> 增程 / 纯电
增程 -> 插混 / 纯电
燃油 -> 插混
新能源 -> 纯电 / 插混 / 增程
```

- 如果用户选择“新能源”，严格阶段已经覆盖 `纯电 / 插混 / 增程`，通常不会产生额外动力放宽候选。

`SIMILAR_RECOMMEND`：

- 保留审核状态、未删除、评分存在、排除品牌、排除车型、最低座位数。
- 不再硬过滤预算、车型类型和动力类型。
- 仍使用价格分和用户权重计算综合分。

## 9. matchLevel、recommendStatus、fallbackMessage

### 9.1 matchLevel

`matchLevel` 是单条推荐明细的匹配状态：

- `STRICT`
- `RELAX_BUDGET`
- `RELAX_BODY_TYPE`
- `RELAX_ENERGY_TYPE`
- `SIMILAR_RECOMMEND`

### 9.2 recommendStatus

`recommendStatus` 是一次推荐记录的总体状态：

- `SUCCESS`：存在推荐项，且只有 `STRICT` 推荐项。
- `FALLBACK`：存在推荐项，且存在任意非 `STRICT` 推荐项。
- `EMPTY`：没有任何推荐项。

### 9.3 fallbackMessage

`fallbackMessage` 是后端保存的追溯字段：

- 后端保存。
- 管理端和历史详情可展示。
- 用户端推荐结果页默认不展示顶部强提示。

生成建议：

```text
SUCCESS:
  "" 或 "已为您找到完全匹配车型"

FALLBACK 且 strictCount > 0:
  "完全匹配车型数量不足，系统已补充部分推荐车型，并在每条结果中标明匹配状态。"

FALLBACK 且 strictCount = 0:
  "未找到完全匹配车型，系统已根据您的核心偏好提供相近推荐。"

EMPTY:
  "暂未找到合适车型，请调整预算、车型类型或动力类型后重试。"
```

## 10. 推荐标签 tags

`tags` 只表示推荐亮点，例如：

- `空间优秀`
- `安全配置高`
- `能耗表现好`
- `智能配置丰富`
- `舒适性较好`
- `动力表现强`
- `口碑较好`
- `热门车型`
- `价格匹配度高`

生成规则：

- `spaceScore >= 85`：空间优秀。
- `safetyScore >= 85`：安全配置高。
- `energyScore >= 85`：能耗表现好。
- `intelligenceScore >= 85`：智能配置丰富。
- `comfortScore >= 85`：舒适性较好。
- `powerScore >= 85`：动力表现强。
- `reputationScore >= 85`：口碑较好。
- `popularityScore >= 85`：热门车型。
- `priceScore >= 90`：价格匹配度高。
- 最多返回 2-3 个主要标签。
- 没有维度达到阈值时返回 1-2 个保底标签，例如“表现均衡”“接近需求”。

`tags` 不允许包含：

- `完全匹配`
- `降级推荐`
- `放宽预算`
- `放宽车型`
- `放宽动力`
- `相似推荐`

`matchLevel` 不能混入 `tags`。

## 11. 推荐理由和不足提醒

### 11.1 推荐理由 reasonText

生成逻辑：

1. 选择用户权重最高的 3-4 个维度。
2. 从这些维度中找出车型得分 >= 80 的维度。
3. 生成 2-3 条自然语言理由。
4. 若高权重维度没有高分，则选择车型最高分维度补充说明。
5. 若仍无法生成具体理由，使用保底文案：

```text
该车型在多个维度上与您的需求较为接近，可作为备选车型进一步对比。
```

### 11.2 不足提醒 weaknessText

生成逻辑：

1. 选择用户权重较高的维度。
2. 找出车型得分 < 65 的维度。
3. 生成 1-2 条不足提醒。
4. 如果没有明显短板，使用保底文案：

```text
该车型整体匹配较均衡，暂无明显短板。
```

每条 `recommend_item` 都要保存 `reasonText` 和 `weaknessText` 快照。

## 12. 推荐记录快照

### 12.1 recommend_record

一次推荐记录需要保存：

- `userId`
- `demandId`
- `profileText`
- `recommendStatus`
- `fallbackMessage`
- 权重快照
- 需求快照或可回查的需求 ID

### 12.2 recommend_item

每条推荐明细需要保存：

- `carId`
- `rankNo`
- `totalScore`
- `priceScore`
- 各维度评分快照
- `matchLevel`
- `tags`
- `reasonText`
- `weaknessText`

历史记录读取快照，不重新计算历史推荐的 `tags`、分数、理由、不足和 `matchLevel`。

## 13. 历史基线伪代码

以下伪代码描述升级前加权求和流程，用于理解历史基线和 TOPSIS 兜底来源。当前主流程的 Pareto-TOPSIS 伪代码见 `RECOMMENDATION_ALGORITHM_UPGRADE.md`。

```text
function generateRecommendation(request):
    userId = request.userId or DEFAULT_DEMO_USER_ID
    demand = loadUserDemand(request.demandId, userId)
    weights = readWeightsFromDemand(demand)
    weightSnapshot = serializeWeights(weights)

    allCars = loadCarsWhere(
        deleted = false,
        auditStatus = APPROVED,
        featureScore exists
    )

    excludedBrands = parseJsonArray(demand.excludedBrands)
    excludedCarIds = parseJsonArray(demand.excludedCarIds)
    strictBodyTypes = parseJsonArray(demand.bodyTypes)
    strictEnergyTypes = expandEnergyTypes(parseJsonArray(demand.energyTypes))

    selectedByCarId = ordered map

    strictCandidates = []
    for car in allCars:
        if car.brand in excludedBrands:
            continue
        if car.id in excludedCarIds:
            continue
        if demand.minSeats exists and car.seats < demand.minSeats:
            continue
        if demand.budgetMax exists and car.guidePrice > demand.budgetMax:
            continue
        if strictBodyTypes not empty and car.bodyType not in strictBodyTypes:
            continue
        if strictEnergyTypes not empty and car.energyType not in strictEnergyTypes:
            continue
        strictCandidates.add(car)

    for car in strictCandidates:
        item = buildScoredItem(car, demand, weights, STRICT)
        selectedByCarId.putIfAbsent(car.id, item)

    for stage in [RELAX_BUDGET, RELAX_BODY_TYPE, RELAX_ENERGY_TYPE, SIMILAR_RECOMMEND]:
        stageCandidates = []

        for car in allCars:
            if selectedByCarId contains car.id:
                continue
            if car.brand in excludedBrands:
                continue
            if car.id in excludedCarIds:
                continue
            if demand.minSeats exists and car.seats < demand.minSeats:
                continue

            if stage == RELAX_BUDGET:
                if demand.budgetMax is empty:
                    continue
                if car.guidePrice > demand.budgetMax * 1.10:
                    continue
                if strictBodyTypes not empty and car.bodyType not in strictBodyTypes:
                    continue
                if strictEnergyTypes not empty and car.energyType not in strictEnergyTypes:
                    continue

            if stage == RELAX_BODY_TYPE:
                relaxedBodyTypes = buildRelaxedBodyTypes(strictBodyTypes)
                if relaxedBodyTypes empty or car.bodyType not in relaxedBodyTypes:
                    continue
                if demand.budgetMax exists and car.guidePrice > demand.budgetMax:
                    continue
                if strictEnergyTypes not empty and car.energyType not in strictEnergyTypes:
                    continue

            if stage == RELAX_ENERGY_TYPE:
                relaxedEnergyTypes = buildRelaxedEnergyTypes(demand.energyTypes, strictEnergyTypes)
                if relaxedEnergyTypes empty or car.energyType not in relaxedEnergyTypes:
                    continue
                if demand.budgetMax exists and car.guidePrice > demand.budgetMax:
                    continue
                if strictBodyTypes not empty and car.bodyType not in strictBodyTypes:
                    continue

            if stage == SIMILAR_RECOMMEND:
                # budgetMax、bodyTypes、energyTypes 不再作为硬过滤
                pass

            stageCandidates.add(car)

        for car in stageCandidates:
            item = buildScoredItem(car, demand, weights, stage)
            selectedByCarId.putIfAbsent(car.id, item)

    allItems = selectedByCarId.values()
    strictItems = allItems where matchLevel == STRICT
    recommendItems = allItems where matchLevel != STRICT

    sort strictItems by totalScore desc, reputationScore desc, popularityScore desc
    sort recommendItems by totalScore desc, reputationScore desc, popularityScore desc

    finalItems = strictItems + recommendItems

    rankNo = 1
    for item in finalItems:
        item.rankNo = rankNo
        rankNo = rankNo + 1

    if finalItems empty:
        recommendStatus = EMPTY
    else if recommendItems empty:
        recommendStatus = SUCCESS
    else:
        recommendStatus = FALLBACK

    fallbackMessage = buildFallbackMessage(recommendStatus, strictItems.size)

    record = new RecommendRecord()
    record.userId = userId
    record.demandId = demand.id
    record.profileText = demand.profileText
    record.weightSnapshot = weightSnapshot
    record.fallbackMessage = fallbackMessage
    record.recommendStatus = recommendStatus
    save(record)

    for item in finalItems:
        recommendItem = new RecommendItem()
        recommendItem.recordId = record.id
        recommendItem.carId = item.carId
        recommendItem.rankNo = item.rankNo
        recommendItem.totalScore = item.totalScore
        recommendItem.priceScore = item.priceScore
        recommendItem.spaceScore = item.spaceScore
        recommendItem.safetyScore = item.safetyScore
        recommendItem.energyScore = item.energyScore
        recommendItem.intelligenceScore = item.intelligenceScore
        recommendItem.comfortScore = item.comfortScore
        recommendItem.powerScore = item.powerScore
        recommendItem.reputationScore = item.reputationScore
        recommendItem.popularityScore = item.popularityScore
        recommendItem.matchLevel = item.matchLevel
        recommendItem.tags = toJson(item.tags)
        recommendItem.reasonText = item.reasonText
        recommendItem.weaknessText = item.weaknessText
        save(recommendItem)

    return buildResponse(record, finalItems)


function buildScoredItem(car, demand, weights, matchLevel):
    priceScore = calculatePriceScore(car.guidePrice, demand.budgetMin, demand.budgetMax, matchLevel)
    totalScore =
        priceScore * weights.price
        + car.score.spaceScore * weights.space
        + car.score.safetyScore * weights.safety
        + car.score.energyScore * weights.energy
        + car.score.intelligenceScore * weights.intelligence
        + car.score.comfortScore * weights.comfort
        + car.score.powerScore * weights.power
        + car.score.reputationScore * weights.reputation
        + car.score.popularityScore * weights.popularity

    tags = buildTags(priceScore, car.score)
    reasonText = buildReasonText(weights, car.score, demand)
    weaknessText = buildWeaknessText(weights, car.score)

    return scored item


function expandEnergyTypes(energyTypes):
    expanded = empty set
    for energyType in energyTypes:
        if energyType == "新能源":
            expanded.add("纯电")
            expanded.add("插混")
            expanded.add("增程")
        else:
            expanded.add(energyType)
    return expanded
```

## 14. 示例场景

### 14.1 家庭出行

用户需求：

- 预算 10-15 万。
- 可接受 `SUV / MPV`。
- 可接受 `插混 / 增程`。
- 场景为家庭出行和长途自驾。
- 空间、安全权重高。

预期行为：

- `bodyTypes` 命中 SUV 或 MPV 即可。
- `energyTypes` 命中插混或增程即可。
- `budgetMax` 过滤超过 15 万的 STRICT 候选。
- 推荐结果优先展示空间、安全、舒适表现更好的车型。
- 若完全匹配车型不足，补充推荐进入“推荐”组。

### 14.2 城市通勤

用户需求：

- 预算 8-12 万。
- 可接受 `轿车 / SUV`。
- 可接受 `燃油 / 纯电`。
- 价格、能耗、智能权重高。

预期行为：

- 预算区间内且能耗分高的车型获得更高 `priceScore` 和 `energyScore` 贡献。
- 纯电续航较好或燃油油耗较低的车型排序靠前。
- 推荐理由重点解释价格匹配、能耗表现和智能配置。

### 14.3 极端条件

用户需求：

- 预算较低。
- 需要 7 座纯电 SUV。
- 空间和安全权重高。

预期行为：

- STRICT 阶段可能结果为空或数量较少。
- `minSeats` 始终不放宽。
- 排除品牌和排除车型始终不放宽。
- 系统补充 `RELAX_BUDGET`、`RELAX_BODY_TYPE`、`RELAX_ENERGY_TYPE` 或 `SIMILAR_RECOMMEND` 候选。
- 用户端展示“完全匹配车型”和“推荐”两组，管理端可追溯每条结果的 `matchLevel`。
