# 数据库设计

本文档只描述数据库表职责、关键字段和数据追溯关系。推荐闭环概要见 `RECOMMENDATION_DESIGN.md`，当前主算法细节见 `RECOMMENDATION_ALGORITHM_UPGRADE.md`，升级前算法基线和特征评分规则见 `RECOMMENDATION_ALGORITHM.md`，接口设计见 `API_DESIGN.md`，实施计划见 `IMPLEMENTATION_TASKS.md`。

## 1. 设计原则

- 数据库设计必须服务于推荐主链路。
- 车型评分由参数计算得出，不应人工随意指定。
- 推荐结果必须可追溯，不能只保存车型 ID。
- 所有价格统一使用元，尺寸统一使用毫米，续航统一使用公里。
- 软删除字段统一使用 `deleted`。
- 时间字段统一使用 `create_time`、`update_time`。
- JSON 字段第一版优先使用 MySQL 8 `JSON` 类型；如果 ORM 或方言处理不方便，可以退化为文本 JSON，但 API 结构保持不变。

## 2. 核心表总览

| 表名 | 阶段 | 职责 |
| --- | --- | --- |
| `app_user` | 必须 | 普通用户账户 |
| `admin` | 必须 | 管理员账户 |
| `car_model` | 必须 | 车型基础展示信息 |
| `car_param` | 必须 | 车型参数和配置 |
| `car_feature_score` | 必须 | 车型多维特征评分 |
| `user_demand` | 必须 | 用户购车需求、画像和权重 |
| `recommend_record` | 必须 | 一次推荐任务的总体记录 |
| `recommend_item` | 必须 | 推荐结果明细和解释依据 |
| `text_demand_parse_record` | 建议 | 自然语言解析记录 |
| `favorite` | 建议 | 用户收藏车型 |
| `feedback` | 建议 | 用户推荐反馈 |

## 3. 账户表

### 3.1 `app_user`

普通用户表，服务于需求保存、推荐历史、收藏和反馈。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `username` | 用户名，唯一 |
| `password` | 密码密文 |
| `nickname` | 昵称 |
| `phone` | 手机号，可选 |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

默认演示用户要求：

```text
app_user.id = 1
username = demo_user
```

第一版用户端接口 `userId` 为空时，后端使用该默认演示用户。

### 3.2 `admin`

管理员表，只需支持简单管理员登录，不做复杂 RBAC。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `username` | 管理员用户名，唯一 |
| `password` | 密码密文 |
| `role` | 固定为 `ADMIN` 或简单角色标识 |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

默认演示管理员要求：

```text
admin.id = 1
username = demo_admin
```

## 4. 车型数据表

### 4.1 `car_model`

车型基础信息表，用于列表、详情、推荐结果展示和基础过滤。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `brand` | 品牌 |
| `series` | 车系 |
| `model_name` | 车型名称 |
| `guide_price` | 指导价，单位元 |
| `body_type` | 车型类型：SUV、轿车、MPV |
| `energy_type` | 动力类型：燃油、纯电、插混、增程 |
| `seats` | 座位数 |
| `launch_year` | 上市年份 |
| `image_url` | 图片地址 |
| `sales_volume` | 销量或测试热度 |
| `user_rating` | 口碑评分，0-5 |
| `audit_status` | 审核状态，推荐仅使用通过车型 |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

`audit_status` 枚举：

```text
APPROVED：审核通过，可进入推荐候选
PENDING：待审核，不进入推荐候选
REJECTED：审核拒绝，不进入推荐候选
```

### 4.2 `car_param`

车型参数表，用于评分规则计算。

关键字段：

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

### 4.3 `car_feature_score`

车型特征评分表，由 `car_param` 和 `car_model` 计算生成。

关键字段：

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

注意：`price_score` 不存储在该表中，因为价格匹配分依赖用户预算，必须在推荐阶段动态计算。

当前版本不额外维护座椅材质、悬架类型、隔音、空调分区等舒适性参数，因此 `comfort_score` 由空间分、智能分和口碑分组合估算。具体公式见 `RECOMMENDATION_ALGORITHM.md`。

`popularity_score` 依赖全局销量最大值。单车重算可以更新参数类评分，但不保证全局热度归一化完全准确；当 `sales_volume` 发生变化时，建议执行全部车型评分重算。

## 5. 用户需求与画像表

