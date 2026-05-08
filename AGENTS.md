# AGENTS.md - 工程协作规则

本文件适用于本仓库的 AI 编程代理、开发者和自动化脚本。项目文档已经按当前可运行工程组织，详细规则按以下入口查阅：

- `docs/README.md`：文档目录。
- `docs/DEVELOPMENT.md`：本地开发、测试和新功能流程。
- `docs/ARCHITECTURE.md`：系统架构、模块边界和数据流。
- `docs/API.md`：接口、统一响应、枚举和请求响应字段。
- `docs/DATABASE.md`：数据库表、字段、快照和初始化规则。
- `docs/RECOMMENDATION.md`：推荐算法、车型评分、排序和解释规则。
- `docs/FRONTEND.md`：前端页面、路由、交互和展示规范。
- `docs/OPERATIONS.md`：运行、配置、健康检查和常见问题。
- `docs/ROADMAP.md`：未实现功能和后续计划。

## 1. 推荐主链路优先

系统的核心不是普通车型 CRUD 或条件筛选，而是可解释汽车购买推荐。任何修改都不能破坏主链路：

```text
车型参数 -> 特征评分 -> 用户画像 -> 多维匹配 -> 推荐解释 -> 补充推荐 -> 推荐记录追溯
```

必须保证：

- 推荐结果来自真实评分、用户权重和推荐算法计算。
- 推荐结果包含综合推荐分、推荐标签、推荐理由、不足提醒和维度评分。
- 无完全匹配时可以补充相近推荐，并保留 `matchLevel`。
- 推荐历史读取保存快照，不重新计算覆盖历史结果。

## 2. 推荐算法规则

- 当前主算法版本是 `pareto-topsis-v1`。
- 当前算法名称是“主客观组合权重 + Pareto-TOPSIS”。
- `totalScore` 是 TOPSIS 综合推荐分，取值范围为 0-100。
- `rankNo` 是推荐结果展示排序的唯一权威。
- 前端不得按 `totalScore`、口碑分、热度分或其他字段二次排序推荐结果。
- `priceScore` 不存入 `car_feature_score`，只在推荐生成时按用户预算动态计算。
- Pareto 标记只用于同分辅助、管理追溯和算法可视化说明，不作为用户端推荐标签。
- 反馈只进入统计，不自动修改权重、车型评分或推荐排序。
- 系统不包装成深度学习、协同过滤或在线学习推荐。

## 3. 用户需求字段规则

当前用户需求 API 只使用以下字段：

- `budgetMin`
- `budgetMax`
- `brands`
- `bodyTypes`
- `energyTypes`
- `seatOptions`
- `scenes`
- `factorWeights`

`brands`、`bodyTypes`、`energyTypes`、`seatOptions` 为空数组时分别表示全部品牌、全部级别、全部动力和全部座位；`scenes` 为空时按综合需求计算；`budgetMax = null` 表示预算上限不限。`minSeats`、`excludedBrands`、`excludedCarIds` 仅作为后端兼容字段保留，当前产品前端不再展示排除品牌或排除车型入口。

动力规则：

- 用户需求侧允许选择 `新能源`。
- 后端将 `新能源` 展开为 `纯电 / 插混 / 增程` 参与匹配。
- `car_model.energy_type` 不保存 `新能源`，只保存 `燃油 / 纯电 / 插混 / 增程`。

## 4. 数据库与接口规则

