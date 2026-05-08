# 推荐算法文档

当前推荐算法版本：

```text
pareto-topsis-v1
```

算法名称：

```text
主客观组合权重 + Pareto-TOPSIS
```

该算法用于冷启动购车场景。系统不依赖长期点击、收藏、购买等行为训练模型，而是基于车型内容特征、用户结构化需求和多指标决策方法生成可解释推荐。

## 输入数据

| 数据 | 说明 |
| --- | --- |
| `car_model` | 车型基础信息、指导价、车型类型、动力类型、座位数、审核状态 |
| `car_param` | 车型参数和配置，用于评分来源追溯 |
| `car_feature_score` | 空间、安全、能耗、智能、舒适、动力、口碑、热度八维静态评分 |
| `user_demand` | 用户预算、品牌筛选、车型偏好、动力偏好、座位选项、场景和显式权重 |
| `recommend_record` / `recommend_item` | 推荐结果、权重和解释快照 |

当前用户需求字段：

- `budgetMin`
- `budgetMax`
- `brands`
- `bodyTypes`
- `energyTypes`
- `seatOptions`
- `scenes`
- `factorWeights`

`minSeats`、`excludedBrands`、`excludedCarIds` 是兼容字段，当前产品前端不再展示排除品牌或排除车型入口；候选过滤优先使用 `seatOptions`，仅在其为空时兼容 `minSeats`。

动力规则：

- 用户需求侧可以选择 `新能源`。
- 后端将 `新能源` 展开为 `纯电 / 插混 / 增程`。
- `car_model.energy_type` 只保存具体动力类型：`燃油 / 纯电 / 插混 / 增程`。

## 车型特征评分

车型特征评分负责把 `car_model` 和 `car_param` 中的车型基础数据转成 0-100 的静态评分向量，保存到 `car_feature_score`。

当前八维静态评分：

| 评分字段 | 推荐维度 | 来源 |
| --- | --- | --- |
| `space_score` | `space` | 轴距、车长、座位数、车型类型 |
| `safety_score` | `safety` | 气囊、ABS、ESP、主动刹车、车道保持、自适应巡航、并线辅助 |
| `energy_score` | `energy` | 燃油车油耗、纯电续航、插混和增程综合续航 |
| `intelligence_score` | `intelligence` | OTA、语音、中控屏、影像、辅助驾驶、自动泊车 |
| `comfort_score` | `comfort` | 空间、智能、口碑组合估算 |
| `power_score` | `power` | 百公里加速 |
| `reputation_score` | `reputation` | `user_rating / 5 * 100` |
| `popularity_score` | `popularity` | `sales_volume / maxSalesVolume * 100` |

当前舒适分公式：

```text
comfortScore = spaceScore * 0.5 + intelligenceScore * 0.2 + reputationScore * 0.3
```

`priceScore` 不属于车型静态评分，不写入 `car_feature_score`。价格匹配分只在推荐生成时根据用户预算动态计算。

评分重算入口：

```text
POST /api/admin/cars/{id}/score/recalculate
POST /api/admin/cars/scores/recalculate
```

初始化种子数据、修改评分规则或修改销量后，应执行全部车型评分重算。

## 推荐九维指标

推荐生成使用九个正向指标，分数范围均为 0-100：

| 指标 | 来源 | 说明 |
| --- | --- | --- |
| `price` | 动态 `priceScore` | 根据用户预算在推荐生成时计算 |
| `space` | `spaceScore` | 空间表现 |
| `safety` | `safetyScore` | 安全配置 |
| `energy` | `energyScore` | 能耗或续航 |
| `intelligence` | `intelligenceScore` | 智能配置 |
| `comfort` | `comfortScore` | 舒适性 |
| `power` | `powerScore` | 动力表现 |
| `reputation` | `reputationScore` | 口碑表现 |
| `popularity` | `popularityScore` | 热度表现 |

价格已经转换成正向 `priceScore`，不再作为成本型指标处理。

## 硬约束与候选生成

候选生成先于 TOPSIS 排序执行。

硬约束：

- 审核通过且未删除。
- 存在 `car_feature_score`。
- `brands` 非空时，车型品牌必须在 `brands` 内。
- 兼容字段 `excludedBrands`、`excludedCarIds` 命中时排除。
- `seatOptions` 非空时按选项并集匹配；包含 `7_PLUS` 时允许 `seats >= 7`。
- `seatOptions` 为空且 `minSeats` 非空时，兼容旧逻辑 `seats >= minSeats`。
- 严格匹配时满足用户预算区间：`budgetMin <= guidePrice <= budgetMax`。
- 只填写 `budgetMax` 时，严格匹配要求 `guidePrice <= budgetMax`。
- 只填写 `budgetMin` 时，严格匹配要求 `guidePrice >= budgetMin`。
- 严格匹配时命中 `bodyTypes`。
- 严格匹配时命中展开后的 `energyTypes`。

候选补充顺序：

1. `STRICT`
2. `RELAX_BUDGET`
3. `RELAX_BODY_TYPE`
4. `RELAX_ENERGY_TYPE`
5. `SIMILAR_RECOMMEND`

