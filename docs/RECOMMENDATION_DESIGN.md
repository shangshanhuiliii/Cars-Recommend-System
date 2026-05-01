# 推荐算法设计

本文档只描述推荐算法和推荐闭环，不重复数据库 DDL、接口清单或阶段任务。数据库细节见 `DATABASE_DESIGN.md`，接口细节见 `API_DESIGN.md`，实施步骤见 `IMPLEMENTATION_TASKS.md`。

## 1. 推荐目标

系统推荐能力必须围绕以下闭环实现：

```text
车型特征评分 -> 用户画像建模 -> 多维匹配计算 -> 推荐解释生成 -> 无匹配降级处理 -> 推荐记录追溯
```

推荐结果必须满足：

- 来自真实车型数据和评分规则。
- 能说明推荐理由和不足提醒。
- 能标识严格匹配或降级匹配状态。
- 能保存推荐依据，支持历史回查。

## 2. 推荐算法定义

本项目采用的推荐算法是：

> 基于内容特征的规则评分与多维加权匹配推荐算法。

它不是协同过滤算法，也不是深度学习推荐模型。原因是本系统面向本科毕设和项目初期实现，缺少真实用户长期点击、收藏、购买和反馈行为数据。直接使用协同过滤或深度学习模型会导致数据来源不足、结果难解释、答辩时难说明。因此，本项目采用更适合当前场景的可解释规则推荐算法。

算法核心思想是：

```text
先把车型参数转化为车型特征向量，
再把用户需求转化为用户偏好权重向量，
然后计算车型特征向量与用户偏好权重之间的加权匹配分，
最后根据匹配分排序，并生成推荐理由和不足提醒。
```

### 2.1 算法输入

算法输入分为两类。

第一类是车型侧数据：

- 车型基础信息：品牌、车系、车型名称、价格、车型类型、动力类型、座位数等。
- 车型参数信息：轴距、车长、油耗、续航、安全配置、智能配置、百公里加速等。
- 车型特征评分：空间、安全、能耗、智能、舒适、动力、口碑、热度等 0-100 分维度。

第二类是用户侧数据：

- 硬性条件：预算、可接受车型类型集合、可接受动力类型集合、座位数、排除品牌等。
- 偏好条件：多个使用场景和 0-10 显式偏好权重滑块。
- 用户画像权重：价格、空间、安全、能耗、智能、舒适、动力、口碑、热度等权重。

### 2.2 算法输出

算法输出不是简单车型列表，而是一组可解释推荐结果：

- 推荐车型
- 排名
- 综合匹配分
- 价格分
- 各维度评分快照
- 匹配状态
- 推荐理由
- 不足提醒
- 降级提示，可选

### 2.3 算法流程

完整算法流程如下：

```text
1. 读取用户购车需求。
2. 根据多个使用场景和显式 `factorWeights` 生成用户偏好权重。
3. 读取车型基础信息、车型参数和车型特征评分。
4. 按预算上限、车型类型、动力类型、座位数等条件执行严格过滤。
5. 对候选车型动态计算价格匹配分。
6. 使用多维加权公式计算综合匹配分。
7. 按综合匹配分排序，得到 Top-K 推荐结果。
8. 根据高权重维度和车型高分维度生成推荐理由。
9. 根据高权重维度和车型低分维度生成不足提醒。
10. 如果严格匹配结果少于 `min(5, topK)`，执行分级降级推荐补充候选。
11. 保存推荐记录和推荐明细，支持后续追溯。
```

推荐生成伪代码如下：