### 5.1 `user_demand`

保存用户结构化需求、自然语言原文、画像文本和权重快照。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `user_id` | 用户 ID；接口未传时写入默认演示用户 `app_user.id = 1` |
| `raw_text` | 自然语言原始输入，可空 |
| `budget_min` | 预算下限 |
| `budget_max` | 预算上限 |
| `body_types` | 车型类型偏好，JSON 数组 |
| `energy_types` | 动力类型偏好，JSON 数组 |
| `min_seats` | 最低座位数 |
| `scenes` | 使用场景，JSON 数组 |
| `factor_weights` | 用户显式偏好权重滑块，JSON 对象，值域 0-10 |
| `excluded_brands` | 排除品牌，JSON 或文本 JSON |
| `excluded_car_ids` | 排除车型 ID，JSON 或文本 JSON |
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

需求侧 `energy_types` 可保存宽泛偏好 `新能源`；车型表 `car_model.energy_type` 不保存 `新能源`，只保存具体动力类型 `燃油 / 纯电 / 插混 / 增程`。

### 5.2 需求模型重构字段处理

本次需求模型重构后，`user_demand` 以多选和显式权重字段为准：

| 新字段 | 类型建议 | 示例 |
| --- | --- | --- |
| `body_types` | JSON 数组 | `["SUV", "MPV"]` |
| `energy_types` | JSON 数组 | `["插混", "新能源"]` |
| `scenes` | JSON 数组 | `["家庭出行", "长途自驾"]` |
| `factor_weights` | JSON 对象 | `{"price":0,"space":8,"safety":9,"energy":6,"intelligence":3,"comfort":7,"power":0,"reputation":4,"popularity":0}` |

如果当前数据库为兼容已有实现临时使用 TEXT 保存 JSON 字符串，字段语义仍必须按 JSON 数组或 JSON 对象处理；API 层返回数组或对象，不返回 JSON 字符串。

旧字段处理决策：

| 旧字段 | 新字段 | 处理建议 |
| --- | --- | --- |
| `body_type` | `body_types` | 本地库重建后删除旧列，不做长期兼容 |
| `energy_type` | `energy_types` | 本地库重建后删除旧列，不做长期兼容 |
| `scene` | `scenes` | 本地库重建后删除旧列，不做长期兼容 |
| `focus_factors` | `factor_weights` | 本地库重建后删除旧列，不做长期兼容；旧关注因素不再转换为新权重 |

不能长期同时维护新旧两套字段语义，否则会导致推荐过滤、画像文本和历史追溯出现歧义。已确认采用重建本地库方案，实施顺序：

1. 调整 `user_demand` 表结构，只保留 `body_types`、`energy_types`、`scenes`、`factor_weights`。
2. 更新初始化脚本和种子数据，旧列 `body_type`、`energy_type`、`scene`、`focus_factors` 不再创建。
3. 后端和前端全部切换到新字段，不实现新旧字段双写、双读或自动兼容。
4. 重建本地数据库前仍需在执行操作时再次确认，因为该操作会清空本地联调数据。

如果本地 MySQL 中已有联调数据，重建库、删除表或清空表会丢失现有需求和推荐记录；执行前必须向用户说明影响并获得操作确认。推荐记录和推荐明细已有快照，不应通过重新计算方式迁移覆盖。

## 6. 推荐追溯表

### 6.1 `recommend_record`

保存一次推荐任务的总体信息。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `user_id` | 用户 ID；演示模式下写入 `app_user.id = 1` |
| `demand_id` | 用户需求 ID |
| `profile_text` | 推荐时的画像文本快照 |
| `weight_snapshot` | 权重快照，JSON 或文本 JSON；新版包含 `algorithmVersion`、`alpha`、`subjectiveWeight`、`objectiveWeight`、`finalWeight` |
| `fallback_message` | 降级提示 |
| `recommend_status` | 推荐状态，例如 `SUCCESS`、`FALLBACK`、`EMPTY` |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

`recommend_status` 枚举与生成规则：

```text
SUCCESS：最终返回结果不为空，且所有推荐项均为 STRICT。
FALLBACK：最终返回结果不为空，且至少存在一个非 STRICT 推荐项。
EMPTY：所有阶段都没有候选结果。
```

### 6.2 `recommend_item`