规则：

- `STRICT` 组整体优先展示。
- 非 `STRICT` 结果作为推荐补充，不能排到 `STRICT` 组之前。
- 同一车型只进入推荐集一次。
- `matchLevel` 保留车型首次进入推荐集时的状态。
- `brands`、`seatOptions`、`minSeats`、`excludedBrands`、`excludedCarIds` 不参与放宽。
- `RELAX_BUDGET` 只补充预算区间外但接近用户预算的车型；低于预算下限或高于预算上限的车型不能标记为 `STRICT`。
- 预算放宽阶段仍要求命中严格车型类型和严格动力类型。
- 若存在 `budgetMin`，预算放宽下界为 `budgetMin * 0.9`。
- 若存在 `budgetMax`，预算放宽上界为 `budgetMax * 1.1`。
- 推荐请求不包含推荐数量字段。

## 动态价格分

`budgetMin` 和 `budgetMax` 都是 `STRICT` 阶段的预算边界。预算下限存在时，低于下限的车型不能进入 `STRICT`；预算上限存在时，高于上限的车型不能进入 `STRICT`。预算区间外但接近区间的车型只能作为预算放宽推荐进入推荐组。

`priceScore` 是推荐生成时动态计算的价格匹配分，用于表示车型价格与用户预算区间的接近程度，不写入 `car_feature_score`。

预算上下限都存在时：

```text
budgetMid = (budgetMin + budgetMax) / 2
budgetRange = budgetMax - budgetMin
distanceRatio = abs(price - budgetMid) / max(budgetRange / 2, 1)
priceScore = max(100 - distanceRatio * 10, 90)
```

低于预算下限时：

```text
lowerRatio = (budgetMin - price) / budgetMin
priceScore = max(90 - lowerRatio * 50, 75)
```

补充候选超出预算上限时：

```text
overRatio = (price - budgetMax) / budgetMax
priceScore = max(80 - overRatio * 100, 50)
```

未填写预算时：

```text
priceScore = 75
```

## 主观权重

用户通过 `factorWeights` 表达 0-10 的显式偏好。

```text
subjectiveWeight_j = rawWeight_j / sum(rawWeight)
```

规则：

- 只要 `factorWeights` 至少一个维度大于 0，就按滑块值归一化。
- 如果 `factorWeights` 全部为 0 或未传，则按 `scenes` 场景模板生成。
- 如果 `scenes` 为空，则使用 `综合需求` 模板。
- 九维权重总和必须为 1。

## 客观权重

客观权重使用熵权法衡量当前候选集在各指标上的差异度。差异越大，该指标对排序的客观区分价值越高。

给定 `n` 辆候选车、`m = 9` 个指标：

```text
x_ij = 第 i 辆车在第 j 个指标上的分数
x'_ij = max(x_ij, epsilon)
epsilon = 0.0001
p_ij = x'_ij / sum_i(x'_ij)
e_j = -1 / ln(n) * sum_i(p_ij * ln(p_ij))
d_j = 1 - e_j
objectiveWeight_j = d_j / sum_j(d_j)
```

边界规则：

- 候选车型数量 `n <= 1` 时，`objectiveWeight` 使用 `subjectiveWeight`。
- 所有差异系数之和不可用时，`objectiveWeight` 使用 `subjectiveWeight`。
- 权重归一化异常时，使用九维均匀权重。

## 组合权重

组合权重公式：

```text
finalWeight_j =
  alpha * subjectiveWeight_j
  + (1 - alpha) * objectiveWeight_j
```

`alpha` 规则：

- 用户显式设置 `factorWeights` 时，`alpha = 0.75`。
- 用户仅依赖 `scenes` 场景模板时，`alpha = 0.60`。

`finalWeight` 计算后再次归一化，总和为 1。`recommend_record.weight_snapshot` 保存：

- `algorithmVersion`
- `alpha`
- `subjectiveWeight`
- `objectiveWeight`
- `finalWeight`

## Pareto 非支配标记

Pareto 识别用于同组排序辅助和算法可视化，不删除候选。

高权重维度集合 `K`：

```text
K = finalWeight 最高的前 4 个维度
```

支配关系：

```text
如果车型 A 在所有 j in K 上 score_Aj >= score_Bj
并且至少一个 j in K 满足 score_Aj > score_Bj
则 A 支配 B
```

规则：

- 被支配车型不删除。
- 非支配车型作为同分辅助排序优先项。
- Pareto 标记不写入用户端推荐标签。
- 普通用户端不展示 Pareto 术语；管理端和 `/algorithm-demo` 可以展示。

## TOPSIS 推荐分

输入矩阵：

```text
X = n * 9 的决策矩阵
x_ij = 第 i 辆车第 j 个指标的原始分数
```

向量归一化：

```text
r_ij = x_ij / sqrt(sum_i(x_ij^2))
```

加权归一化：

```text
v_ij = r_ij * finalWeight_j
```

正理想解与负理想解：

```text
A+_j = max_i(v_ij)
A-_j = min_i(v_ij)
```

距离：