```text
function generateRecommendation(userId, demandId, topK):
    demand = loadDemand(demandId)
    weights = buildWeights(demand)
    candidates = loadApprovedCarsWithScores()
    resultItems = []
    fallbackThreshold = min(5, topK)

    strictCandidates = filterCandidates(candidates, demand, STRICT)
    resultItems.addAll(scoreAndExplain(strictCandidates, demand, weights, STRICT))
    sort resultItems by totalScore desc, reputationScore desc, popularityScore desc

    if resultItems.size < fallbackThreshold:
        for stage in [RELAX_BUDGET, RELAX_BODY_TYPE, RELAX_ENERGY_TYPE, SIMILAR_RECOMMEND]:
            stageCandidates = filterCandidates(candidates, demand, stage)

            for car in stageCandidates:
                if car.id already exists in resultItems:
                    continue

                priceScore = calculatePriceScore(car.guidePrice, demand, stage)
                totalScore = calculateTotalScore(car.featureScore, priceScore, weights)
                tags = generateTags(car.featureScore, priceScore)
                reasonText = generateReasons(car.featureScore, weights)
                weaknessText = generateWeaknesses(car.featureScore, weights)

                resultItems.add({
                    carId,
                    totalScore,
                    priceScore,
                    featureScoreSnapshot,
                    matchLevel: stage,
                    tags,
                    reasonText,
                    weaknessText
                })

            sort resultItems by totalScore desc, reputationScore desc, popularityScore desc

            if resultItems.size >= topK:
                break

    finalItems = first topK resultItems
    recommendStatus = buildRecommendStatus(finalItems)
    fallbackMessage = buildFallbackMessage(recommendStatus, strictCount(finalItems))
    record = saveRecommendRecord(demand, weights, fallbackMessage, recommendStatus)
    saveRecommendItems(record.id, finalItems)
    return finalItems
```

### 2.4 算法类型说明

本算法可以在论文中表述为：

```text
本文设计了一种基于车型内容特征与用户多维偏好的可解释推荐算法。该算法首先依据车型客观参数构建车型特征评分向量，再根据用户预算、可接受车型/动力集合、多个使用场景和显式偏好权重构建用户画像向量，随后通过多维加权匹配计算车型与用户需求之间的综合匹配度，并结合规则模板生成推荐理由和不足提醒。针对严格条件下匹配不足的问题，算法进一步设计了分级降级匹配策略，以提升推荐结果的可用性和用户体验。
```

### 2.5 与普通条件筛选的区别

普通条件筛选只回答：

```text
哪些车满足条件？
```

本项目推荐算法要回答：

```text
哪些车更适合当前用户？
为什么这些车适合？
这些车还有哪些不足？
如果没有完全匹配结果，哪些车比较接近？
```

因此，本算法不仅进行硬性条件过滤，还会对候选车型进行多维评分、加权排序、解释生成和降级处理。

### 2.6 与协同过滤和深度学习推荐的区别

| 方法 | 是否采用 | 原因 |
| --- | --- | --- |
| 条件筛选 | 部分采用 | 只作为硬性过滤步骤，不作为完整推荐算法 |
| 基于内容的推荐 | 采用 | 车型参数和用户需求都可结构化，适合本项目 |
| 多维加权评分 | 采用 | 可解释、可实现、适合论文说明 |
| 协同过滤 | 不采用 | 缺少真实用户行为数据 |
| 深度学习推荐 | 不采用 | 数据不足、复杂度高、可解释性弱 |
| 在线学习推荐 | 不采用 | 不适合当前毕设范围，可作为展望 |

---

## 3. 车型特征评分

车型特征评分将车型参数映射为统一的 0-100 分。价格分不固定存储，而是在推荐阶段根据用户预算动态计算。

| 维度 | 字段 | 主要依据 |
| --- | --- | --- |
| 空间 | `spaceScore` | 轴距、车长、座位数、车型类型 |
| 安全 | `safetyScore` | 气囊、ABS、ESP、主动刹车、车道保持、自适应巡航、并线辅助 |
| 能耗 | `energyScore` | 燃油车看油耗，纯电看续航，插混/增程看综合续航 |
| 智能 | `intelligenceScore` | 语音、OTA、中控屏、倒车影像、360 全景、辅助驾驶、自动泊车 |
| 舒适 | `comfortScore` | 当前由空间、智能和口碑组合估算，后续可扩展舒适配置字段 |
| 动力 | `powerScore` | 百公里加速，缺失时给默认分并提示 |
| 口碑 | `reputationScore` | 用户评分或测试口碑字段换算 |
| 热度 | `popularityScore` | 销量或测试热度归一化 |

