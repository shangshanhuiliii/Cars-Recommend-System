# API 设计

本文档只描述接口分组、请求响应约定和核心字段。数据库字段见 `DATABASE_DESIGN.md`，推荐算法见 `RECOMMENDATION_DESIGN.md`，阶段任务见 `IMPLEMENTATION_TASKS.md`。

## 1. 通用约定

### 1.1 基础路径

所有接口统一以 `/api` 开头。

### 1.2 统一响应

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 1.3 分页响应

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "size": 10
}
```

### 1.4 统一枚举字典

第一版接口只使用以下枚举，避免前端、后端、数据库和算法自由扩展导致不一致：

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

说明：

- `新能源` 只作为用户需求中的宽泛动力偏好，不作为车型表真实动力类型；需求中包含 `新能源` 时实际匹配 `纯电 / 插混 / 增程`。
- `matchLevel` 是单条推荐明细的匹配状态。
- `recommendStatus` 是一次推荐记录的总体状态。
- 需求模型重构后，用户端 API 只使用 `bodyTypes`、`energyTypes`、`scenes`、`factorWeights`。旧字段 `bodyType`、`energyType`、`scene`、`focusFactors` 不再作为请求或响应字段保留，不做长期兼容。

### 1.5 演示用户策略

第一版采用“演示用户优先”策略，不强制登录：

- 需要用户上下文的接口可以传 `userId`。
- 如果 `userId` 为空，后端使用默认演示用户 `app_user.id = 1`。
- 管理端第一版使用默认演示管理员 `admin.id = 1` 或简化入口。
- 后续若启用 JWT，再从 token 获取用户身份。
- JWT 不作为第一版推荐闭环的阻塞条件。

### 1.6 权重字段映射

API 使用 camelCase，数据库使用 snake_case，算法内部可使用 Java 风格字段名。权重字段必须按下表一一映射：

| API 字段 | 数据库字段 | 算法字段 |
| --- | --- | --- |
| `weights.price` | `weight_price` | `weightPrice` |
| `weights.space` | `weight_space` | `weightSpace` |
| `weights.safety` | `weight_safety` | `weightSafety` |
| `weights.energy` | `weight_energy` | `weightEnergy` |
| `weights.intelligence` | `weight_intelligence` | `weightIntelligence` |
| `weights.comfort` | `weight_comfort` | `weightComfort` |
| `weights.power` | `weight_power` | `weightPower` |
| `weights.reputation` | `weight_reputation` | `weightReputation` |
| `weights.popularity` | `weight_popularity` | `weightPopularity` |

## 2. 第一档接口

第一档接口必须优先实现，保证推荐闭环成立。

### 2.1 健康检查

```text
GET /api/health
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "backend": "running",
    "database": "connected"
  }
}
```

### 2.2 车型基础信息

```text
GET    /api/admin/cars
GET    /api/admin/cars/{id}
POST   /api/admin/cars
PUT    /api/admin/cars/{id}
DELETE /api/admin/cars/{id}
GET    /api/car/{id}
GET    /api/car/brands
GET    /api/car/options
```

`GET /api/car/{id}` 是用户端车型详情只读接口，只返回未删除车型。车型不存在或已删除时返回 404；参数或评分尚不存在时对应字段返回 `null`，接口不触发评分重算。

响应示例：

```json
{
  "carModel": {
    "id": 1,
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
  },
  "carParam": {
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
  },
  "carFeatureScore": {
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
}
```

分页查询参数：

| 参数 | 说明 |
| --- | --- |
| `page` | 页码 |
| `size` | 每页数量 |
| `keyword` | 品牌、车系、车型名称关键词 |
| `bodyType` | 车型类型 |
| `energyType` | 动力类型 |
| `minPrice` | 最低价 |
| `maxPrice` | 最高价 |

用户端需求页排除品牌和排除车型不允许手动输入，必须通过数据库已有数据搜索选择。

`GET /api/car/brands` 查询可用品牌：

响应示例：

```json
["比亚迪", "吉利", "丰田"]
```

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

### 2.3 车型参数

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

### 2.4 车型评分

```text
GET  /api/admin/cars/{id}/score
POST /api/admin/cars/{id}/score/recalculate
POST /api/admin/cars/scores/recalculate
```

评分响应核心字段：

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
  "scoreVersion": "v1",
  "calculatedTime": "2026-04-30 18:00:00"
}
```

### 2.5 用户需求

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

请求字段说明：

| 字段 | 说明 |
| --- | --- |
| `bodyTypes` | 可接受的车型类型，多选；为空表示不以车型类型作为硬过滤 |
| `energyTypes` | 可接受的动力类型，多选；包含 `新能源` 时展开为 `纯电 / 插混 / 增程` |
| `minSeats` | 最低座位数；作为硬约束，不参与降级放宽 |
| `scenes` | 使用场景，多选；为空时按 `综合需求` 处理 |
| `factorWeights` | 用户显式偏好权重滑块，0-10；后端负责归一化 |
| `excludedBrands` | 通过 `GET /api/car/brands` 搜索选择，不手动输入 |
| `excludedCarIds` | 通过 `GET /api/car/options` 搜索选择，不手动输入 |

预算约定：

- `budgetMax` 是预算硬上限，严格推荐阶段过滤 `guidePrice > budgetMax` 的车型。
- `budgetMin` 是预算软偏好，不作为严格过滤条件，只参与动态价格分计算。

响应应包含画像和权重：

```json
{
  "id": 10,
  "bodyTypes": ["SUV", "MPV"],
  "energyTypes": ["插混", "新能源"],
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
  "profileText": "家庭和长途出行用户，预算10-15万，可接受SUV或MPV，偏好插混或新能源车型，重点关注安全、空间和舒适。",
  "weights": {
    "price": 0.00,
    "space": 0.22,
    "safety": 0.24,
    "energy": 0.16,
    "intelligence": 0.08,
    "comfort": 0.19,
    "power": 0.00,
    "reputation": 0.11,
    "popularity": 0.00
  }
}
```

权重生成规则见 `RECOMMENDATION_DESIGN.md`。简要约定：

- `factorWeights` 至少一个值大于 0 时，后端直接归一化 `factorWeights` 作为最终 `weights`。
- `factorWeights` 全部为 0 时，后端根据 `scenes` 多场景模板平均值生成默认 `weights`。
- 旧请求字段 `bodyType`、`energyType`、`scene`、`focusFactors` 在需求模型重构后不再作为用户端标准请求字段。

### 2.6 推荐生成与追溯

```text
POST /api/recommend/generate
GET  /api/recommend/{recordId}
GET  /api/recommend/history
```

推荐生成请求：

```json
{
  "userId": 1,
  "demandId": 10
}
```

说明：用户端不展示“推荐数量”输入，也不传 `topK`。阶段 9.5 后推荐生成不再按固定 TopK 截断结果，而是返回全部符合当前推荐分组规则的候选。

推荐响应核心结构：

```json
{
  "recordId": 100,
  "demandId": 10,
  "userId": 1,
  "profileText": "家庭和长途出行用户，预算10-15万，可接受SUV或MPV，偏好插混或新能源车型，重点关注安全、空间和舒适。",
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
      "weaknessText": "当前结果为严格匹配推荐，可结合维度评分继续对比车型差异。"
    }
  ],
  "createTime": "2026-05-01T10:30:00"
}
```

其中 `tags` 由推荐算法按高分维度生成，并保存为 `recommend_item.tags` 快照。推荐历史查询时应返回保存的标签快照，不应每次查询时重新生成。标签只用于前端卡片快速展示，不替代 `reasonText` 和 `weaknessText`。

阶段 6 起推荐生成支持 `STRICT`、`RELAX_BUDGET`、`RELAX_BODY_TYPE`、`RELAX_ENERGY_TYPE`、`SIMILAR_RECOMMEND` 分级匹配。阶段 9.5 后系统不再按固定 TopK 截断结果：`STRICT` 组返回全部完全匹配候选，非 `STRICT` 组按放宽阶段补充全部推荐候选，并在每条推荐明细中保存对应 `matchLevel`，不覆盖已进入推荐集的严格匹配结果。

`recommendStatus` 生成规则：

- `SUCCESS`：最终返回结果不为空，且全部推荐项 `matchLevel = STRICT`。
- `FALLBACK`：最终返回结果不为空，且至少存在一个非 `STRICT` 推荐项。
- `EMPTY`：所有阶段都没有候选结果，`items` 为空。

`fallbackMessage` 生成规则：

- `SUCCESS`：空字符串或“已为您找到完全匹配车型”。
- `FALLBACK` 且最终结果中存在 `STRICT`：使用“完全匹配车型数量不足，系统已补充部分推荐车型”类文案。
- `FALLBACK` 且最终结果中不存在 `STRICT`：使用“未找到完全匹配车型，系统已根据核心偏好提供相近推荐”类文案。
- `EMPTY`：“暂未找到合适车型，请调整预算、车型类型或动力类型后重试。”

推荐历史详情必须返回：

- 用户画像
- 权重快照
- 降级提示
- 推荐状态
- 推荐车型
- 综合分和各维度分
- 推荐标签快照
- 推荐理由
- 不足提醒
- 匹配状态

`GET /api/recommend/history` 返回轻量历史列表，用于历史页列表展示。支持查询参数：

| 参数 | 说明 |
| --- | --- |
| `userId` | 用户 ID；为空时使用默认演示用户 `app_user.id = 1` |
| `page` | 页码，默认 1 |
| `size` | 每页数量，默认 10，最大 100 |

```json
{
  "records": [
    {
      "recordId": 100,
      "createTime": "2026-04-30 20:00:00",
      "profileText": "家庭和长途出行用户，预算10-15万，可接受SUV或MPV，偏好插混或新能源车型，重点关注安全、空间和舒适。",
      "recommendStatus": "SUCCESS",
      "fallbackMessage": "",
      "topCarNames": ["宋PLUS DM-i", "银河L7"],
      "itemCount": 10
    }
  ],
  "total": 1,
  "page": 1,
  "size": 10
}
```

`GET /api/recommend/{recordId}` 支持查询参数 `userId`，为空时使用默认演示用户 `app_user.id = 1`。接口只返回当前用户自己的推荐记录；查询其他用户的记录应返回无权限或未找到。

该接口返回完整历史详情，结构与推荐生成响应保持一致，并额外返回 `weights`、`demand` 和 `createTime`。历史详情中的 `items[].tags`、分数、理由、不足和 `matchLevel` 必须来自保存的推荐快照，不允许重新计算。

## 3. 第二档接口

### 3.1 自然语言解析

```text
POST /api/demand/parse-text
```

请求：

```json
{
  "userId": 1,
  "text": "我想买15万以内的SUV，家用，最好省油安全"
}
```

响应：

```json
{
  "userId": 1,
  "rawText": "我想买15万以内的SUV，家用，最好省油安全",
  "budgetMin": null,
  "budgetMax": 150000,
  "bodyTypes": ["SUV"],
  "energyTypes": [],
  "minSeats": null,
  "scenes": ["家庭出行"],
  "factorWeights": {
    "price": 0,
    "space": 0,
    "safety": 8,
    "energy": 8,
    "intelligence": 0,
    "comfort": 0,
    "power": 0,
    "reputation": 0,
    "popularity": 0
  },
  "excludedBrands": [],
  "excludedCarIds": [],
  "profileText": "家庭实用型用户，预算15万以内，偏好SUV，关注能耗和安全。",
  "unsupportedTerms": [],
  "ambiguousTerms": [],
  "confidenceScore": 0.86
}
```

解析结果字段应与 `POST /api/user/demand` 请求字段保持一致。前端必须先展示解析结果供用户确认修改，确认后再提交结构化需求。

### 3.2 车型对比

```text
POST /api/car/compare
```

请求：

```json
{
  "carIds": [1, 2, 3]
}
```

响应应包含基础参数、评分维度和简短结论。

### 3.3 收藏

```text
POST   /api/favorite/{carId}
DELETE /api/favorite/{carId}
GET    /api/favorite/list
```

### 3.4 反馈

```text
POST /api/feedback
```

请求：

```json
{
  "userId": 1,
  "recordId": 100,
  "satisfactionScore": 4,
  "reasonType": "价格偏高",
  "feedbackText": "推荐结果还可以，但预算稍高。"
}
```

### 3.5 后台统计

```text
GET /api/admin/stat/overview
GET /api/admin/stat/demand
GET /api/admin/stat/recommend
GET /api/admin/stat/feedback
```

阶段 9 先实现 `GET /api/admin/stat/overview` 只读总览接口，统计数据必须来自当前数据库，不使用随机数据或前端假数据。统计指标至少包括：

- 预算区间分布
- 使用场景分布
- 显式偏好权重分布
- 热门推荐车型
- 推荐状态分布
- 动力类型偏好分布
- 车型类型偏好分布
- 满意度分布，反馈表未实现前返回空数组
- 不满意原因分布，反馈表未实现前返回空数组

统计图表统一使用 `{ "name": "...", "value": 0 }` 结构，便于前端 ECharts 直接渲染：

```json
{
  "budgetDistribution": [{ "name": "10-15万", "value": 12 }],
  "sceneDistribution": [{ "name": "家庭出行", "value": 8 }],
  "factorWeightDistribution": [{ "name": "安全", "value": 10 }],
  "popularCars": [{ "name": "宋PLUS DM-i", "value": 6 }],
  "recommendStatusDistribution": [{ "name": "FALLBACK", "value": 3 }],
  "energyTypeDistribution": [{ "name": "插混", "value": 5 }],
  "bodyTypeDistribution": [{ "name": "SUV", "value": 7 }],
  "satisfactionDistribution": [],
  "feedbackReasonDistribution": []
}
```

## 4. 错误码建议

| code | 含义 |
| --- | --- |
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或 token 无效 |
| 403 | 无权限 |
| 404 | 数据不存在 |
| 500 | 系统异常 |

## 5. 实施顺序

接口实现顺序建议：

1. 健康检查
2. 车型基础信息
3. 车型参数
4. 车型评分
5. 用户需求和画像
6. 推荐生成和追溯
7. 自然语言解析
8. 对比、收藏、反馈
9. 后台统计
