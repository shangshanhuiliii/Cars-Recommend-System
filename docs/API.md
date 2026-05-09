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
| `bodyTypes[]` | `轿车` / `SUV` / `MPV` / `跑车` / `卡车` |
| `carModel.energyType` | `燃油` / `纯电` / `插混` / `增程` |
| `userDemand.energyTypes[]` | `燃油` / `纯电` / `插混` / `增程` / `新能源` |
| `seatOptions[]` | `2` / `4` / `5` / `6` / `7` / `7_PLUS` |
| `scenes[]` | `城市通勤` / `家庭出行` / `长途自驾` / `新手代步` / `商务接待` / `接送孩子` / `露营旅行` / `年轻运动` / `豪华舒适` / `低成本通勤` / `科技智能` / `综合需求` |
| `factorWeights` keys | `price` / `space` / `safety` / `energy` / `intelligence` / `comfort` / `power` / `reputation` / `popularity` |
| `auditStatus` | `APPROVED` / `PENDING` / `REJECTED` |
| `recommendStatus` | `SUCCESS` / `FALLBACK` / `EMPTY` |
| `matchLevel` | `STRICT` / `RELAX_BUDGET` / `RELAX_BODY_TYPE` / `RELAX_ENERGY_TYPE` / `SIMILAR_RECOMMEND` |
| `satisfactionLevel` | `SATISFIED` / `NEUTRAL` / `DISSATISFIED` |
| `appUser.status` | `ACTIVE` / `DISABLED` |

`新能源` 只作为用户需求中的宽泛动力偏好。后端匹配时展开为 `纯电 / 插混 / 增程`，车型表不保存该值。

## 登录认证与权限

当前基础认证能力已经实现。除公开接口外，请求必须携带：

```text
Authorization: Bearer <token>
```

角色边界：

| 角色 | 身份来源 | 说明 |
| --- | --- | --- |
| `USER` | `app_user` 登录 token | 用户端需求、推荐、历史、收藏、反馈和用户级车型对比。 |
| `ADMIN` | `admin` 登录 token，且 `admin.role = ADMIN` | 管理端车型、图片、用户管理、收藏车型、反馈记录、推荐记录、运营概览、健康检查和算法可视化。 |

公开接口：

- `GET /api/health`
- `POST /api/auth/login`
- `POST /api/auth/user/login`
- `POST /api/auth/user/register`
- `POST /api/auth/admin/login`
- `GET /api/car/**`
- `/uploads/**` 静态图片资源

`OPTIONS` 请求放行，用于 CORS 预检。无 token 或 token 过期、签名错误返回 `401`；角色不匹配返回 `403`。前端菜单隐藏不是安全边界，后端拦截器才是接口权限边界。

认证拦截器采用 fail-closed 策略。新增 `/api/**` 接口必须显式归类为公开、`USER` 或 `ADMIN`；未归类的 `/api/**` 不默认放行。未携带 token 访问未归类 API 返回 `401`，携带有效 token 访问未归类 API 返回 `404`。

本地 seed 默认账号仅用于本地开发登录，不再作为接口默认身份来源：

| 类型 | 用户名 | 密码 |
| --- | --- | --- |
| 普通用户 | `demo_user` | `demo123456` |
| 管理员 | `demo_admin` | `admin123456` |

## 认证接口

统一登录：

```text
POST /api/auth/login
```

该接口供产品前端统一登录弹窗使用。后端按 `username` 同时查询普通用户和管理员，分别用现有密码哈希校验：

- 只有普通用户匹配时，返回 `principalType = USER` 的 `AuthTokenVO`。
- 只有管理员匹配时，返回 `principalType = ADMIN` 的 `AuthTokenVO`。
- 用户名或密码不匹配、普通用户被禁用时返回 `401`。
- 如果普通用户和管理员同时匹配，返回明确错误，提示账号冲突，避免无角色选择时误判身份。

普通用户登录：

```text
POST /api/auth/user/login
```

管理员登录：

```text
POST /api/auth/admin/login
```

旧分角色登录接口保留兼容和测试使用；产品前端统一调用 `POST /api/auth/login`，不再提供独立 `/login` 与 `/admin/login` 产品页面。

请求：

```json
{
  "username": "demo_user",
  "password": "demo123456"
}
```

响应 `data`：

