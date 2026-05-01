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
| `bodyType` | `SUV` / `轿车` / `MPV` |
| `carModel.energyType` | `燃油` / `纯电` / `插混` / `增程` |
| `userDemand.energyType` | `燃油` / `纯电` / `插混` / `增程` / `新能源` |
| `scene` | `城市通勤` / `家庭出行` / `长途自驾` / `新手代步` / `商务接待` / `综合需求` |
| `focusFactors` | `价格` / `空间` / `安全` / `能耗` / `智能` / `舒适` / `动力` / `口碑` / `热度` |
| `auditStatus` | `APPROVED` / `PENDING` / `REJECTED` |
| `recommendStatus` | `SUCCESS` / `FALLBACK` / `EMPTY` |
| `matchLevel` | `STRICT` / `RELAX_BUDGET` / `RELAX_BODY_TYPE` / `RELAX_ENERGY_TYPE` / `SIMILAR_RECOMMEND` |

说明：

- `新能源` 只作为用户需求中的宽泛动力偏好，不作为车型表真实动力类型。
- `matchLevel` 是单条推荐明细的匹配状态。
- `recommendStatus` 是一次推荐记录的总体状态。

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
  "bodyType": "SUV",
  "energyType": "插混",
  "seats": 5,
  "scene": "家庭出行",
  "focusFactors": ["空间", "安全"],
  "excludedBrands": [],
  "excludedCarIds": []
}
```

预算约定：

- `budgetMax` 是预算硬上限，严格推荐阶段过滤 `guidePrice > budgetMax` 的车型。
- `budgetMin` 是预算软偏好，不作为严格过滤条件，只参与动态价格分计算。

响应应包含画像和权重：

```json
{
  "id": 10,
  "profileText": "家庭实用型用户，预算10-15万，偏好插混SUV，关注空间和安全。",
  "weights": {
    "price": 0.09,
    "space": 0.28,
    "safety": 0.28,
    "energy": 0.09,
    "intelligence": 0.07,
    "comfort": 0.12,
    "power": 0.03,
    "reputation": 0.03,
    "popularity": 0.01
  }
}
```

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
  "demandId": 10,
  "topK": 10
}
```

推荐响应核心结构：

```json
{
  "recordId": 100,
  "demandId": 10,
  "userId": 1,
  "profileText": "家庭实用型用户，预算10-15万，偏好插混SUV，关注空间和安全。",
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
      "reasonText": "该车型空间表现和安全配置得分较高，符合家庭出行场景。",
      "weaknessText": "当前结果为严格匹配推荐，可结合维度评分继续对比车型差异。"
    }
  ],
  "createTime": "2026-05-01T10:30:00"
}
```

其中 `tags` 由推荐算法按高分维度生成，并保存为 `recommend_item.tags` 快照。推荐历史查询时应返回保存的标签快照，不应每次查询时重新生成。标签只用于前端卡片快速展示，不替代 `reasonText` 和 `weaknessText`。

阶段 6 起推荐生成支持 `STRICT`、`RELAX_BUDGET`、`RELAX_BODY_TYPE`、`RELAX_ENERGY_TYPE`、`SIMILAR_RECOMMEND` 分级匹配。若严格匹配结果少于 `min(5, topK)`，系统会按阶段补充候选，并在每条推荐明细中保存对应 `matchLevel`，不覆盖已进入推荐集的严格匹配结果。

`recommendStatus` 生成规则：

- `SUCCESS`：最终返回结果不为空，且全部推荐项 `matchLevel = STRICT`。
- `FALLBACK`：最终返回结果不为空，且至少存在一个非 `STRICT` 推荐项。
- `EMPTY`：所有阶段都没有候选结果，`items` 为空。

`fallbackMessage` 生成规则：

- `STRICT`：空字符串或“已为您找到完全匹配车型”。
- `RELAX_BUDGET`：“未找到足够的完全匹配车型，系统已适度放宽预算上限。”
- `RELAX_BODY_TYPE`：“未找到足够的完全匹配车型，系统已扩展相近车型类型。”
- `RELAX_ENERGY_TYPE`：“未找到足够的完全匹配车型，系统已扩展相近动力类型。”
- `SIMILAR_RECOMMEND`：“未找到完全匹配车型，以下车型并非完全满足条件，但在核心偏好上较接近。”
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
      "profileText": "家庭实用型用户，预算10-15万，偏好插混SUV，关注空间和安全。",
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
  "bodyType": "SUV",
  "energyType": null,
  "seats": null,
  "scene": "家庭出行",
  "focusFactors": ["能耗", "安全"],
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
- 关注因素分布
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
  "focusFactorDistribution": [{ "name": "安全", "value": 10 }],
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