保存每个推荐结果的分数、排序和解释依据。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `record_id` | 推荐记录 ID |
| `car_id` | 车型 ID |
| `rank_no` | 排名 |
| `total_score` | 综合匹配度；阶段 9.6-D 起为 TOPSIS 推荐分，旧历史记录可能为加权求和基线分 |
| `price_score` | 动态价格分 |
| `space_score` | 空间分快照 |
| `safety_score` | 安全分快照 |
| `energy_score` | 能耗分快照 |
| `intelligence_score` | 智能分快照 |
| `comfort_score` | 舒适分快照 |
| `power_score` | 动力分快照 |
| `reputation_score` | 口碑分快照 |
| `popularity_score` | 热度分快照 |
| `tags` | 推荐标签快照，JSON 或文本 JSON |
| `match_level` | 匹配状态 |
| `reason_text` | 推荐理由 |
| `weakness_text` | 不足提醒 |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

推荐明细中的 `tags`、各维度分数、`reason_text`、`weakness_text` 和 `match_level` 都是推荐发生时的快照。历史记录查询时不应重新计算覆盖这些字段，否则会破坏推荐追溯一致性。

## 7. 第二档功能表

### 7.1 `text_demand_parse_record`

保存自然语言解析过程，便于展示和排查。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `user_id` | 用户 ID |
| `raw_text` | 原始输入 |
| `parse_result_json` | 解析结果 |
| `mapped_terms` | 映射词 |
| `unsupported_terms` | 不支持词 |
| `ambiguous_terms` | 模糊词 |
| `confidence_score` | 置信度 |
| `confirm_status` | 确认状态 |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

### 7.2 `favorite`

用户收藏表。

关键字段：`id`、`user_id`、`car_id`、`deleted`、`create_time`、`update_time`。

### 7.3 `feedback`

用户反馈表。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `user_id` | 用户 ID |
| `record_id` | 推荐记录 ID |
| `satisfaction_score` | 满意度 1-5 |
| `reason_type` | 不满意原因 |
| `feedback_text` | 文字反馈 |
| `deleted` | 软删除 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

## 8. 数据关系

```text
app_user 1 - n user_demand
user_demand 1 - n recommend_record
recommend_record 1 - n recommend_item
car_model 1 - 1 car_param
car_model 1 - 1 car_feature_score
car_model 1 - n recommend_item
recommend_record 1 - n feedback
app_user 1 - n favorite
car_model 1 - n favorite
```

## 9. 种子数据要求

最低演示数据：20 条车型。

答辩建议数据：50-80 条车型。

如果没有足够真实数据，则允许编写合理的测试数据用于开发和测试。测试数据不是市场结论，只用于验证系统流程、推荐算法和页面展示。

数据覆盖要求：

- 车型类型：SUV、轿车、MPV。
- 动力类型：燃油、纯电、插混、增程。
- 价格区间：8 万以内、8-12 万、10-15 万、15-25 万、25 万以上。
- 场景适配：家庭出行、城市通勤、长途自驾、新手代步、商务接待。
- 需求样例应覆盖多个 `body_types`、多个 `energy_types`、多个 `scenes` 和显式 `factor_weights`。

测试数据编写原则：

- 同一车型的价格、车身尺寸、座位数、动力类型应保持逻辑一致。
- 燃油车应填写油耗，纯电车应填写纯电续航，插混/增程应填写综合续航。
- 安全和智能配置应有高、中、低差异，避免所有车型评分过于接近。
- `sales_volume` 和 `user_rating` 可以使用测试值，但应保持合理分布。
- 不允许直接编写 `total_score` 作为推荐结果；推荐总分必须由算法计算得到。
- 可以预置少量极端数据，用于验证无匹配降级推荐。

## 10. 建议索引

建议添加以下索引：

- `car_model(brand)`
- `car_model(body_type)`
- `car_model(energy_type)`
- `car_model(guide_price)`
- `car_param(car_id)` 唯一索引
- `car_feature_score(car_id)` 唯一索引
- `user_demand(user_id)`
- 如使用 MySQL 8 JSON 字段，可根据实际查询方式评估 `body_types`、`energy_types`、`scenes` 的生成列或函数索引；第一版可以先依赖应用层解析和候选过滤。
- `recommend_record(user_id)`
- `recommend_record(demand_id)`
- `recommend_item(record_id)`
- `favorite(user_id, car_id)` 唯一索引
