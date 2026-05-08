# 数据库文档

本文档描述当前 MySQL 数据库表职责、关键字段、推荐快照、收藏反馈和本地初始化方式。真实结构以 `backend/src/main/resources/db/schema.sql` 为准，种子数据以 `backend/src/main/resources/db/seed-data.sql` 为准。

## 设计原则

- 数据库设计服务于推荐主链路。
- 所有价格统一使用元，尺寸统一使用毫米，续航统一使用公里。
- 软删除字段统一使用 `deleted`。
- 时间字段统一使用 `create_time`、`update_time`。
- `priceScore` 不写入 `car_feature_score`，只在推荐生成时动态计算。
- 推荐历史必须读取快照，不重新计算覆盖。
- 真实 MySQL 的迁移、重建、清空或删除数据必须先获得明确确认。

## 表总览

| 表名 | 职责 |
| --- | --- |
| `app_user` | 普通用户账户，关联需求、推荐历史、收藏和反馈。 |
| `admin` | 管理员账户。 |
| `car_model` | 车型基础展示信息和基础过滤字段。 |
| `car_image_asset` | 车型图片上传资源、压缩后文件元数据和审核状态。 |
| `car_param` | 车型参数和配置，作为车型评分来源。 |
| `car_feature_score` | 八维车型静态评分。 |
| `user_demand` | 用户购车需求、画像文本和主观权重。 |
| `recommend_record` | 一次推荐任务的总体快照。 |
| `recommend_item` | 推荐结果明细、分数、排序和解释快照。 |
| `user_compare_car` | 用户级车型对比列表，保存当前登录 USER 的对比选择。 |
| `user_favorite` | 用户收藏车型。 |
| `recommend_feedback` | 用户对推荐记录的反馈。 |

## 账户表

### `app_user`

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `username` | 用户名，唯一 |
| `password` | PBKDF2 密码哈希，格式为 `pbkdf2$iterations$base64Salt$base64Hash` |
| `nickname` | 昵称 |
| `phone` | 手机号 |
| `status` | 账号状态：`ACTIVE` / `DISABLED` |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

本地 seed 默认用户账号：

```text
username = demo_user
password = demo123456
```

`app_user.id = 1` 只表示本地 seed 账号主键，不再作为接口默认身份来源。用户端接口身份必须来自 JWT。

`deleted` 用于软删除；`status` 用于账号启用 / 禁用。普通用户注册时写入 `ACTIVE`，登录只允许 `status = ACTIVE` 且 `deleted = FALSE` 的账号。管理员禁用用户不会删除其需求、推荐记录、收藏或反馈。

### `admin`

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `username` | 管理员用户名，唯一 |
| `password` | PBKDF2 密码哈希，格式为 `pbkdf2$iterations$base64Salt$base64Hash` |
| `role` | 角色，当前默认为 `ADMIN` |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

本地 seed 默认管理员账号：

```text
username = demo_admin
password = admin123456
role = ADMIN
```

`admin.id = 1` 只表示本地 seed 管理员主键，不再作为管理端接口默认身份来源。管理端写入字段必须来自 JWT 当前管理员。

## 车型数据表

### `car_model`

用于列表、详情、推荐展示和基础过滤。

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `brand` | 品牌 |
| `series` | 车系 |
| `model_name` | 车型名称 |
| `guide_price` | 指导价，单位元 |
| `body_type` | 车型类型：`轿车` / `SUV` / `MPV` / `跑车` / `卡车` |
| `energy_type` | 动力类型：`燃油` / `纯电` / `插混` / `增程` |
| `seats` | 座位数 |
| `launch_year` | 上市年份 |
| `image_url` | 图片地址 |
| `sales_volume` | 销量或测试热度 |
| `user_rating` | 口碑评分，0-5 |
| `audit_status` | `APPROVED` / `PENDING` / `REJECTED` |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

`car_model.image_url` 是当前生效图片地址。管理端图片资源上传后先保存到 `car_image_asset`，只有审核通过时才会更新该字段。