```json
{
  "token": "...",
  "tokenType": "Bearer",
  "expiresAt": "2026-05-08 12:00:00",
  "principal": {
    "id": 1,
    "username": "demo_user",
    "displayName": "演示用户",
    "principalType": "USER",
    "role": "USER",
    "permissions": ["user:demand", "user:recommend"],
    "menus": [
      { "code": "home", "label": "Home", "path": "/" },
      { "code": "recommend", "label": "Recommendation", "path": "/recommend" }
    ]
  }
}
```

普通用户注册：

```text
POST /api/auth/user/register
```

请求：

```json
{
  "username": "new_user",
  "password": "User123456",
  "confirmPassword": "User123456",
  "nickname": "新用户",
  "phone": "13800000000"
}
```

字段规则：

- `username` 必填，trim 后 4-32 位，只允许字母、数字、下划线，且不能与已有普通用户或管理员用户名冲突。
- `password` 必填，8-32 位，至少包含字母和数字。
- `confirmPassword` 必须与 `password` 一致。
- `nickname` 可选，最多 32 个字符；为空时默认使用用户名。
- `phone` 可选，当前仅做格式校验，不强制唯一。

注册成功返回与登录相同的 `AuthTokenVO`，`principalType = USER`；注册接口只能创建普通 `app_user`，不能创建管理员账号。重复用户名或与管理员用户名冲突返回 `400`，禁用用户再次登录返回 `401`。

当前登录身份：

```text
GET /api/auth/me
```

返回当前 principal、permissions 和 menus。

退出登录：

```text
POST /api/auth/logout
```

后端不维护 token 黑名单，该接口用于前端清理型退出；前端仍需删除本地 token。
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

`imageUrl` 是当前生效图片地址。管理端图片资源上传后默认不覆盖该字段，只有图片资源审核通过后才会由后端更新。

用户端车型只读接口：

```text
GET /api/car/{id}
GET /api/car/brands
GET /api/car/options
GET /api/car/home-carousel?limit=6
```

`GET /api/car/{id}` 只返回未删除车型。车型不存在或已删除时返回 404；参数或评分不存在时对应字段返回 `null`，接口不触发评分重算。

`GET /api/car/brands` 返回可用品牌数组。

`GET /api/car/home-carousel?limit=6` 是首页公开车辆轮播接口，默认返回 6 条，`limit` 允许范围为 3-12，非法值返回 400。接口只读，只查询 `deleted = FALSE` 且 `audit_status = 'APPROVED'` 的车型；优先随机返回 `imageUrl` 非空车型，有图车型不足时再随机补齐其他审核通过车型。该接口不写数据库、不触发车型评分重算、不保存用户需求、不生成推荐记录。

响应字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 车型 ID |
| `brand` | 品牌 |
| `series` | 车系 |
| `modelName` | 车型名称 |
| `guidePrice` | 指导价，单位元 |
| `bodyType` | 车型类型 |
| `energyType` | 动力类型 |
| `seats` | 座位数 |
| `imageUrl` | 当前生效图片地址，可为空，前端用本地兜底图展示 |

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

## 管理端车型图片资源接口

图片资源用于管理车型图片的上传、压缩、审核和绑定。当前默认使用本地文件系统存储，公开访问路径为：

```text
/uploads/car-images/{storedFilename}
```

资源状态：

| 状态 | 说明 |
| --- | --- |
| `PENDING` | 上传成功后的默认状态，可预览但不会覆盖车型当前图片。 |
| `APPROVED` | 审核通过，后端将对应 `car_model.image_url` 更新为资源 `publicUrl`。 |
| `REJECTED` | 审核拒绝，保存 `rejectReason`，不更新车型当前图片。 |

资源响应核心字段：

```json
{
  "id": 1,
  "carId": 2,
  "originalFilename": "song-plus.png",
  "storedFilename": "car-2-uuid.png",
  "contentType": "image/png",
  "sizeBytes": 126000,
  "width": 1600,
  "height": 900,
  "publicUrl": "/uploads/car-images/car-2-uuid.png",
  "storagePath": "car-2-uuid.png",
  "checksum": "sha256hex",
  "auditStatus": "PENDING",
  "rejectReason": null,
  "createdByAdminId": 1,
  "reviewedByAdminId": null,
  "createTime": "2026-05-07T10:30:00",
  "updateTime": "2026-05-07T10:30:00",
  "reviewTime": null
}
```

