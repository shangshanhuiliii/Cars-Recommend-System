# 已完成阶段汇总

本文档记录项目当前已经完成并验收通过的阶段、实际能力、验证结果和后续注意事项。详细需求边界仍以 `PROJECT_SPEC.md` 为准；推荐算法公式和伪代码见 `RECOMMENDATION_ALGORITHM.md`；阶段计划见 `IMPLEMENTATION_TASKS.md`。

项目主链路按以下顺序逐步闭环：

```text
车型参数 -> 特征评分 -> 用户画像 -> 多维匹配 -> 推荐解释 -> 降级推荐 -> 推荐记录追溯 -> 前端展示 -> 管理端追溯统计
```

## 阶段 0：工程骨架与约定

### 阶段目标

建立前后端工程基础、统一接口响应和项目级协作规则，使后续推荐主链路开发有稳定工程入口。

### 已完成内容

- 后端 Spring Boot 3、Java 17、Maven 工程骨架。
- 前端 Vue 3、Vite、Element Plus 工程骨架。
- `GET /api/health` 健康检查。
- 统一响应结构、基础异常处理、CORS 配置。
- 推荐相关枚举和服务分层约定。
- 本地敏感配置规范和 `.gitignore` 规则。

### 主要接口或页面

- `GET /api/health`
- 前端基础路由和首页入口。

### 主要数据表或字段变化

- 本阶段不创建业务表。
- 明确默认演示用户和管理员将在阶段 1 通过种子数据提供。

### 测试与验证结果

- 后端健康检查可访问。
- 前端开发服务可启动。
- 本地配置文件 `application-local.yml` 被忽略，不进入 Git。

### 是否达到最低完成标准

是。工程基础、约定和健康检查已完成。

### 遗留问题或后续注意事项

- 认证系统暂不实现，使用演示用户策略。
- 后续功能必须继续保持推荐算法逻辑在 Service 层。

## 阶段 1：数据库与种子数据

### 阶段目标

建立推荐闭环需要的核心数据结构，并提供可用于开发、测试和答辩演示的车型种子数据。

### 已完成内容

- 建立 8 张核心表：
  `app_user`、`admin`、`car_model`、`car_param`、`car_feature_score`、`user_demand`、`recommend_record`、`recommend_item`。
- 提供默认演示用户 `app_user.id = 1 / demo_user`。
- 提供默认演示管理员 `admin.id = 1 / demo_admin`。
- 提供 20 条合理车型种子数据和对应参数。
- 明确推荐追溯字段：`recommend_record.recommend_status`、`recommend_item.tags`、`recommend_item.match_level`、`user_demand.excluded_car_ids`。

### 主要接口或页面

- 本阶段以数据库脚本为主，不新增用户可见页面。

### 主要数据表或字段变化

- `car_model` 保存车型基础信息，`energy_type` 仅允许 `燃油 / 纯电 / 插混 / 增程`。
- `car_param` 保存评分所需参数。
- `car_feature_score` 保存车型特征评分，不保存 `price_score`。
- `user_demand` 保存需求、画像和权重。
- `recommend_record`、`recommend_item` 保存推荐快照。

### 测试与验证结果

- `schema.sql` 可创建核心表。
- `seed-data.sql` 可导入默认用户、管理员和 20 条车型数据。
- 已验证 `car_model.energy_type` 不包含“新能源”。

### 是否达到最低完成标准

是。推荐算法所需的数据基础已建立。

### 遗留问题或后续注意事项

- 车型数据仍是演示数据，不代表真实市场结论。
- 当真实 MySQL 已有联调数据时，重建数据库前必须再次确认。

## 阶段 2：车型管理与参数维护

### 阶段目标

让管理端可以维护推荐算法需要的车型基础信息和参数数据。

### 已完成内容

- 管理端车型基础信息接口。
- 车型参数查询与保存接口。
- 车型软删除。
- 管理端车型维护页面。
- 接口路径统一为 `/api/admin/cars` 风格。

### 主要接口或页面

