# 当前主算法说明：主客观组合权重 + Pareto-TOPSIS

本文档是当前主推荐算法实现文档，算法版本为 `pareto-topsis-v1`。算法名称为：

```text
基于主客观组合权重与 Pareto-TOPSIS 的可解释汽车推荐算法
```

本文只维护当前公式、流程、排序、解释和快照规则。车型特征评分规则见 `RECOMMENDATION_ALGORITHM.md`，代码调用链见 `RECOMMENDATION_IMPLEMENTATION_LOGIC.md`。

## 1. 算法定位

本系统面向冷启动购车场景，使用车型内容特征和用户结构化需求进行多指标决策推荐。系统不依赖长期用户点击、收藏、购买行为训练模型，因此不属于深度学习、协同过滤或在线学习推荐。

推荐算法先通过硬性约束生成候选集，再使用主客观组合权重、Pareto 非支配识别和 TOPSIS 相对接近度计算排序，最后保存推荐标签、推荐理由、不足提醒、分数和权重快照。

## 2. 输入数据

推荐计算使用以下数据：

| 数据 | 说明 |
| --- | --- |
| `car_model` | 车型基础信息、指导价、车型类型、动力类型、座位数、审核状态 |
| `car_param` | 车型参数和配置，用于评分来源追溯 |
| `car_feature_score` | 空间、安全、能耗、智能、舒适、动力、口碑、热度八维静态评分 |
| `user_demand` | 用户预算、车型偏好、动力偏好、最低座位数、场景、显式权重和排除条件 |
| `recommend_record` / `recommend_item` | 推荐结果和解释快照 |

当前用户需求字段：

- `bodyTypes`
- `energyTypes`
- `scenes`
- `factorWeights`
- `minSeats`
- `budgetMin`
- `budgetMax`
- `excludedBrands`
- `excludedCarIds`

动力规则：

- 用户需求侧可以选择 `新能源`。
- 后端将 `新能源` 展开为 `纯电 / 插混 / 增程`。
- `car_model.energy_type` 只保存具体动力类型，例如 `燃油 / 纯电 / 插混 / 增程`。

## 3. 九维指标体系

推荐阶段使用九个正向指标，分数范围均为 0-100：

| 指标 | 来源 | 说明 |
| --- | --- | --- |
| `price` | 动态 `priceScore` | 根据用户预算在推荐阶段计算 |
| `space` | `spaceScore` | 空间表现 |
| `safety` | `safetyScore` | 安全配置 |
| `energy` | `energyScore` | 能耗或续航 |
| `intelligence` | `intelligenceScore` | 智能配置 |
| `comfort` | `comfortScore` | 舒适性 |
| `power` | `powerScore` | 动力表现 |
| `reputation` | `reputationScore` | 口碑表现 |
| `popularity` | `popularityScore` | 热度表现 |

价格不作为成本型指标进入 TOPSIS，因为推荐阶段已经将价格转换成正向 `priceScore`。

## 4. 候选生成

候选生成先于 TOPSIS 排序执行。匹配阶段固定为：

1. `STRICT`
2. `RELAX_BUDGET`
3. `RELAX_BODY_TYPE`
4. `RELAX_ENERGY_TYPE`
5. `SIMILAR_RECOMMEND`

规则：

- `STRICT` 组整体优先展示。
- 推荐组作为补充候选，不能排到 `STRICT` 组之前。
- 同一车型只进入推荐集一次，`matchLevel` 保留首次进入推荐集的阶段。
- `minSeats`、`excludedBrands`、`excludedCarIds` 不参与放宽。
- 推荐请求不包含推荐数量字段，后端按候选集、分组和排序规则返回结果。

## 5. 动态价格分

`budgetMax` 是严格阶段预算硬约束。`budgetMin` 是软偏好，只影响价格匹配分。

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

`priceScore` 只在推荐阶段计算，不写入 `car_feature_score`。

## 6. 主观权重 subjectiveWeight

用户通过 `factorWeights` 表达 0-10 的显式偏好。

```text
subjectiveWeight_j = rawWeight_j / sum(rawWeight)
```