上传车型图片：

```text
POST /api/admin/car-images
Content-Type: multipart/form-data
```

表单字段：

| 字段 | 说明 |
| --- | --- |
| `file` | 必填，JPEG / PNG 图片。业务限制 5MB。 |
| `carId` | 必填，绑定车型 ID。 |

规则：

- 后端校验图片真实内容，不只信任扩展名或请求 `contentType`。
- 文件名由后端重新生成，原始文件名只作为展示字段保存。
- 上传时执行压缩/缩放，最长边不超过 1600px；JPEG 使用质量参数压缩，PNG 至少执行尺寸缩放。
- 上传成功后资源状态为 `PENDING`，返回元数据和 `publicUrl`，不覆盖 `car_model.image_url`。

分页查询图片资源：

```text
GET /api/admin/car-images?page=1&size=10&carId=&auditStatus=
```

查询参数：

| 参数 | 说明 |
| --- | --- |
| `page` | 页码，默认 1。 |
| `size` | 每页数量，默认 10，最大 100。 |
| `carId` | 按车型过滤，可选。 |
| `auditStatus` | 按 `PENDING / APPROVED / REJECTED` 过滤，可选。 |

审核图片资源：

```text
PUT /api/admin/car-images/{id}/audit
```

请求：

```json
{
  "auditStatus": "APPROVED",
  "rejectReason": ""
}
```

规则：

- 只审核 `PENDING` 资源。
- `auditStatus = APPROVED` 时更新资源状态，并将对应车型 `car_model.image_url` 更新为资源 `publicUrl`。
- `auditStatus = REJECTED` 时需要填写 `rejectReason`，不更新车型当前图片。
- 审核不触发车型评分重算，不影响推荐排序或历史推荐快照。

删除图片资源：

```text
DELETE /api/admin/car-images/{id}
```

规则：