### 3.1 空间分

建议规则：

```text
轴距 < 2600mm：50
2600-2699mm：65
2700-2799mm：80
2800-2899mm：90
>= 2900mm：95
SUV：+3
MPV：+8
座位数 >= 7：+5
车长 > 4800mm：+5
最高 100
```

### 3.2 安全分

建议规则：

```text
基础分：30
ABS：+10
ESP：+15
气囊数 >= 6：+20
主动刹车：+15
车道保持：+10
自适应巡航：+10
并线辅助：+5
最高 100
```

### 3.3 能耗分

燃油车：

```text
油耗 <= 5L：95
5-6L：85
6-7L：75
7-8L：65
8-10L：55
> 10L：45
```

纯电车：

```text
续航 >= 700km：95
600-699km：90
500-599km：80
400-499km：70
300-399km：60
< 300km：50
```

插混/增程：

```text
综合续航 >= 1000km：95
800-999km：85
600-799km：75
< 600km：65
```

### 3.4 智能分

建议规则：

```text
语音交互：+10
OTA：+10
中控屏 >= 12 英寸：+10
倒车影像：+8
360 全景影像：+12
L2 辅助驾驶：+20
自动泊车：+10
最高 100
```

### 3.5 动力、口碑、热度

动力分：

```text
百公里加速 <= 4s：100
4-6s：90
6-8s：80
8-10s：70
10-12s：60
> 12s：50
缺失：60
```

口碑分：

```text
reputationScore = userRating / 5.0 * 100
```

热度分：

```text
popularityScore = salesVolume / maxSales * 100
```

### 3.6 舒适分

当前数据库暂未维护座椅材质、座椅通风加热、悬架类型、隔音、空调分区等细粒度舒适性参数。为避免舒适分来源不清，第一版舒适分采用组合估算：

```text
comfortScore = spaceScore * 0.5 + intelligenceScore * 0.2 + reputationScore * 0.3
```

该规则含义是：

- 空间表现影响乘坐舒适性，占 50%。
- 智能和便利配置影响使用舒适性，占 20%。
- 口碑评分反映用户主观体验，占 30%。

如果后续增加座椅配置、隔音、悬架、空调等字段，可再扩展舒适性评分规则。

计算顺序要求：

```text
1. 先计算 spaceScore。
2. 再计算 intelligenceScore。
3. 再计算 reputationScore。
4. 最后根据三者计算 comfortScore。
```

## 4. 用户画像与权重

用户需求字段：

- 预算下限 `budgetMin`
- 预算上限 `budgetMax`
- 可接受车型类型 `bodyTypes`
- 可接受动力类型 `energyTypes`
- 座位数 `seats`
- 使用场景 `scenes`
- 显式偏好权重 `factorWeights`
- 排除品牌 `excludedBrands`
- 排除车型 `excludedCarIds`

需求模型重构后，用户端标准字段只保留 `bodyTypes`、`energyTypes`、`scenes`、`factorWeights`。旧字段 `bodyType`、`energyType`、`scene`、`focusFactors` 删除，不做长期兼容，避免推荐过滤和画像权重产生双重语义。

### 4.1 场景权重模板

| 场景 | 价格 | 空间 | 安全 | 能耗 | 智能 | 舒适 | 动力 | 口碑 | 热度 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 城市通勤 | 0.25 | 0.08 | 0.12 | 0.25 | 0.15 | 0.07 | 0.04 | 0.03 | 0.01 |
| 家庭出行 | 0.10 | 0.25 | 0.25 | 0.10 | 0.08 | 0.14 | 0.03 | 0.04 | 0.01 |
| 长途自驾 | 0.08 | 0.14 | 0.20 | 0.20 | 0.08 | 0.20 | 0.06 | 0.03 | 0.01 |
| 新手代步 | 0.25 | 0.06 | 0.25 | 0.14 | 0.20 | 0.04 | 0.03 | 0.02 | 0.01 |
| 商务接待 | 0.05 | 0.15 | 0.12 | 0.05 | 0.15 | 0.25 | 0.05 | 0.15 | 0.03 |
| 综合需求 | 0.15 | 0.13 | 0.15 | 0.13 | 0.12 | 0.12 | 0.08 | 0.07 | 0.05 |