`car_model.energy_type` 不保存 `新能源`。用户需求侧选择 `新能源` 时，由后端展开为 `纯电 / 插混 / 增程`。

### `car_image_asset`

保存车型图片资源的上传、压缩、访问地址和审核状态。该表只增强车型展示资源，不参与推荐评分、候选生成、排序或历史快照重算。

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `car_id` | 绑定车型 ID |
| `original_filename` | 用户上传时的原始文件名，仅用于展示 |
| `stored_filename` | 后端重新生成的存储文件名，唯一 |
| `content_type` | `image/jpeg` / `image/png` |
| `size_bytes` | 压缩/缩放后文件大小 |
| `width` | 压缩/缩放后图片宽度 |
| `height` | 压缩/缩放后图片高度 |
| `public_url` | 静态访问路径，例如 `/uploads/car-images/{storedFilename}` |
| `storage_path` | 存储根目录下的相对路径 |
| `checksum` | 压缩/缩放后文件的 SHA-256 |
| `audit_status` | `PENDING` / `APPROVED` / `REJECTED` |
| `reject_reason` | 拒绝原因 |
| `created_by_admin_id` | 上传管理员，来自 JWT 当前管理员；认证关闭的测试模式才回退本地 seed 管理员 |
| `reviewed_by_admin_id` | 审核管理员，来自 JWT 当前管理员；认证关闭的测试模式才回退本地 seed 管理员 |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |
| `review_time` | 审核时间 |

规则：

- 上传成功后默认 `audit_status = PENDING`，不会覆盖 `car_model.image_url`。
- 审核通过后资源状态改为 `APPROVED`，并将对应车型 `car_model.image_url` 更新为该资源 `public_url`。
- 审核拒绝后资源状态改为 `REJECTED`，保存 `reject_reason`，车型当前图片保持不变。
- 删除资源只更新 `deleted`，不物理删除文件；当前被 `car_model.image_url` 使用的已通过资源不能直接删除。

### `car_param`

用于车型评分规则计算。

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `car_id` | 车型 ID，唯一 |
| `length_mm` | 车长 |
| `width_mm` | 车宽 |
| `height_mm` | 车高 |
| `wheelbase_mm` | 轴距 |
| `fuel_consumption` | 油耗 L/100km |
| `electric_consumption` | 电耗 kWh/100km |
| `electric_range_km` | 纯电续航 |
| `total_range_km` | 综合续航 |
| `acceleration_100` | 百公里加速 |
| `airbag_count` | 安全气囊数量 |
| `has_abs` | 是否有 ABS |
| `has_esp` | 是否有 ESP |
| `has_active_brake` | 是否有主动刹车 |
| `has_lane_keep` | 是否有车道保持 |
| `has_adaptive_cruise` | 是否有自适应巡航 |
| `has_blind_spot` | 是否有并线辅助 |
| `has_reverse_camera` | 是否有倒车影像 |
| `has_360_camera` | 是否有 360 全景影像 |
| `has_ota` | 是否支持 OTA |
| `has_voice_control` | 是否有语音交互 |
| `has_auto_parking` | 是否有自动泊车 |
| `screen_size` | 中控屏尺寸 |
| `assist_drive_level` | 辅助驾驶等级 |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

### `car_feature_score`

由 `car_model` 和 `car_param` 计算生成。

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `car_id` | 车型 ID，唯一 |
| `space_score` | 空间分 |
| `safety_score` | 安全分 |
| `energy_score` | 能耗分 |
| `intelligence_score` | 智能分 |
| `comfort_score` | 舒适分 |
| `power_score` | 动力分 |
| `reputation_score` | 口碑分 |
| `popularity_score` | 热度分 |
| `score_version` | 评分规则版本 |
| `calculated_time` | 计算时间 |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

评分范围为 0-100。`popularity_score` 依赖全局销量最大值，修改 `sales_volume` 后建议执行全部车型评分重算。

## 用户需求表

### `user_demand`