- `GET /api/admin/cars`
- `GET /api/admin/cars/{id}`
- `POST /api/admin/cars`
- `PUT /api/admin/cars/{id}`
- `DELETE /api/admin/cars/{id}`
- `GET /api/admin/cars/{id}/param`
- `PUT /api/admin/cars/{id}/param`
- `/admin/cars`

### 主要数据表或字段变化

- 使用 `car_model` 和 `car_param`。
- 通过 `deleted` 实现软删除。

### 测试与验证结果

- 车型基础信息可新增、编辑、查询、软删除。
- 车型参数可保存并被后续评分服务读取。

### 是否达到最低完成标准

是。车型数据维护能力已满足评分和推荐前置需求。

### 遗留问题或后续注意事项

- 车型管理只服务于推荐数据基础，不扩展 Excel 导入、复杂审核流或复杂权限。

## 阶段 3：特征评分规则引擎

### 阶段目标

将车型参数转换为统一的多维特征评分，为推荐算法提供可计算的车型向量。

### 已完成内容

- 根据 `car_model` 和 `car_param` 自动计算 `car_feature_score`。
- 已实现评分维度：
  `spaceScore`、`safetyScore`、`energyScore`、`intelligenceScore`、`comfortScore`、`powerScore`、`reputationScore`、`popularityScore`。
- `priceScore` 不写入 `car_feature_score`，保留给推荐阶段动态计算。
- `comfortScore = spaceScore * 0.5 + intelligenceScore * 0.2 + reputationScore * 0.3`。
- `popularityScore = salesVolume / maxSales * 100`。
- 支持单车评分重算、全部车型评分重算和评分查询。
- 旧评分路径已清理，统一使用车型管理路径。

### 主要接口或页面

- `GET /api/admin/cars/{id}/score`
- `POST /api/admin/cars/{id}/score/recalculate`
- `POST /api/admin/cars/scores/recalculate`
- 管理端车型页评分入口。

### 主要数据表或字段变化

- 写入 `car_feature_score`。
- `score_version` 和 `calculated_time` 保存评分版本与计算时间。

### 测试与验证结果

- `mvn test` 通过。
- `mvn package` 通过。
- `npm run build` 通过。
- 已验证 20 条种子车型可生成评分。

### 是否达到最低完成标准

是。车型参数已经可以转化为推荐算法所需的多维评分。

### 遗留问题或后续注意事项

- 销量变化会影响全局热度归一化，建议执行全部车型评分重算。
- 舒适分当前为组合估算，后续如增加细粒度舒适配置可扩展。

## 阶段 4：用户需求与画像

### 阶段目标

保存结构化购车需求，并将用户需求转化为画像文本和多维偏好权重。

### 已完成内容

- 实现结构化需求保存。
- 实现最近一次需求查询。
- 实现按 ID 查询用户需求。
- `userId` 为空时使用默认演示用户 `app_user.id = 1`。
- 保存画像文本 `profileText`。
- 保存九维权重字段。
- JSON 字段 API 返回数组或对象，不返回 JSON 字符串。

### 主要接口或页面

- `POST /api/user/demand`
- `GET /api/user/demand/latest`
- `GET /api/user/demand/{id}`

### 主要数据表或字段变化

- 使用 `user_demand`。
- 阶段 9.5 后当前有效需求字段为：
  `body_types`、`energy_types`、`scenes`、`factor_weights`、`min_seats`。
- 旧字段 `body_type`、`energy_type`、`scene`、`focus_factors` 已废弃，不做长期兼容。

### 测试与验证结果

- 覆盖家庭出行、城市通勤等画像权重场景。
- 覆盖默认演示用户。
- 覆盖 `energyTypes` 中允许“新能源”作为用户侧宽泛偏好。
- 覆盖排除车型保存和返回。

### 是否达到最低完成标准

是。需求可以保存并生成可用于推荐的画像和权重。

### 遗留问题或后续注意事项

- 自然语言解析尚未实现，当前以结构化表单为准。
- 显式偏好权重只影响权重和排序，不作为硬过滤条件。

## 阶段 5：真实推荐算法基础闭环

### 阶段目标

实现真实推荐算法的基础闭环，替代 mock 推荐和简单车型 ID 返回。

### 已完成内容