- 删除为软删除，只更新 `deleted`。
- 不物理删除上传文件。
- 如果已通过资源正被对应车型 `image_url` 使用，接口返回清晰错误，车型图片保持不变。

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
  "budgetMin": 100000,
  "budgetMax": null,
  "brands": ["BYD", "Toyota"],
  "bodyTypes": ["SUV"],
  "energyTypes": ["插混", "新能源"],
  "seatOptions": ["5", "7_PLUS"],
  "scenes": ["家庭出行", "科技智能"],
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
  }
}
```

字段规则：

| 字段 | 说明 |
| --- | --- |
| `budgetMin` | 预算下限，单位元；为空表示不限制下限。 |
| `budgetMax` | 预算上限，单位元；为空表示不限上限。 |
| `brands` | 正向品牌筛选，多选；为空表示全部品牌。 |
| `bodyTypes` | 可接受车型类型，多选；为空表示全部级别。 |
| `energyTypes` | 可接受动力类型，多选；为空表示全部动力；包含 `新能源` 时展开为 `纯电 / 插混 / 增程`。 |
| `seatOptions` | 座位选项，多选；为空表示全部座位。`7_PLUS` 表示 `seats >= 7`。 |
| `scenes` | 使用场景，多选；为空时按 `综合需求` 处理。 |
| `factorWeights` | 用户显式偏好权重，0-10；全为 0 时使用场景模板，后端负责归一化。 |
| `minSeats` | 兼容旧字段；当 `seatOptions` 为空时才作为最低座位数硬约束。 |
| `excludedBrands` | 兼容旧字段；当前产品前端不展示排除品牌入口。 |
| `excludedCarIds` | 兼容旧字段；当前产品前端不展示排除车型入口。 |

需求接口需要 `USER` 权限。`userId` 始终来自 JWT 当前用户；即使请求体或查询参数传入 `userId`，后端也不会用它覆盖当前登录身份。需求详情只返回当前用户自己的需求，不属于当前用户时返回 `404`。

需求响应包含 `brands`、`seatOptions`、`profileText` 和归一化后的九维 `weights`。

## 自然语言解析接口

```text
POST /api/user/demand/parse-text
```

请求：

```json
{
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
- 当前产品前端不展示该入口；主推荐流程只通过结构化需求表单提交。

## 推荐接口

```text
POST /api/recommend/generate
GET  /api/recommend/history
GET  /api/recommend/{recordId}
```

推荐生成请求：

```json
{
  "demandId": 10
}
```

推荐接口需要 `USER` 权限。推荐生成请求不包含推荐数量字段，`userId` 来自 JWT 当前用户，`demandId` 必须属于当前用户。后端按候选集、分组和排序规则返回结果。

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
      "imageUrl": "/uploads/car-images/car-1-demo.png",
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
- `matchLevel = STRICT` 表示车型满足预算区间、车型类型、动力类型、最低座位数和排除条件等严格条件；预算区间外但接近预算的车型只能进入 `RELAX_BUDGET` 等推荐组。
- 推荐历史详情读取快照，不重新计算分数、标签、理由、不足或匹配状态。

推荐历史列表：

```text
GET /api/recommend/history?page=1&size=10
```

返回当前登录用户自己的推荐历史。`size` 默认 10，最大 100。

历史详情：

```text
GET /api/recommend/{recordId}
```

只返回当前用户自己的推荐记录。历史详情结构与推荐生成响应保持一致，并额外返回需求和权重信息。

算法可视化接口属于 `ADMIN` 权限：

```text
GET /api/recommend/{recordId}/algorithm-visualization
GET /api/admin/recommend-records/{recordId}/algorithm-visualization
```

推荐使用更清晰的管理端路径。该接口只读取推荐快照和相关车型数据，用于 `/algorithm-demo` 页面。它不重新生成推荐，不写数据库，不覆盖历史快照。

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

用户端当前使用用户级持久化对比列表：

```text
GET    /api/user/compare
POST   /api/user/compare/{carId}
DELETE /api/user/compare/{carId}
DELETE /api/user/compare
```

规则：

- 需要 `USER` 权限，`userId` 始终来自 JWT 当前用户，不接受 `userId` 参数。
- 管理员不能访问用户级车型对比接口。
- 每个用户最多保存 3 辆对比车型。
- 重复加入同一车型保持幂等，并确保该记录为未删除状态。
- 移除和清空对比项按当前用户软删除，不能影响其他用户。
- 查询返回当前用户保存的车型详情，按 `sort_no ASC, update_time ASC, create_time ASC` 等稳定顺序展示。
- 对比只读取 `car_model`、`car_param` 和 `car_feature_score` 当前快照，不触发评分重算，不生成推荐记录，不影响推荐排序。

旧静态对比查询继续保留：

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
- 该接口不保存用户选择，用户端“加入对比”和 `/compare` 页面应使用 `/api/user/compare`。

## 收藏接口

```text
POST   /api/user/favorites/{carId}
DELETE /api/user/favorites/{carId}
GET    /api/user/favorites?page=1&size=10
GET    /api/user/favorites/status?carIds=1,2,3
```

规则：

- 需要 `USER` 权限，`userId` 来自 JWT 当前用户。
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
  "satisfactionScore": 4,
  "reasonTags": ["推荐有帮助", "解释清楚"],
  "comment": "推荐结果比较符合家用需求"
}
```

规则：

- 需要 `USER` 权限，`userId` 来自 JWT 当前用户。
- 只能反馈自己的推荐记录。
- `satisfactionScore` 范围为 1-5。
- `satisfactionLevel` 由后端按分数生成：4-5 为 `SATISFIED`，3 为 `NEUTRAL`，1-2 为 `DISSATISFIED`。
- `reasonTags` 保存为 JSON 数组。
- `comment` 最大 500 字。
- 同一用户同一推荐记录只保留一条反馈，重复提交会覆盖原反馈。
- 反馈不修改推荐记录、推荐明细、权重或排序。

## 管理端用户管理接口

以下接口均需要 `ADMIN` token。管理端用户管理只管理 `app_user`，不管理 `admin`，也不会触发推荐生成或反馈学习。

分页查询普通用户：

```text
GET /api/admin/users?page=1&size=10&keyword=&status=
```

返回 `PageResult<AdminUserListItemVO>`，列表项包含 `id`、`username`、`nickname`、`phone`、`status`、`deleted`、`recommendRecordCount`、`favoriteCount`、`feedbackCount`、`createTime`、`updateTime`。

用户详情：

```text
GET /api/admin/users/{userId}
```

返回用户基础信息、`summary` 计数、`latestDemand` 和 `recentRecommendRecords`。收藏车型和反馈记录已拆分到独立只读管理页面，用户详情只保留统计数字和跳转入口。

按用户查看推荐历史：

```text
GET /api/admin/users/{userId}/recommend-records?page=1&size=10
```

启用 / 禁用普通用户：

```text
PUT /api/admin/users/{userId}/status
```

请求：

```json
{
  "status": "DISABLED"
}
```

`status` 只能为 `ACTIVE` 或 `DISABLED`。禁用后用户不能重新登录；当前轻量 JWT 不维护服务端黑名单，已签发 token 在过期前仍可能有效。

## 管理端收藏车型接口

以下接口均需要 `ADMIN` token，只读查看收藏统计，不提供取消收藏、删除收藏或代用户操作能力。

```text
GET /api/admin/favorites/cars?page=1&size=10&keyword=&userId=
GET /api/admin/favorites/cars/{carId}/users?page=1&size=10
```

`GET /api/admin/favorites/cars` 按车型聚合收藏数，并按 `favoriteCount DESC` 排序。`keyword` 可按品牌、车系、车型名筛选；`userId` 用于从用户管理跳转后筛选该用户收藏过的车型，但返回的 `favoriteCount` 仍是全站收藏总数。

返回字段包括 `carId`、`brand`、`series`、`modelName`、`guidePrice`、`bodyType`、`energyType`、`imageUrl`、`favoriteCount`、`latestFavoriteTime`。

`GET /api/admin/favorites/cars/{carId}/users` 返回收藏该车型的用户，包括 `userId`、`username`、`nickname`、`phone`、`status`、`favoriteTime`。

## 管理端反馈记录接口

该接口需要 `ADMIN` token，只读查看反馈记录，不提供删除、修改或反馈学习能力。

```text
GET /api/admin/feedbacks?page=1&size=10&keyword=&userId=&satisfactionScore=
```

筛选规则：

- `keyword`：用户名、昵称、评论内容模糊搜索。
- `userId`：筛选指定用户反馈记录。
- `satisfactionScore`：按 1-5 满意度评分筛选。

返回字段包括 `feedbackId`、`userId`、`username`、`nickname`、`recordId`、`satisfactionScore`、`satisfactionLevel`、`reasonTags`、`comment`、`createTime`、`updateTime`。反馈只进入统计，不修改推荐记录、推荐明细、权重或排序。

## 管理端推荐记录接口

```text
GET /api/admin/recommend-records?page=1&size=10&userId=
GET /api/admin/recommend-records/{recordId}
GET /api/admin/recommend-records/{recordId}/algorithm-visualization
```

这些接口需要 `ADMIN` 权限。管理员可以查看全量推荐记录，也可以通过 `userId` 过滤某个用户的推荐记录。普通用户不能通过用户端推荐历史接口查看他人记录。

## 管理端运营概览接口

```text
GET /api/admin/stat/overview
```

该接口需要 `ADMIN` 权限。统计数据必须来自当前数据库，不使用随机数或前端假数据。当前总览用于管理端“运营概览”页面，不包装为实时 BI 系统。当前总览包含：

| 字段 | 说明 |
| --- | --- |
| `userCount` | 用户总数。 |
| `activeUserCount` | ACTIVE 用户数。 |
| `disabledUserCount` | DISABLED 用户数。 |
| `carCount` | 车型总数。 |
| `recommendRecordCount` | 推荐记录总数。 |
| `todayRecommendRecordCount` | 今日推荐记录数。 |
| `recentRecommendRecordCount` | 最近 7 天推荐记录数。 |
| `favoriteCount` | 收藏总数。 |
| `budgetDistribution` | 预算区间分布。 |
| `sceneDistribution` | 使用场景分布。 |
| `focusFactorDistribution` | 关注因素 / 权重分布，对应用户显式偏好权重统计。 |
| `popularCars` | 热门推荐车型。 |
| `recommendStatusDistribution` | 推荐状态分布，展示名中文化为完全匹配、含补充推荐、暂无结果。 |
| `energyTypeDistribution` | 动力类型偏好分布。 |
| `bodyTypeDistribution` | 车型类型偏好分布。 |
| `favoriteTopCars` | 收藏最多车型。 |
| `satisfactionDistribution` | 满意度分布。 |
| `feedbackReasonDistribution` | 反馈原因标签分布。 |
| `feedbackCount` | 反馈数量。 |
| `averageSatisfaction` | 平均满意度。 |

其中各类图表数组使用 `{ "name": "...", "value": 0 }` 结构，便于前端图表或列表渲染。