保存结构化需求、自然语言原文、画像文本和主观权重。

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `user_id` | 用户 ID；用户端接口由 JWT 当前 `USER` 写入 |
| `raw_text` | 自然语言原始输入，可空 |
| `budget_min` | 预算下限，单位元 |
| `budget_max` | 预算上限，单位元 |
| `brands` | 正向品牌筛选，JSON 数组 |
| `body_types` | 车型类型偏好，JSON 数组 |
| `energy_types` | 动力类型偏好，JSON 数组 |
| `seat_options` | 座位数选项，JSON 数组，支持 `2/4/5/6/7/7_PLUS` |
| `min_seats` | 最低座位数 |
| `scenes` | 使用场景，JSON 数组 |
| `factor_weights` | 九维显式偏好权重，JSON 对象，值域 0-10 |
| `excluded_brands` | 排除品牌，JSON 数组 |
| `excluded_car_ids` | 排除车型 ID，JSON 数组 |
| `profile_text` | 用户画像文本 |
| `weight_price` | 价格权重 |
| `weight_space` | 空间权重 |
| `weight_safety` | 安全权重 |
| `weight_energy` | 能耗权重 |
| `weight_intelligence` | 智能权重 |
| `weight_comfort` | 舒适权重 |
| `weight_power` | 动力权重 |
| `weight_reputation` | 口碑权重 |
| `weight_popularity` | 热度权重 |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

当前用户需求 API 字段固定为：

- `budgetMin`
- `budgetMax`
- `brands`
- `bodyTypes`
- `energyTypes`
- `seatOptions`
- `scenes`
- `factorWeights`

`minSeats`、`excludedBrands`、`excludedCarIds` 作为兼容字段保留；当前产品前端以 `brands` 正向筛选和 `seatOptions` 座位选项为主，不再展示排除品牌或排除车型入口。

如果数据库使用 JSON 类型，应用层应按数组或对象处理；API 不返回 JSON 字符串。

## 推荐快照表

### `recommend_record`

保存一次推荐任务的总体快照。

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `user_id` | 用户 ID |
| `demand_id` | 用户需求 ID |
| `profile_text` | 推荐时的画像文本快照 |
| `weight_snapshot` | 权重快照，JSON |
| `fallback_message` | 补充推荐提示 |
| `recommend_status` | `SUCCESS` / `FALLBACK` / `EMPTY` |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

`weight_snapshot` 当前包含：

- `algorithmVersion`
- `alpha`
- `subjectiveWeight`
- `objectiveWeight`
- `finalWeight`

### `recommend_item`

保存每个推荐结果的分数、排序和解释依据。

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `record_id` | 推荐记录 ID |
| `car_id` | 车型 ID |
| `rank_no` | 展示排名权威快照 |
| `total_score` | TOPSIS 综合推荐分 |
| `price_score` | 动态价格分 |
| `space_score` | 空间分快照 |
| `safety_score` | 安全分快照 |
| `energy_score` | 能耗分快照 |
| `intelligence_score` | 智能分快照 |
| `comfort_score` | 舒适分快照 |
| `power_score` | 动力分快照 |
| `reputation_score` | 口碑分快照 |
| `popularity_score` | 热度分快照 |
| `tags` | 推荐标签快照，JSON 数组 |
| `match_level` | 匹配状态 |
| `reason_text` | 推荐理由 |
| `weakness_text` | 不足提醒 |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

历史详情必须读取上述快照，不重新计算 `totalScore`、`rankNo`、`tags`、`reason_text`、`weakness_text` 或 `match_level`。

## 用户对比、收藏和反馈表

### `user_compare_car`

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `user_id` | 用户 ID，来自 JWT 当前 USER |
| `car_id` | 对比车型 ID |
| `sort_no` | 展示顺序，当前默认按加入顺序使用 |
| `deleted` | 软删除；移除对比不物理删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

约束：