### 4.2 多场景默认权重

`scenes` 支持多选。用户选择多个使用场景时，后端按场景模板逐维求平均，得到默认权重，再统一归一化使权重总和为 1。

```text
defaultWeight[dimension] = average(sceneTemplate[dimension] for scene in scenes)
```

规则：

- `scenes` 为空时，按 `综合需求` 模板处理。
- 多个场景只影响默认权重，不作为车型硬过滤条件。
- 未识别场景不得参与计算，应由接口参数校验拦截。

### 4.3 显式偏好权重

`factorWeights` 是用户在需求表单中通过 0-10 滑块表达的显式偏好，维度固定为：

```text
price / space / safety / energy / intelligence / comfort / power / reputation / popularity
```

生成最终权重规则：

```text
如果 factorWeights 至少一个维度 > 0：
  使用 factorWeights 归一化后的结果作为最终权重。
如果 factorWeights 全部为 0：
  使用 scenes 多场景平均后的默认权重作为最终权重。
```

说明：

- 缺失维度按 0 处理。
- 所有最终权重字段必须归一化，保证总和为 1。
- `factorWeights` 替代旧 `focusFactors` 增强规则，不再执行“每个关注因素 +0.08”的旧逻辑。
- 显式偏好权重只影响排序和解释，不应被转换为硬过滤条件。

### 4.4 画像文本

画像文本用于展示和记录，不参与复杂 NLP。

示例：

```text
家庭和长途出行用户，预算10-15万，可接受SUV或MPV，偏好插混或新能源车型，重点关注安全、空间和舒适。
```

## 5. 匹配计算

### 5.1 严格过滤

严格匹配阶段过滤：

- 未审核或已删除车型
- 用户排除品牌或车型
- 座位数不满足最低要求
- 价格高于预算上限 `budgetMax`
- 车型类型不在 `bodyTypes` 中
- 动力类型不在 `energyTypes` 展开后的集合中

其中 `budgetMax`、车型类型、动力类型可在降级阶段放宽；排除品牌、排除车型、最低座位数原则上不放宽。`bodyTypes` 或 `energyTypes` 为空时，对应维度不作为严格硬过滤条件。

预算下限 `budgetMin` 是软偏好，不作为严格过滤条件。价格低于 `budgetMin` 的车型仍可进入严格候选集，但会在动态价格分中降低匹配分。

### 5.2 车型和动力匹配规则

`car_model.energy_type` 中只保存具体动力类型：

```text
燃油
纯电
插混
增程
```

`新能源` 不是车型表中的真实动力类型，而是用户侧宽泛偏好。`energyTypes` 支持多选，用户选择 `新能源` 时，严格阶段展开匹配：

```text
纯电 / 插混 / 增程
```

用户同时选择多个具体动力类型时，严格阶段匹配任一具体类型。若 `energyTypes` 同时包含 `新能源` 和具体动力类型，应先展开并去重。

`bodyTypes` 支持多选。车型类型严格匹配只使用数据库中存在的类型，匹配任一选中类型即可：

```text
SUV / 轿车 / MPV
```

### 5.3 动态价格分

价格分根据预算动态计算：

- 未填写预算上限和预算下限：给中性分 75。
- 仅填写预算上限：可将预算下限视为 0。
- 仅填写预算下限：预算下限仍是软偏好，预算上限为空时不执行超预算过滤；`price < budgetMin` 按低于预算下限公式计算，`price >= budgetMin` 给 90 分。

当 `budgetMin` 和 `budgetMax` 都存在时，采用确定公式：