- 实现 `POST /api/recommend/generate`。
- 读取用户需求、用户画像权重、车型、参数和特征评分。
- 候选车型只允许未删除、审核通过且存在评分的车型。
- 实现严格过滤：
  排除品牌、排除车型、最低座位数、预算上限、车型类型、动力类型。
- 实现动态 `priceScore`。
- 实现多维加权 `totalScore`。
- 实现推荐排序和推荐标签 `tags`。
- 保存 `recommend_record` 和 `recommend_item`。
- 阶段 5 仅实现 `STRICT`，不实现降级推荐。

### 主要接口或页面

- `POST /api/recommend/generate`

### 主要数据表或字段变化

- 写入 `recommend_record`。
- 写入 `recommend_item`，包含总分、价格分、各维度分、标签和匹配状态。
- `priceScore` 只保存在推荐明细，不写入车型评分表。

### 测试与验证结果

- 覆盖家庭出行和城市通勤推荐。
- 覆盖预算上限严格过滤、预算下限软偏好。
- 覆盖 `energyTypes` 包含“新能源”时展开为 `纯电 / 插混 / 增程`。
- 覆盖排除品牌、排除车型。
- 覆盖真实权重和评分计算。
- 覆盖推荐记录和推荐明细保存。

### 是否达到最低完成标准

是。推荐结果来自真实评分、动态价格分和用户权重计算。

### 遗留问题或后续注意事项

- 推荐解释和降级策略在阶段 6 补齐。
- 阶段 9.5 后推荐生成不再使用固定 TopK 截断。

## 阶段 6：推荐解释与降级

### 阶段目标

补齐推荐理由、不足提醒和无匹配或匹配不足时的分级补充推荐。

### 已完成内容

- 每条推荐生成 `reasonText`。
- 每条推荐生成 `weaknessText`。
- 实现分级匹配状态：
  `STRICT`、`RELAX_BUDGET`、`RELAX_BODY_TYPE`、`RELAX_ENERGY_TYPE`、`SIMILAR_RECOMMEND`。
- 降级推荐作为候选补充，不覆盖前序阶段结果。
- 按 `carId` 去重，同一车型保留首次进入推荐集时的 `matchLevel`。
- 排除品牌、排除车型、最低座位数不参与降级放宽。
- 生成 `recommendStatus`：`SUCCESS`、`FALLBACK`、`EMPTY`。
- 根据 `strictCount` 生成语义正确的 `fallbackMessage`。

### 主要接口或页面

- `POST /api/recommend/generate`
- 推荐结果页和管理端记录页消费 `reasonText`、`weaknessText`、`matchLevel`、`fallbackMessage`。

### 主要数据表或字段变化

- `recommend_record.recommend_status`
- `recommend_record.fallback_message`
- `recommend_item.match_level`
- `recommend_item.reason_text`
- `recommend_item.weakness_text`

### 测试与验证结果

- 覆盖严格匹配、预算放宽、车型放宽、动力放宽和相似推荐。
- 覆盖去重和首次匹配状态保留。
- 覆盖 `FALLBACK`、`EMPTY` 状态。
- 覆盖无明显短板时的保底不足提醒。
- 覆盖历史保存的 `matchLevel`、`reasonText`、`weaknessText` 非空。

### 是否达到最低完成标准

是。推荐结果已具备解释能力和可追溯降级能力。

### 遗留问题或后续注意事项

- 用户端阶段 9.5 后弱化技术降级提示，但后端和管理端仍保留追溯字段。
- `fallbackMessage` 不应从后端删除。

## 阶段 7：推荐记录追溯

### 阶段目标

实现推荐历史列表和历史详情，让每次推荐可回查、可复现、可解释。

### 已完成内容

- 实现推荐历史列表。
- 实现推荐历史详情。
- 历史详情读取 `recommend_record` 和 `recommend_item` 快照。
- 不重新计算历史标签、分数、理由、不足或匹配状态。
- 支持默认演示用户。
- 推荐明细按 `rankNo` 升序返回。

### 主要接口或页面

- `GET /api/recommend/history`
- `GET /api/recommend/{recordId}`
- 用户端推荐历史页。