- 本地 seed 默认普通用户为 `demo_user / demo123456`，`app_user.id = 1` 仅作为本地种子主键，不再作为接口默认身份来源。
- 本地 seed 默认管理员为 `demo_admin / admin123456`，`admin.id = 1` 仅作为本地种子主键，不再作为管理端接口默认身份来源。
- 用户端接口身份必须来自 JWT 当前 `USER`；管理端写入身份必须来自 JWT 当前 `ADMIN`。
- 新增 `/api/**` 接口必须明确归类为公开、`USER` 或 `ADMIN` 权限；认证拦截器默认拒绝未归类 API，不允许依赖默认放行。
- 普通用户注册只能创建 `USER` 账号，不允许创建或提升为管理员账号。
- 管理端用户管理只维护 `app_user.status`，可查看用户需求和推荐历史入口；收藏和反馈通过独立只读管理页面追溯，均不得触发推荐生成。
- `recommend_record` 和 `recommend_item` 必须保存推荐依据，不允许只保存车型 ID。
- 收藏只维护用户关注车型，不影响推荐排序。
- 反馈只维护反馈记录和统计数据，不改变推荐结果。
- 车型对比是当前登录 `USER` 的后端持久化列表，写入 `user_compare_car`；不得使用固定 localStorage key 保存车型 ID，不得通过 `userId` 参数操作他人对比。
- 自然语言解析后端接口保留，但当前产品前端不展示入口；它不直接保存需求或生成推荐，也不参与主推荐流程。
- `/api/recommend/{recordId}/algorithm-visualization` 只读，不写数据库，不生成推荐。
- 推荐生成请求不包含推荐数量字段，后端按候选集、分组和排序规则返回结果。
- 修改 API、数据库、前端或推荐算法时，必须同步更新对应文档和测试。

## 5. 代码组织规则

后端推荐相关逻辑必须在 Service 层实现，不得写在前端或 Controller 中。

后端分层：

```text
controller   接收请求、参数校验、统一响应
service      业务逻辑和推荐算法
mapper       数据访问
entity       数据库实体
dto          请求对象
vo           响应对象
common       统一响应、异常处理、分页对象
config       跨域、MyBatis-Plus、认证配置
util         评分、解析、权重归一化等工具
```

推荐相关服务包括：

- `CarFeatureScoreService`
- `UserProfileService`
- `RecommendationService`
- `RecommendationReasonService`
- `FallbackRecommendationService`
- `RecommendationRecordService`
- `DemandTextParseService`

## 6. 前端规则

- 前端实现以 `docs/FRONTEND.md` 为准。
- 未登录中间导航只显示首页，登录入口位于右上角；`/login` 只用于普通用户，`/admin/login` 只用于管理员。
- 管理员不显示首页入口，不能进入首页，登录后默认进入 `/admin/cars`，右上角名称固定显示“管理员”。
- 推荐结果页必须展示综合推荐分、推荐标签、推荐理由、不足提醒、维度评分和查看详情入口。
- 用户端不展示 TOPSIS、Pareto、熵权等复杂术语。
- 管理端和 `/algorithm-demo` 可以展示算法细节。
- 普通操作反馈使用页面内状态，不使用顶部 toast。
- 加入对比不立即跳转，用户主动进入 `/compare` 后再查看。
- 管理端“收藏车型”和“反馈记录”为独立只读页面；管理员不得取消收藏、删除反馈或代用户操作。

## 7. 安全和数据规则

- 不提交 `application-local.yml`。
- 不提交 `.env` 或 `.env.local`。
- 不提交真实密码、token、密钥或生产连接信息。
- 不操作真实数据库，除非用户明确确认目标库和操作影响。
- 不对真实 MySQL 执行迁移、重建、清空或删除数据，除非用户明确要求。
- 本地开发库需要重建时，先说明会删除本地需求、推荐记录、对比、收藏和反馈等数据。

## 8. 文档维护规则

修改文档时保持职责清晰：

- 开发流程写入 `docs/DEVELOPMENT.md`。
- 架构、模块边界和数据流写入 `docs/ARCHITECTURE.md`。
- 接口写入 `docs/API.md`。
- 数据库表和字段写入 `docs/DATABASE.md`。
- 推荐算法和评分规则写入 `docs/RECOMMENDATION.md`。
- 前端页面和交互写入 `docs/FRONTEND.md`。
- 运行、配置和常见问题写入 `docs/OPERATIONS.md`。
- 未实现功能写入 `docs/ROADMAP.md`。

不要在多个文档中重复维护同一份详细内容。若算法、数据库、API、前端文档之间出现字段不一致，应以对应专项文档为准，并同步修正相关文档、代码和测试。

## 9. 工作方式

新功能开发前先从原始需求判断目标是否清晰。若需求与当前工程规则冲突，先说明冲突点并等待确认。

方案和实现必须符合：

- 不给出兼容性或补丁性绕路方案。
- 不过度设计，选择满足需求的最短正确路径。
- 不自行扩展用户没有要求的业务逻辑。
- 修改完成后通过搜索、测试或构建验证关键链路。