```text
budgetMid = (budgetMin + budgetMax) / 2
budgetRange = budgetMax - budgetMin
```

1. 当 `price` 在 `[budgetMin, budgetMax]` 内：

```text
distanceRatio = abs(price - budgetMid) / max(budgetRange / 2, 1)
priceScore = 100 - distanceRatio * 10
priceScore = max(priceScore, 90)
```

含义：预算区间中位数附近价格匹配最高，越靠近边界略低，但区间内最低不低于 90。

2. 当 `price < budgetMin`：

```text
lowerRatio = (budgetMin - price) / max(budgetMin, 1)
priceScore = 90 - lowerRatio * 50
priceScore = max(priceScore, 75)
```

含义：低于预算下限通常仍可接受，但过低可能代表级别或配置不符合预期，因此给 75-90 分。

3. 当 `price > budgetMax`：

严格匹配阶段直接过滤。降级阶段使用：

```text
overRatio = (price - budgetMax) / max(budgetMax, 1)
priceScore = 80 - overRatio * 100
priceScore = max(priceScore, 50)
```

含义：超过预算的车型只能在降级阶段进入，且超出越多价格分越低，最低不低于 50。

### 5.4 综合分

```text
totalScore =
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

推荐结果按综合分倒序排列，分数相近时可用口碑和热度作为次级排序。

## 6. 推荐解释

### 6.1 推荐理由

生成流程：

1. 选择用户权重最高的 3-4 个维度。
2. 从这些维度中找出车型得分 >= 80 的维度。
3. 生成 2-3 条自然语言理由。
4. 若高权重维度没有高分，则选择车型最高分维度补充说明。
5. 若仍无法生成具体理由，使用保底文案：“该车型在多个维度上与您的需求较为接近，可作为备选车型进一步对比。”

示例：

```text
该车型空间表现较好，适合家庭出行和多人乘坐场景。
该车型安全配置得分较高，符合您对安全性的关注。
```

### 6.2 不足提醒

生成流程：

1. 选择用户权重较高的维度。
2. 找出车型得分 < 65 的维度。
3. 生成 1-2 条不足提醒。
4. 如果没有明显短板，提示“该车型整体匹配较均衡，暂无明显短板。”

示例：

```text
该车型智能配置表现一般，如果您更看重车机体验和辅助驾驶，可继续对比其他车型。
```

## 7. 推荐标签

推荐标签 `tags` 用于前端卡片快速展示车型优势，不替代推荐理由。标签由车型高分维度和价格匹配分生成。

生成规则：

- 从得分最高且达到阈值的维度中选择 2-3 个标签。
- 若 `spaceScore >= 85`，生成“空间优秀”。
- 若 `safetyScore >= 85`，生成“安全配置高”。
- 若 `energyScore >= 85`，生成“能耗表现好”。
- 若 `intelligenceScore >= 85`，生成“智能配置丰富”。
- 若 `comfortScore >= 85`，生成“舒适性较好”。
- 若 `powerScore >= 85`，生成“动力表现强”。
- 若 `reputationScore >= 85`，生成“口碑较好”。
- 若 `popularityScore >= 85`，生成“热门车型”。
- 若 `priceScore >= 90`，生成“价格匹配度高”。
- 如果没有维度达到 85，则选择最高的 1-2 个维度生成“表现均衡”“接近需求”等保底标签。

标签仅用于摘要展示，详细解释仍以 `reasonText` 和 `weaknessText` 为准。

## 8. 降级推荐

当严格匹配结果少于 `min(5, topK)` 条时触发降级，避免 `topK < 5` 时阈值不合理。用户端不展示推荐数量输入，`topK` 由后端默认值或内部调用控制，最终推荐默认返回 Top 10。

| 阶段 | 匹配状态 | 处理方式 | 提示 |
| --- | --- | --- | --- |
| 1 | `STRICT` | 严格匹配 | 完全符合您的筛选条件 |
| 2 | `RELAX_BUDGET` | 预算上限放宽 10% | 系统已适度放宽预算上限 |
| 3 | `RELAX_BODY_TYPE` | 放宽车型类型 | 系统已扩展相近车型类型 |
| 4 | `RELAX_ENERGY_TYPE` | 放宽动力类型 | 系统已扩展相近动力类型 |
| 5 | `SIMILAR_RECOMMEND` | 按综合分返回相似车型 | 以下车型并非完全匹配，但在核心偏好上较接近 |

降级推荐必须展示匹配状态和放宽原因，不允许隐式放宽条件。

各阶段过滤边界：

- `STRICT`：使用全部硬性条件；`price > budgetMax` 时过滤，`budgetMin` 不过滤。
- `RELAX_BUDGET`：仅放宽预算上限。若存在 `budgetMax`，允许 `price <= budgetMax * 1.10`；若没有 `budgetMax`，该阶段不产生额外预算放宽候选。
- `RELAX_BODY_TYPE`：仅放宽车型类型到 `bodyTypes` 对应映射类型的并集，其余硬性条件仍按严格阶段执行；若 `bodyTypes` 为空，该阶段通常不产生额外车型类型放宽候选。
- `RELAX_ENERGY_TYPE`：仅放宽动力类型到 `energyTypes` 对应映射类型的并集，其余硬性条件仍按严格阶段执行；若 `energyTypes` 为空，该阶段通常不产生额外动力类型放宽候选。
- `SIMILAR_RECOMMEND`：保留审核状态、排除品牌、排除车型、最低座位数等不可放宽条件，预算、车型类型和动力类型可不再作为硬过滤，但仍参与价格分和综合排序。

降级推荐采用分阶段候选集补充策略，而不是用后续阶段覆盖前一阶段结果。

具体规则：

```text
1. 系统首先获取 STRICT 候选车型。
2. 如果 STRICT 数量达到 min(5, topK)，不启动降级，直接返回严格匹配 TopK。
3. 如果 STRICT 数量不足 min(5, topK)，进入 RELAX_BUDGET 阶段补充新候选。
4. 如果仍不足 topK，则进入 RELAX_BODY_TYPE 阶段补充新候选。
5. 如果仍不足 topK，则进入 RELAX_ENERGY_TYPE 阶段补充新候选。
6. 如果仍不足 topK，则进入 SIMILAR_RECOMMEND 阶段补充新候选。
7. 每个阶段新增候选必须按 carId 去重。
8. 同一辆车只保留首次进入推荐集时对应的 matchLevel。
9. 每轮补充后重新按 totalScore 排序。
10. 达到 topK 后停止继续放宽。
```

这样可以保证严格匹配结果不会被宽松阶段覆盖，也可以避免同一辆车在多个阶段重复出现。

### 8.1 车型类型降级映射

只使用数据库中存在的车型类型，不引入“跨界车”等额外类型。

```text
SUV -> MPV
MPV -> SUV
轿车 -> SUV
```

当用户选择多个 `bodyTypes` 时，放宽车型类型取每个已选类型映射结果的并集，并排除严格阶段已经覆盖的车型类型。例如 `["SUV", "轿车"]` 可扩展出 `MPV`，但 `SUV` 已属于严格匹配集合，不应作为新的降级类型重复加入。

### 8.2 动力类型降级映射

动力类型放宽规则：

```text
纯电 -> 插混 / 增程
插混 -> 增程 / 纯电
增程 -> 插混 / 纯电
燃油 -> 插混
新能源 -> 纯电 / 插混 / 增程
```

说明：

- 用户选择 `新能源` 时，严格阶段已经匹配 `纯电 / 插混 / 增程`，通常不需要额外动力放宽。
- 当用户选择多个 `energyTypes` 时，放宽动力类型取每个已选类型映射结果的并集，并排除严格阶段已经覆盖的动力类型。
- 排除品牌、排除车型、最低座位数不参与降级放宽。

### 8.3 降级提示生成

`fallbackMessage` 根据 `recommendStatus` 和最终返回结果中的严格匹配数量 `strictCount` 生成，避免顶部提示与单条 `matchLevel` 状态产生语义冲突。

| 条件 | 推荐提示 |
| --- | --- |
| `recommendStatus = SUCCESS` | 空字符串或“已为您找到完全匹配车型” |
| `recommendStatus = FALLBACK` 且 `strictCount > 0` | “完全匹配车型数量不足，系统已补充部分降级推荐车型，并在每条结果中标明匹配状态。” |
| `recommendStatus = FALLBACK` 且 `strictCount = 0` | “未找到完全匹配车型，系统已根据您的核心偏好提供相近推荐。” |
| `recommendStatus = EMPTY` | “暂未找到合适车型，请调整预算、车型类型或动力类型后重试。” |

如需按最高放宽阶段细化文案，也必须先区分 `strictCount > 0` 和 `strictCount = 0`。只要最终结果中仍存在 `STRICT` 明细，顶部文案就不应表达为“未找到完全匹配车型”。

### 8.4 推荐状态生成

`recommendStatus` 是一次推荐记录的总体状态，`matchLevel` 是单条推荐明细的匹配状态，`fallbackMessage` 是给用户展示的放宽条件摘要。三者职责不同，不能混用。

`recommendStatus` 根据最终返回并保存的推荐明细生成：

```text
SUCCESS：最终推荐结果不为空，且所有推荐项的 matchLevel 均为 STRICT。
FALLBACK：最终推荐结果不为空，且至少存在一个推荐项的 matchLevel 不是 STRICT。
EMPTY：所有阶段都没有候选结果，最终推荐结果为空。
```

`fallbackMessage` 根据 `recommendStatus` 和 `strictCount` 生成。如果 `recommendStatus = EMPTY`，提示“暂未找到合适车型，请调整预算、车型类型或动力类型后重试。”

## 9. 推荐追溯要求

一次推荐必须保存：

- 用户需求
- 画像文本
- 权重快照
- 降级提示
- 推荐状态

每个推荐明细必须保存：

- 排名
- 车型 ID
- 综合分
- 价格分
- 各维度分
- 匹配状态
- 推荐标签
- 推荐理由
- 不足提醒

这些字段用于历史记录、管理端推荐记录页和论文可解释性说明。推荐明细中的分数、标签、理由、不足和匹配状态都是推荐发生时的快照，历史查询时不得重新计算覆盖。

## 10. 自然语言解析范围

自然语言解析是第二档功能，只作为辅助填写，不替代结构化表单。解析后必须允许用户确认修改。

最低支持：

```text
xx万以内、xx到xx万
一个或多个车型类型：SUV、轿车、MPV
一个或多个动力类型：燃油、纯电、插混、增程、新能源
一个或多个使用场景：家用、通勤、长途、新手、商务
偏好词映射为 factorWeights 初始值：省油、空间大、安全、智能、舒适、动力强、口碑、热门、性价比
```

增强支持：

- `unsupportedTerms`
- `ambiguousTerms`
- `confidenceScore`
- 解析记录保存

## 11. 核心测试场景

家庭出行：

```text
预算 10-15 万，可接受 SUV 或 MPV，可接受插混或新能源，场景为家庭出行和长途自驾，空间和安全滑块较高。
期望：推荐空间分和安全分较高的车型。
```

城市通勤：

```text
预算 8-12 万，可接受轿车或 SUV，动力类型可接受燃油或纯电，场景为城市通勤，价格和能耗滑块较高。
期望：推荐价格分和能耗分较高的车型。
```

极端条件：

```text
预算 5 万，7 座，车型只接受 SUV，动力只接受纯电，空间和安全滑块较高。
期望：触发降级推荐，不返回空白。
```

自然语言：

```text
我想买15万以内的SUV或MPV，家用也要能长途，最好省油安全。
期望：识别预算、多个车型类型、多个使用场景，并生成能耗和安全相关 factorWeights。
```