### 主要数据表或字段变化

- 读取 `recommend_record`。
- 读取 `recommend_item`。
- 关联读取当时的 `user_demand` 需求快照字段。

### 测试与验证结果

- 覆盖生成推荐后历史列表可查询。
- 覆盖历史列表 `topCarNames` 和 `itemCount`。
- 覆盖历史详情返回画像、权重、状态和降级提示。
- 覆盖历史详情返回分数、标签、理由、不足和匹配状态快照。
- 覆盖分页和用户隔离。

### 是否达到最低完成标准

是。推荐记录已经可追溯，不依赖重新计算。

### 遗留问题或后续注意事项

- 后续若评分规则变化，历史记录仍应展示保存快照，不应覆盖。

## 阶段 8：用户端核心页面

### 阶段目标

将阶段 4-7 的接口串联成用户可操作的推荐闭环。

### 已完成内容

- 购车需求页接入需求保存和推荐生成。
- 推荐生成成功后跳转推荐结果页。
- 推荐结果页读取历史详情快照。
- 推荐结果页展示画像、权重摘要、推荐状态、车型列表、综合分、价格分、各维度分、标签、理由和不足。
- 车型详情页接入用户端只读接口 `GET /api/car/{id}`。
- 推荐历史页接入历史接口。
- 首页提供简洁入口。
- 新增默认车型图片兜底，避免无图片时空白。

### 主要接口或页面

- `/recommend`
- `/recommend/result/:recordId`
- `/car/:id`
- `/history`
- `GET /api/car/{id}`
- `GET /api/car/brands`
- `GET /api/car/options`

### 主要数据表或字段变化

- 不新增业务表。
- 用户端车型详情读取 `car_model`、`car_param`、`car_feature_score`。

### 测试与验证结果

- `npm run build` 通过。
- 如新增后端接口，`mvn test` 和 `mvn package` 通过。
- 真实 MySQL 链路联调通过：
  车型评分重算、车型列表、车型详情、需求提交、推荐生成、推荐详情、推荐历史。

### 是否达到最低完成标准

是。用户端已经能完成需求提交、推荐生成、结果查看、详情查看和历史查看。

### 遗留问题或后续注意事项

- 自然语言解析、对比、收藏、反馈尚未实现。
- 推荐结果页阶段 9.5 后已按“完全匹配车型”和“推荐”分组优化。

## 阶段 9：管理端核心页面

### 阶段目标

完成管理端核心页面，使管理端服务于推荐数据基础、推荐追溯和答辩展示。

### 已完成内容

- 完善车型管理页：基础信息、参数维护、评分查看、单车评分重算、全部评分重算。
- 完成管理端推荐记录页：展示需求画像、权重快照、推荐状态、降级提示和推荐明细。
- 完成统计仪表盘基础能力。
- 实现只读统计接口 `GET /api/admin/stat/overview`。
- 健康检查增强为可真实探测 DataSource。

### 主要接口或页面

- `/admin/cars`
- `/admin/recommend-records`
- `/admin/dashboard`
- `GET /api/admin/stat/overview`

### 主要数据表或字段变化

- 统计来自 `user_demand`、`recommend_record`、`recommend_item` 和车型相关表。
- 不新增复杂运营后台表。

### 测试与验证结果

- `mvn test` 通过。
- `mvn package` 通过。
- `npm run build` 通过。
- 手工验证管理端车型、推荐记录和统计页可访问。
- 搜索确认管理端推荐记录页不调用推荐生成或评分重算接口来重算历史结果。
- 统计图表不使用随机数据。

### 是否达到最低完成标准

是。管理端已具备推荐闭环追溯和基础统计展示能力。

### 遗留问题或后续注意事项

- 反馈表尚未实现，因此满意度和不满意原因统计可为空。
- 管理端不做复杂权限、复杂运营分析或删除推荐记录。

## 阶段 9.5：需求模型与推荐结果体验重构

### 阶段目标

将购车需求模型从单选和标签关注因素重构为多选和显式权重滑块，同时优化推荐结果页展示语义。

### 已完成内容

