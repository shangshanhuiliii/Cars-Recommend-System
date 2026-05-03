# 系统规则合同

本文档定义当前项目不能随意改动的规则。新功能开发前必须先查本文档；如果需求、建议或实现方式与本文档冲突，必须先停下来确认。

## 1. 推荐算法合同

- 当前推荐算法版本是 `pareto-topsis-v1`。
- 当前算法名称是“主客观组合权重 + Pareto-TOPSIS”。
- `totalScore` 是 TOPSIS 综合推荐分，取值范围为 0-100。
- `rankNo` 是推荐结果展示排序的唯一权威。
- `STRICT` 组整体在推荐组前，同组内排序由后端确定。
- Pareto 只参与排序辅助、管理追溯和算法可视化说明，不作为用户端技术标签展示。
- 用户端不展示 TOPSIS、Pareto、熵权等复杂术语；管理端和 `/algorithm-demo` 可以展示算法细节。
- 反馈只进入统计，不自动修改权重、车型评分或推荐排序。
- 系统不包装成深度学习、协同过滤或在线学习推荐。

## 2. 用户需求字段合同

当前用户需求 API 只使用以下字段：

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

- 用户需求侧允许选择 `新能源`。
- 后端将 `新能源` 展开为 `纯电 / 插混 / 增程` 参与匹配。
- `car_model.energy_type` 只保存具体动力类型，例如 `燃油 / 纯电 / 插混 / 增程`。

## 3. 数据库合同

- 默认演示用户是 `app_user.id = 1`。
- 默认演示管理员是 `admin.id = 1`。
- `recommend_record` 和 `recommend_item` 是推荐历史快照。
- 历史推荐记录读取快照，不重新计算覆盖。
- `priceScore` 不进入 `car_feature_score`，只在推荐阶段动态计算。
- 收藏只维护用户关注车型，不影响推荐排序。
- 反馈只维护反馈记录和统计数据。
- `application-local.yml` 不得提交。
- 真实 MySQL 迁移、重建或清空数据必须先确认。

## 4. API 合同

- 推荐生成接口只接收用户和需求标识，不包含推荐数量字段。
- `POST /api/user/demand/parse-text` 只解析自然语言并返回表单草稿，不写库，不生成推荐。
- `GET /api/recommend/{recordId}/algorithm-visualization` 是只读接口，不写库，不生成推荐，不改变推荐快照。
- 车型对比接口只读，不触发评分重算。
- 收藏接口只维护收藏关系。
- 反馈接口只维护反馈记录，不修改推荐记录和推荐明细。
- 管理端统计必须来自真实数据库数据，不使用随机数或前端假数据。

## 5. 前端合同

- 推荐结果页按后端 `rankNo` 展示。
- 前端不得按 `totalScore`、口碑分、热度分或其他字段二次排序推荐结果。
- 用户端只展示用户可理解的综合推荐分、推荐标签、推荐理由、不足提醒和维度评分。
- 管理端和算法可视化页可以展示算法细节。
- 普通操作反馈不使用顶部 toast。
- 加入对比不立即跳转，通过页面内状态反馈；用户主动点击查看对比后再进入 `/compare`。
- 自然语言解析是辅助填写，不是主入口；解析结果必须允许用户确认或修改。

## 6. 文档合同

- 接口字段只在 `API_DESIGN.md` 维护。
- 数据库字段只在 `DATABASE_DESIGN.md` 维护。
- 当前推荐算法详细说明只在 `RECOMMENDATION_ALGORITHM_UPGRADE.md` 和 `RECOMMENDATION_IMPLEMENTATION_LOGIC.md` 维护。
- 车型特征评分规则只在 `RECOMMENDATION_ALGORITHM.md` 维护。
- 前端体验只在 `FRONTEND_DESIGN.md` 维护。
- 阶段历史文档只用于追溯和论文材料，不作为当前开发规则源。