```text
D_i+ = sqrt(sum_j((v_ij - A+_j)^2))
D_i- = sqrt(sum_j((v_ij - A-_j)^2))
```

相对接近度：

```text
C_i = D_i- / (D_i+ + D_i-)
```

综合推荐分：

```text
totalScore = C_i * 100
```

当候选数为 1，或 `D_i+ + D_i- = 0` 导致 TOPSIS 正负理想解距离不可区分时，使用推荐生成前计算的 `fallbackScore`：

```text
fallbackScore_i = sum_j(x_ij * subjectiveWeightFromUserDemand_j)
```

`fallbackScore` 由 `RecommendationServiceImpl.calculateFallbackScore` 计算，当前使用 `user_demand` 中保存的主观权重字段和九维候选分数进行加权。该分数只用于 TOPSIS 边界场景兜底，不替代正常 TOPSIS 排序。`totalScore` 保留 2 位小数，并限制在 0-100。

## 排序规则

后端生成并写入 `rankNo`：

1. `STRICT` 组在前。
2. 推荐组在后。
3. 同组内部按 `totalScore desc`。
4. 同分时优先 Pareto 非支配车型。
5. 再按 `reputationScore desc`。
6. 再按 `popularityScore desc`。

`rankNo` 是前端展示排序的唯一权威。前端不得按 `totalScore`、口碑分、热度分或其他字段重新排序。

## 推荐解释

推荐解释由 `tags`、`reasonText` 和 `weaknessText` 组成，并保存到 `recommend_item` 快照。

### `tags`

`tags` 只表示车型亮点，例如：

- 空间优秀
- 安全配置高
- 能耗表现好
- 智能配置丰富
- 舒适性较好
- 动力表现强
- 口碑较好
- 热门车型
- 价格匹配度高
- 接近理想车型
- 多维表现均衡

`tags` 不包含 `matchLevel`、Pareto、TOPSIS、熵权术语或补充推荐技术状态。

### `reasonText`

推荐理由来自贡献度较高且原始维度表现不差的指标：

```text
contribution_ij = weightedNormalizedScore_ij
```

系统选择贡献较高的 2-3 个维度生成用户可读理由，不在用户端暴露公式变量。

### `weaknessText`

不足提醒来自用户高权重维度中与正理想解差距较大的指标：

```text
gap_ij = A+_j - v_ij
```

没有明显短板时使用：

```text
该车型整体匹配较均衡，暂无明显短板。
```

## 快照追溯

`recommend_record` 保存：

- `algorithmVersion`
- `alpha`
- `subjectiveWeight`
- `objectiveWeight`
- `finalWeight`
- `profileText`
- `recommendStatus`
- `fallbackMessage`

`recommend_item` 保存：

- `rankNo`
- `totalScore`
- `priceScore`
- 八维静态分数快照
- `matchLevel`
- `tags`
- `reasonText`
- `weaknessText`

历史详情读取保存快照，不重新计算历史推荐结果、分数、标签、理由、不足或匹配状态。

## 算法可视化

`/algorithm-demo` 通过只读接口读取推荐快照：

```text
GET /api/recommend/{recordId}/algorithm-visualization
```

该接口可以展示：

- 需求和约束。
- 九维评分矩阵。
- 主观权重、客观权重和组合权重。
- `alpha`。
- Pareto 标记。
- TOPSIS 距离和相对接近度。
- 推荐解释和快照边界。

该接口不生成推荐，不写数据库，不覆盖历史快照。

## 与普通筛选的区别

普通筛选只回答“哪些车型满足条件”。当前系统先按硬性条件生成候选，再将车型转成九维评分矩阵，结合用户主观权重和候选集客观差异得到 `finalWeight`，通过 TOPSIS 相对接近度计算 `totalScore`，并用 Pareto 非支配标记做同分辅助排序和算法追溯。

系统最终回答：

- 哪些车型符合或接近需求。
- 哪些车型在多指标决策下更适合。
- 为什么推荐。
- 哪里可能不足。
- 如果没有严格匹配，系统如何补充推荐。
- 历史推荐能否按当时快照回查。

## 边界情况

- TOPSIS 是相对候选集排序，候选集变化会影响 `totalScore`。
- 熵权法在候选很少或候选无差异时退化为 `subjectiveWeight`。
- 舒适分当前由空间、智能和口碑组合估算。
- Pareto 标记不持久化到 `recommend_item`，历史主要通过 `rankNo`、`totalScore`、维度分和权重快照追溯。
- 当前没有使用用户行为在线学习。
- 当前没有使用深度学习或协同过滤。

## 修改算法时的测试要求

修改推荐算法时至少覆盖：

- 显式 `factorWeights` 归一化。
- `scenes` 场景权重兜底。
- 熵权法正常候选和边界候选。
- `alpha` 规则。
- `finalWeight` 总和为 1。
- Pareto 非支配识别。
- TOPSIS 排序稳定性。
- 边界兜底计算。
- `STRICT` 组始终在推荐组前。
- `rankNo` 连续且为前端排序权威。
- 历史详情读取快照，不重新计算。
- 用户端推荐标签不包含算法术语或匹配状态。