规则：

- 只要 `factorWeights` 至少一个维度大于 0，就按滑块值归一化。
- 如果 `factorWeights` 全部为 0 或未传，则按 `scenes` 场景模板生成。
- 如果 `scenes` 为空，则使用“综合需求”模板。
- 九维权重总和必须为 1。

## 7. 客观权重 objectiveWeight

熵权法用于衡量当前候选集在各指标上的差异度。差异越大，该指标对排序的客观区分价值越高。

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

## 8. 组合权重 finalWeight

组合权重公式：

```text
finalWeight_j =
  alpha * subjectiveWeight_j
  + (1 - alpha) * objectiveWeight_j
```

`alpha` 规则：

- 用户显式设置 `factorWeights` 时，`alpha = 0.75`。
- 用户仅依赖 `scenes` 场景模板时，`alpha = 0.60`。

`finalWeight` 计算后再次归一化，总和为 1。`recommend_record.weight_snapshot` 保存 `algorithmVersion`、`alpha`、`subjectiveWeight`、`objectiveWeight` 和 `finalWeight`。

## 9. Pareto 非支配识别

Pareto 识别用于辅助同组排序和算法解释，不删除候选。

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

## 10. TOPSIS 推荐分

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

边界兜底计算：

```text
utilityScore_i = sum_j(x_ij * finalWeight_j)
```

当候选数为 1，或 `D_i+ + D_i- = 0` 时，使用边界兜底分；如兜底分也不可用，使用 50。`totalScore` 保留 2 位小数，并限制在 0-100。

## 11. 最终排序

推荐结果排序由后端生成并写入 `rankNo`：

1. `STRICT` 组在前。
2. 推荐组在后。
3. 同组内部按 `totalScore desc`。
4. 同分时优先 Pareto 非支配车型。
5. 再按 `reputationScore desc`。
6. 再按 `popularityScore desc`。

前端必须按 `rankNo` 展示，不按 `totalScore`、口碑、热度或其他字段重新排序。

## 12. 推荐解释

推荐解释由 `tags`、`reasonText` 和 `weaknessText` 组成，并保存到 `recommend_item` 快照。

### tags

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

`tags` 不包含匹配阶段、Pareto、TOPSIS 或熵权术语。

### reasonText

推荐理由来自贡献度较高且原始维度表现不差的指标：

```text
contribution_ij = weightedNormalizedScore_ij
```

选择贡献较高的 2-3 个维度生成用户可读理由，不在用户端暴露公式变量。

### weaknessText

不足提醒来自用户高权重维度中与正理想解差距较大的指标：

```text
gap_ij = A+_j - v_ij
```

没有明显短板时使用保底文案：

```text
该车型整体匹配较均衡，暂无明显短板。
```

## 13. 快照规则

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

历史详情读取保存快照，不重新计算历史推荐结果、分数、标签、理由、不足或匹配阶段。

## 14. 前端展示边界

用户端推荐结果页展示：

- 完全匹配车型。
- 推荐。
- 综合推荐分。
- 推荐标签。
- 推荐理由。
- 不足提醒。
- 维度评分。

用户端不展示 TOPSIS、Pareto、熵权等复杂术语，也不把 `matchLevel` 写入推荐标签。管理端和 `/algorithm-demo` 可以展示算法版本、权重、候选阶段、Pareto 标记、TOPSIS 中间值和快照边界。

## 15. 测试要求

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
- 用户端推荐标签不包含算法术语或匹配阶段。

## 16. 答辩表述

本系统不是普通车型筛选列表，也不是依赖用户行为数据的复杂模型。系统面向冷启动购车场景，基于车型内容特征和用户结构化需求，使用主客观组合权重与 Pareto-TOPSIS 进行多指标决策推荐。

推荐结果不仅返回车型和分数，还保存推荐标签、推荐理由、不足提醒、匹配状态、权重快照和推荐明细快照。因此系统具备可解释、可降级、可追溯的特点，适合缺少长期用户行为数据的本科毕设汽车购买推荐场景。
