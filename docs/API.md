# API 文档

本文档描述当前后端接口、统一响应、枚举和核心字段。所有接口基础路径为 `/api`。

## 通用约定

统一响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页响应：

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "size": 10
}
```

常用错误码：

| code | 含义 |
| --- | --- |
| `200` | 成功 |
| `400` | 请求参数错误 |
| `401` | 未登录或 token 无效 |
| `403` | 无权限 |
| `404` | 数据不存在 |
| `500` | 系统异常 |

## 枚举

| 字段 | 可选值 |
| --- | --- |
| `bodyTypes[]` | `SUV` / `轿车` / `MPV` |
| `carModel.energyType` | `燃油` / `纯电` / `插混` / `增程` |
| `userDemand.energyTypes[]` | `燃油` / `纯电` / `插混` / `增程` / `新能源` |
| `scenes[]` | `城市通勤` / `家庭出行` / `长途自驾` / `新手代步` / `商务接待` / `综合需求` |
| `factorWeights` keys | `price` / `space` / `safety` / `energy` / `intelligence` / `comfort` / `power` / `reputation` / `popularity` |
| `auditStatus` | `APPROVED` / `PENDING` / `REJECTED` |
| `recommendStatus` | `SUCCESS` / `FALLBACK` / `EMPTY` |
| `matchLevel` | `STRICT` / `RELAX_BUDGET` / `RELAX_BODY_TYPE` / `RELAX_ENERGY_TYPE` / `SIMILAR_RECOMMEND` |
| `satisfactionLevel` | `SATISFIED` / `NEUTRAL` / `DISSATISFIED` |

`新能源` 只作为用户需求中的宽泛动力偏好。后端匹配时展开为 `纯电 / 插混 / 增程`，车型表不保存该值。

## 默认用户

需要用户上下文的接口可以传 `userId`。`userId` 为空时，后端使用默认用户上下文：

```text
app_user.id = 1
```

管理端默认管理员上下文：

```text
admin.id = 1
```

## 健康检查

```text
GET /api/health
```

该接口由管理端 `/admin/health` 页面调用，用于查看后端服务和数据库连接状态。普通用户首页不调用该接口。

响应示例：

```json
{
  "backend": "running",
  "database": "connected"
}
```

## 车型接口

管理端车型基础信息：

```text
GET    /api/admin/cars
GET    /api/admin/cars/{id}
POST   /api/admin/cars
PUT    /api/admin/cars/{id}
DELETE /api/admin/cars/{id}
```

分页查询参数：

| 参数 | 说明 |
| --- | --- |
| `page` | 页码 |
| `size` | 每页数量 |
| `keyword` | 品牌、车系、车型名称关键词 |
| `bodyType` | 车型类型 |
| `energyType` | 动力类型 |
| `minPrice` | 最低价，单位元 |
| `maxPrice` | 最高价，单位元 |

车型保存请求核心字段：

```json
{
  "brand": "比亚迪",
  "series": "宋PLUS",
  "modelName": "宋PLUS DM-i",
  "guidePrice": 159800,
  "bodyType": "SUV",
  "energyType": "插混",
  "seats": 5,
  "launchYear": 2025,
  "imageUrl": "",
  "salesVolume": 10000,
  "userRating": 4.6,
  "auditStatus": "APPROVED"
}
```

用户端车型只读接口：

```text
GET /api/car/{id}
GET /api/car/brands
GET /api/car/options
```

`GET /api/car/{id}` 只返回未删除车型。车型不存在或已删除时返回 404；参数或评分不存在时对应字段返回 `null`，接口不触发评分重算。

`GET /api/car/brands` 返回可用品牌数组。

`GET /api/car/options` 查询可选车型：

| 参数 | 说明 |
| --- | --- |
| `keyword` | 品牌、车系、车型名称关键词，可选 |
| `limit` | 返回数量限制，可选，默认 20 |

响应示例：

```json
[
  {
    "id": 1,
    "brand": "比亚迪",
    "modelName": "宋PLUS DM-i",
    "displayName": "比亚迪 宋PLUS DM-i"
  }
]
```

## 车型参数接口

```text
GET /api/admin/cars/{id}/param
PUT /api/admin/cars/{id}/param
```

参数保存请求核心字段：

```json
{
  "carId": 1,
  "lengthMm": 4775,
  "widthMm": 1890,
  "heightMm": 1670,
  "wheelbaseMm": 2765,
  "fuelConsumption": 5.3,
  "electricConsumption": null,
  "electricRangeKm": 110,
  "totalRangeKm": 1050,
  "acceleration100": 7.9,
  "airbagCount": 6,
  "hasAbs": true,
  "hasEsp": true,
  "hasActiveBrake": true,
  "hasLaneKeep": true,
  "hasAdaptiveCruise": true,
  "hasBlindSpot": true,
  "hasReverseCamera": true,
  "has360Camera": true,
  "hasOta": true,
  "hasVoiceControl": true,
  "hasAutoParking": false,
  "screenSize": 15.6,
  "assistDriveLevel": "L2"
}
```

## 车型评分接口

```text
GET  /api/admin/cars/{id}/score
POST /api/admin/cars/{id}/score/recalculate
POST /api/admin/cars/scores/recalculate
```

评分响应核心字段：

`GET /api/admin/cars/{id}/score` 和 `POST /api/admin/cars/{id}/score/recalculate` 返回 `CarFeatureScoreVO`：

```json
{
  "carId": 1,
  "spaceScore": 88,
  "safetyScore": 92,
  "energyScore": 95,
  "intelligenceScore": 80,
  "comfortScore": 84,
  "powerScore": 78,
  "reputationScore": 92,
  "popularityScore": 86,
  "scoreVersion": "feature-score-v1",
  "calculatedTime": "2026-05-01T10:30:00"
}
```

`POST /api/admin/cars/scores/recalculate` 返回 `CarFeatureScoreBatchVO`：

```json
{
  "recalculatedCount": 120,
  "records": [
    {
      "carId": 1,
      "spaceScore": 88,
      "safetyScore": 92,
      "energyScore": 95,
      "intelligenceScore": 80,
      "comfortScore": 84,
      "powerScore": 78,
      "reputationScore": 92,
      "popularityScore": 86,
      "scoreVersion": "feature-score-v1",
      "calculatedTime": "2026-05-01T10:30:00"
    }
  ]
}
```

`CarFeatureScoreBatchVO.recalculatedCount` 表示本次重算车型数量，`records` 表示重算后的车型评分列表。

`priceScore` 不属于车型静态评分，不由上述接口返回。

## 用户需求接口

```text
POST /api/user/demand
GET  /api/user/demand/latest
GET  /api/user/demand/{id}
```

需求提交请求：

```json
{
  "userId": 1,
  "rawText": "",
  "budgetMin": 100000,
  "budgetMax": 150000,
  "bodyTypes": ["SUV", "MPV"],
  "energyTypes": ["插混", "新能源"],
  "minSeats": 5,
  "scenes": ["家庭出行", "长途自驾"],
  "factorWeights": {
    "price": 0,
    "space": 8,
    "safety": 9,
    "energy": 6,
    "intelligence": 3,
    "comfort": 7,
    "power": 0,
    "reputation": 4,
    "popularity": 0
  },
  "excludedBrands": ["特斯拉"],
  "excludedCarIds": [12]
}
```

字段规则：

| 字段 | 说明 |
| --- | --- |
| `bodyTypes` | 可接受车型类型，多选；为空表示不以车型类型作为硬过滤。 |
| `energyTypes` | 可接受动力类型，多选；包含 `新能源` 时展开为 `纯电 / 插混 / 增程`。 |
| `minSeats` | 最低座位数，作为硬约束，不参与放宽。 |
| `scenes` | 使用场景，多选；为空时按 `综合需求` 处理。 |
| `factorWeights` | 用户显式偏好权重，0-10；后端负责归一化。 |
| `budgetMax` | 预算硬上限，严格匹配时过滤 `guidePrice > budgetMax` 的车型。 |
| `budgetMin` | 预算软偏好，只参与动态价格分计算。 |
| `excludedBrands` | 排除品牌，通过 `GET /api/car/brands` 搜索选择。 |
| `excludedCarIds` | 排除车型，通过 `GET /api/car/options` 搜索选择。 |

需求响应包含 `profileText` 和归一化后的九维 `weights`。

## 自然语言解析接口

```text
POST /api/user/demand/parse-text
```

请求：

```json
{
  "userId": 1,
  "text": "我想买15万以内的SUV，家用，最好省油安全"
}
```

响应字段与需求提交请求保持一致，并额外返回：

- `profileText`
- `unsupportedTerms`
- `ambiguousTerms`
- `confidenceScore`

边界：

- 只做规则词典和正则解析。
- 不保存 `user_demand`。
- 不生成推荐。
- 不写入 `recommend_record` 或 `recommend_item`。
- 前端必须让用户确认或修改解析结果后，再提交结构化需求。

## 推荐接口

```text
POST /api/recommend/generate
GET  /api/recommend/history
GET  /api/recommend/{recordId}
GET  /api/recommend/{recordId}/algorithm-visualization
```

推荐生成请求：

```json
{
  "userId": 1,
  "demandId": 10
}
```

推荐生成请求不包含推荐数量字段。后端按候选集、分组和排序规则返回结果。

推荐响应核心结构：

```json
{
  "recordId": 100,
  "demandId": 10,
  "userId": 1,
  "profileText": "家庭和长途出行用户，预算10-15万，可接受SUV或MPV，偏好插混或新能源车型，重点关注安全、空间和舒适。",
  "algorithmVersion": "pareto-topsis-v1",
  "alpha": 0.75,
  "fallbackMessage": "已为您找到完全匹配车型",
  "recommendStatus": "SUCCESS",
  "items": [
    {
      "rankNo": 1,
      "carId": 1,
      "brand": "比亚迪",
      "series": "宋PLUS",
      "modelName": "宋PLUS DM-i",
      "guidePrice": 159800,
      "bodyType": "SUV",
      "energyType": "插混",
      "seats": 5,
      "totalScore": 88.6,
      "priceScore": 92.5,
      "spaceScore": 88,
      "safetyScore": 92,
      "energyScore": 95,
      "intelligenceScore": 80,
      "comfortScore": 84,
      "powerScore": 78,
      "reputationScore": 92,
      "popularityScore": 86,
      "matchLevel": "STRICT",
      "tags": ["空间优秀", "安全配置高", "能耗表现好"],
      "reasonText": "该车型空间表现和安全配置得分较高，符合家庭和长途出行场景。",
      "weaknessText": "该车型整体匹配较均衡，暂无明显短板。"
    }
  ],
  "createTime": "2026-05-01T10:30:00"
}
```

推荐字段规则：

- `algorithmVersion` 当前为 `pareto-topsis-v1`。
- `totalScore` 是 TOPSIS 综合推荐分，范围为 0-100。
- `rankNo` 是推荐结果展示排序的唯一权威。
- `tags` 来自推荐明细快照，不重新生成。
- `reasonText` 和 `weaknessText` 来自推荐明细快照。
- `matchLevel` 保存首次进入推荐集的匹配状态。
- 推荐历史详情读取快照，不重新计算分数、标签、理由、不足或匹配状态。

推荐历史列表：

```text
GET /api/recommend/history?userId=1&page=1&size=10
```

`userId` 为空时使用默认用户上下文。`size` 默认 10，最大 100。

历史详情：

```text
GET /api/recommend/{recordId}?userId=1
```

只返回当前用户自己的推荐记录。历史详情结构与推荐生成响应保持一致，并额外返回需求和权重信息。

算法可视化接口：

```text
GET /api/recommend/{recordId}/algorithm-visualization
```

该接口只读取推荐快照和相关车型数据，用于 `/algorithm-demo` 页面。它不重新生成推荐，不写数据库，不覆盖历史快照。

核心返回内容：

- 推荐记录、需求和用户标识。
- `algorithmVersion`、`alpha`。
- 当次用户需求和硬约束。
- 九维指标来源说明。
- `subjectiveWeight`、`objectiveWeight`、`finalWeight`。
- 候选层级统计。
- 算法流程。
- 九维评分矩阵。
- Pareto 标记和 TOPSIS 中间值。
- 推荐标签、理由、不足和快照说明。

## 车型对比接口

```text
GET /api/car/compare?carIds=1,2,3
```

规则：

- `carIds` 至少 1 个、最多 3 个。
- 重复 ID 去重。
- 只读取未删除车型。
- 只读取 `car_model`、`car_param` 和 `car_feature_score` 当前快照。
- 不触发评分重算。
- 不生成推荐记录。
- 对比维度使用八维车型静态评分；价格只作为基础信息展示。

## 收藏接口

```text
POST   /api/user/favorites/{carId}
DELETE /api/user/favorites/{carId}
GET    /api/user/favorites?page=1&size=10
GET    /api/user/favorites/status?carIds=1,2,3
```

规则：

- `userId` 可作为查询参数传入；为空时使用默认用户上下文。
- 收藏已收藏车型时幂等返回成功。
- 取消未收藏车型时幂等返回成功。
- 只允许收藏未删除车型。
- 收藏列表只返回未删除车型，并包含车型基础信息和评分摘要。
- 收藏不影响推荐排序。

## 反馈接口

```text
POST /api/recommend/{recordId}/feedback
GET  /api/recommend/{recordId}/feedback
```

提交请求：

```json
{
  "userId": 1,
  "satisfactionScore": 4,
  "reasonTags": ["推荐有帮助", "解释清楚"],
  "comment": "推荐结果比较符合家用需求"
}
```

规则：

- `userId` 为空时使用默认用户上下文。
- 只能反馈自己的推荐记录。
- `satisfactionScore` 范围为 1-5。
- `satisfactionLevel` 由后端按分数生成：4-5 为 `SATISFIED`，3 为 `NEUTRAL`，1-2 为 `DISSATISFIED`。
- `reasonTags` 保存为 JSON 数组。
- `comment` 最大 500 字。
- 同一用户同一推荐记录只保留一条反馈，重复提交会覆盖原反馈。
- 反馈不修改推荐记录、推荐明细、权重或排序。

## 管理端统计接口

```text
GET /api/admin/stat/overview
```

统计数据必须来自当前数据库，不使用随机数或前端假数据。当前总览包含：

| 字段 | 说明 |
| --- | --- |
| `budgetDistribution` | 预算区间分布。 |
| `sceneDistribution` | 使用场景分布。 |
| `focusFactorDistribution` | 关注因素 / 权重分布，对应用户显式偏好权重统计。 |
| `popularCars` | 热门推荐车型。 |
| `recommendStatusDistribution` | 推荐状态分布。 |
| `energyTypeDistribution` | 动力类型偏好分布。 |
| `bodyTypeDistribution` | 车型类型偏好分布。 |
| `satisfactionDistribution` | 满意度分布。 |
| `feedbackReasonDistribution` | 反馈原因标签分布。 |
| `feedbackCount` | 反馈数量。 |
| `averageSatisfaction` | 平均满意度。 |

其中各类图表数组使用 `{ "name": "...", "value": 0 }` 结构，便于前端图表或列表渲染。