- `user_id + car_id` 唯一。
- 每个用户当前最多 3 条未删除对比车型。
- 重复加入同一车型保持幂等并恢复 `deleted = FALSE`。
- 用户 A / 用户 B 的对比列表通过 `user_id` 隔离，不接受接口参数覆盖当前用户。
- 管理员不使用该表对应的用户级对比接口。
- 对比不参与推荐排序，不写入推荐权重。

### `user_favorite`

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `user_id` | 用户 ID |
| `car_id` | 收藏车型 ID |
| `deleted` | 软删除；取消收藏不物理删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

约束：

- `user_id + car_id` 唯一。
- 重复收藏保持幂等。
- 收藏不参与推荐排序，不写入推荐权重。

### `recommend_feedback`

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `user_id` | 用户 ID |
| `record_id` | 推荐记录 ID |
| `satisfaction_score` | 满意度 1-5 |
| `satisfaction_level` | `SATISFIED` / `NEUTRAL` / `DISSATISFIED` |
| `reason_tags` | 原因标签 JSON 数组 |
| `comment` | 文字反馈，最长 500 字 |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

约束：

- `user_id + record_id` 唯一。
- 重复提交反馈按覆盖处理。
- 反馈只进入统计，不自动修改用户权重、车型评分、推荐记录或推荐明细。

## 数据关系

```text
app_user 1 - n user_demand
user_demand 1 - n recommend_record
recommend_record 1 - n recommend_item
car_model 1 - 1 car_param
car_model 1 - 1 car_feature_score
car_model 1 - n car_image_asset
car_model 1 - n recommend_item
recommend_record 1 - n recommend_feedback
app_user 1 - n user_compare_car
car_model 1 - n user_compare_car
app_user 1 - n user_favorite
car_model 1 - n user_favorite
```

## 初始化方式

本地初始化脚本：

```text
scripts/init-dev-db.ps1
```

脚本读取：

```text
backend/src/main/resources/application-local.yml
```

并依次执行：

```text
backend/src/main/resources/db/schema.sql
backend/src/main/resources/db/seed-data.sql
```

初始化命令：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1
```

重建本地开发库：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -Recreate
```

`-Recreate` 会要求输入数据库名确认后才删除并重建。只对自己的本地开发库使用。

## 种子数据说明

`seed-data.sql` 当前提供：

- 本地默认普通用户 `demo_user / demo123456`，密码以 PBKDF2 hash 保存。
- 本地默认管理员 `demo_admin / admin123456`，`role = ADMIN`，密码以 PBKDF2 hash 保存。
- 120 条车型基础数据。
- 120 条车型参数数据。

种子数据不预置：

- 车型评分。
- 推荐记录。
- 推荐明细。
- 图片资源。
- 收藏。
- 反馈。

导入后必须执行全部车型评分重算，生成 `car_feature_score`：

```text
POST /api/admin/cars/scores/recalculate
```

初始化完成但未重算评分时，预期：

```text
app_user = 1
admin = 1
car_model = 120
car_param = 120
car_feature_score = 0
```

评分重算后：

```text
car_feature_score = 120
```

## 索引和约束

当前脚本包含常用索引：

- `car_model(brand)`
- `car_model(body_type)`
- `car_model(energy_type)`
- `car_model(guide_price)`
- `car_image_asset(stored_filename)` 唯一约束
- `car_image_asset(car_id)`
- `car_image_asset(audit_status)`
- `car_image_asset(car_id, audit_status)`
- `car_param(car_id)` 唯一约束
- `car_feature_score(car_id)` 唯一约束
- `user_demand(user_id)`
- `recommend_record(user_id)`
- `recommend_record(demand_id)`
- `recommend_item(record_id)`
- `recommend_item(car_id)`
- `user_compare_car(user_id, car_id)` 唯一约束
- `user_compare_car(user_id, deleted, sort_no, update_time)` 查询索引
- `user_favorite(user_id, car_id)` 唯一约束
- `recommend_feedback(user_id, record_id)` 唯一约束

扩充车型数据时应保持字段常识和类型覆盖，不直接写入推荐结果分数。`total_score` 必须由推荐算法生成。