- 用户需求字段重构：
  `bodyTypes`、`energyTypes`、`scenes`、`factorWeights`、`minSeats`。
- 删除旧字段长期兼容，不做新旧字段双读、双写或自动迁移。
- `factorWeights` 0-10 滑块由后端归一化。
- 多场景模板平均后生成默认权重。
- 用户端删除推荐数量输入，不再传 `topK`。
- 推荐生成不再按固定 TopK、默认 Top 10 或 `min(5, topK)` 截断。
- `STRICT` 组返回全部完全匹配候选。
- 非 `STRICT` 组返回全部补充推荐候选。
- 最终展示顺序为 `STRICT` 组在前、推荐组在后。
- 用户端推荐结果页按“完全匹配车型”和“推荐”分组展示。
- 用户端不展示 `fallbackMessage` 顶部强提示，不展示技术性“降级推荐”标签。
- `tags` 只展示推荐亮点，不包含 `matchLevel` 技术状态。
- 推荐结果页车型详情改为 Drawer 或 Dialog 展示，保留独立详情页。
- 新增品牌和车型选项接口用于排除条件搜索选择。

### 主要接口或页面

- `POST /api/user/demand`
- `GET /api/user/demand/latest`
- `GET /api/user/demand/{id}`
- `POST /api/recommend/generate`
- `GET /api/car/brands`
- `GET /api/car/options`
- `/recommend`
- `/recommend/result/:recordId`

### 主要数据表或字段变化

- `user_demand` 新字段：
  `body_types`、`energy_types`、`scenes`、`factor_weights`、`min_seats`。
- `user_demand` 旧字段删除：
  `body_type`、`energy_type`、`scene`、`focus_factors`。
- `car_model.energy_type` 仍不保存“新能源”。

### 测试与验证结果

- `mvn test` 通过。
- `mvn package` 通过。
- `npm run build` 通过。
- 本地 MySQL 重建和阶段 9.5 全链路联调通过：
  - `schema.sql` 和 `seed-data.sql` 执行成功。
  - 20 条车型评分重算成功。
  - 新结构需求提交成功。
  - 推荐生成成功，返回 `recordId`、`recommendStatus`、STRICT 组和推荐组。
  - 推荐详情、历史、品牌列表、车型选项、管理端统计接口均正常。
  - 前端 `/recommend`、`/recommend/result/:recordId`、`/history`、`/admin/dashboard` 可访问。

### 是否达到最低完成标准

是。新需求模型已贯通数据库、API、推荐算法、前端表单和推荐结果展示。

### 遗留问题或后续注意事项

- 本地真实库重建会清空联调数据，执行前必须获得明确授权。
- 自然语言解析需要按新字段输出 `bodyTypes`、`energyTypes`、`scenes`、`factorWeights`。
- 用户端隐藏顶部强提示不代表后端删除 `fallbackMessage`；管理端和历史详情仍可展示。

## 当前 MVP 状态

当前系统已经可以完成完整 MVP 主链路：

```text
车型参数 -> 特征评分 -> 用户需求 -> 用户画像权重 -> 推荐计算 -> 推荐解释 -> 降级推荐 -> 推荐记录保存 -> 前端展示 -> 管理端追溯统计
```

已经具备的核心能力：

- 车型基础数据和参数维护。
- 车型多维特征评分。
- 结构化购车需求保存。
- 多车型类型、多动力类型、多使用场景和九维显式偏好权重建模。
- 动态价格分和多维加权推荐。
- 严格匹配和分级补充推荐。
- 推荐标签、推荐理由和不足提醒。
- 推荐记录和推荐明细快照保存。
- 用户端需求页、推荐结果页、车型详情页和历史页。
- 管理端车型管理、推荐记录追溯和统计仪表盘。

仍未完成的内容：

- 自然语言需求解析。
- 车型对比雷达图。
- 收藏。
- 用户反馈。
- 图片上传。
- 复杂权限。
- Redis。
- 深度学习推荐。
- 在线学习推荐。

建议下一步进入阶段 10：自然语言解析。阶段 10 必须输出新结构字段，不得恢复旧字段或固定 TopK 规则。
