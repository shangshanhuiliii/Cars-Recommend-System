# 代理开发提示词模板

使用方式：每次让代理开发前，复制本文档中的提示词，把「本次任务」「允许修改范围」「禁止修改范围」替换成本次真实要求后发送给代理。

```text
任务：【在这里写清楚本次要做的具体任务】

本次开发必须基于当前可运行工程进行，不要恢复旧阶段、旧方案、旧字段或旧文档口径。当前项目不是阶段测试工程，也不是毕设/答辩工程，而是一个可运行、可继续维护的汽车推荐系统。

开始前必须执行：

```powershell
git status --short
git log --oneline -5
```

如果 `git status --short` 中存在非本次任务产生的未提交改动，必须先说明这些改动，不要覆盖、删除或回滚。除非我明确要求，不要执行 `git reset`、`git checkout --`、删除文件、清空数据库或提交 Git。

开始前必须阅读：

```text
AGENTS.md
README.md
docs/README.md
docs/DEVELOPMENT.md
docs/ARCHITECTURE.md
docs/API.md
docs/DATABASE.md
docs/RECOMMENDATION.md
docs/FRONTEND.md
docs/OPERATIONS.md
docs/ROADMAP.md
```

并根据本次任务额外阅读相关代码：

- 修改接口：阅读相关 Controller、DTO、VO、Service、Mapper。
- 修改数据库：阅读 `docs/DATABASE.md`、`backend/src/main/resources/db/schema.sql`、`backend/src/main/resources/db/seed-data.sql`。
- 修改推荐算法：阅读 `docs/RECOMMENDATION.md` 和 `backend/src/main/java/com/carsrecommend/system/service/impl` 下推荐相关服务。
- 修改前端：阅读 `docs/FRONTEND.md`、`frontend/src/router/index.js` 和相关页面组件。
- 修改运行脚本：阅读 `docs/OPERATIONS.md`、`docs/DEVELOPMENT.md` 和 `scripts/init-dev-db.ps1`。
- 修改文档：阅读所有受影响的专项文档，保证路径、接口、字段、算法和页面描述一致。

如果本次需求和现有文档、代码、数据库结构或 `AGENTS.md` 存在冲突，先停下来说明冲突点，等待确认后再继续。

当前系统硬性规则如下，不能破坏：

1. 推荐主链路

```text
车型参数 -> 特征评分 -> 用户画像 -> 多维匹配 -> 推荐解释 -> 补充推荐 -> 推荐记录追溯
```

推荐结果必须来自真实评分、用户权重和推荐算法计算，不能使用 mock 推荐替代真实推荐。

2. 当前推荐算法

- 算法版本：`pareto-topsis-v1`
- 算法名称：主客观组合权重 + Pareto-TOPSIS
- `totalScore` 是 TOPSIS 综合推荐分，范围 0-100
- `rankNo` 是推荐结果展示排序的唯一权威
- 前端不得按 `totalScore`、口碑、热度或其他字段二次排序
- Pareto 标记只用于排序辅助、管理追溯和算法可视化，不作为用户端标签
- 反馈只进入统计，不自动修改权重、车型评分或推荐排序
- 不要把系统包装成深度学习、协同过滤或在线学习推荐

3. 当前用户需求字段

用户需求 API 只使用：

```text
bodyTypes
energyTypes
scenes
factorWeights
minSeats
budgetMin
budgetMax
excludedBrands
excludedCarIds
```

不要恢复旧字段、旧 topK 或旧推荐数量输入。

4. 当前预算规则

- `budgetMin` 是预算下限
- `budgetMax` 是预算上限
- STRICT 完全匹配必须满足预算区间
- 低于预算下限或高于预算上限的车型不能进入 STRICT
- 预算外但接近预算区间的车型只能进入推荐组
- `priceScore` 仍是推荐阶段动态价格匹配分，不写入 `car_feature_score`

5. 当前动力规则

- 用户需求侧允许选择 `新能源`
- 后端将 `新能源` 展开为 `纯电 / 插混 / 增程`
- `car_model.energy_type` 不保存 `新能源`
- `car_model.energy_type` 只保存具体动力类型，例如 `燃油 / 纯电 / 插混 / 增程`

6. 当前功能边界

- 自然语言解析只辅助填写结构化表单，不直接保存需求或生成推荐
- `/api/recommend/{recordId}/algorithm-visualization` 只读，不生成推荐，不写数据库
- `/algorithm-demo` 是算法可视化页面，不是普通用户主流程
- 收藏不影响推荐排序
- 反馈只进入统计，不做在线学习
- 推荐历史读取 `recommend_record` 和 `recommend_item` 快照，不重新计算覆盖历史结果
- 健康检查页面属于管理端，不放在普通用户首页

7. 前端规则

- 普通用户页面不展示 TOPSIS、Pareto、熵权等复杂算法术语
- 推荐结果页必须按后端 `rankNo` 展示
- 推荐结果页应展示综合推荐分、推荐标签、推荐理由、不足提醒、维度评分和详情入口
- 普通操作反馈使用页面内状态，不使用顶部 toast
- 加入对比不应立即跳转
- 首页面向完整产品，不展示阶段测试、答辩、MVP 等旧口径文字

8. 数据库和安全规则

- 不提交 `backend/src/main/resources/application-local.yml`
- 不提交 `.env`、`.env.local`
- 不提交真实密码、token、密钥或生产连接信息
- 不操作真实 MySQL，除非我明确确认目标库和影响
- 不执行数据库重建、清空、删除、迁移，除非我明确要求
- 本地开发库重建必须说明会删除需求、推荐记录、收藏、反馈和评分等数据

9. 文档维护规则

如果修改了以下内容，必须同步对应文档：

- 接口变更：同步 `docs/API.md`
- 数据库字段或表变更：同步 `docs/DATABASE.md`
- 推荐算法、权重、排序、解释、预算规则变更：同步 `docs/RECOMMENDATION.md`
- 前端页面、路由、交互、展示文案变更：同步 `docs/FRONTEND.md`
- 运行、脚本、初始化方式变更：同步 `docs/OPERATIONS.md` 和 `docs/DEVELOPMENT.md`
- 新功能未实现或暂缓：同步 `docs/ROADMAP.md`

不要在多个文档中重复维护同一份详细规则。专项文档是规则源。

10. 开发方式

请先给出简短实现计划，再修改文件。实现时遵守：

- 不给出兼容性或补丁性绕路方案
- 不过度设计，选择满足需求的最短正确路径
- 不自行扩展我没有要求的业务逻辑
- 不改无关文件
- 不引入无关依赖
- 不改变推荐主链路，除非本次任务明确要求

允许修改范围：

```text
【在这里列出本次允许修改的文件或模块】
```

禁止修改范围：

```text
【在这里列出本次禁止修改的文件或模块，例如 SQL、推荐算法、数据库结构、前端依赖等】
```

如果你认为必须扩大修改范围，先说明原因，等待确认。

完成后根据改动范围执行验证：

后端代码变更：

```powershell
cd backend
mvn test
mvn package
```

前端代码变更：

```powershell
cd frontend
npm run build
```

如果涉及推荐展示，执行：

```powershell
cd frontend
node scripts/verifyRecommendPresentation.mjs
```

如果涉及算法可视化，执行：

```powershell
cd frontend
node scripts/verifyAlgorithmDemo.mjs
```

如果涉及对比、收藏、反馈，执行：

```powershell
cd frontend
node scripts/verifyStage11Features.mjs
```

文档变更至少执行：

```powershell
rg -n "毕设|本科毕设|毕业设计|论文|答辩|阶段 [0-9]|阶段[0-9]|MVP|第一档|第二档|第三档|docs/constraints|docs/reference|IMPLEMENTATION_TASKS|COMPLETED_PHASES" README.md AGENTS.md docs frontend/src
git diff --check
git status --short
```

如果修改了推荐预算、排序或分组规则，额外搜索：

```powershell
rg -n "budgetMin|budgetMax|预算下限|预算上限|预算区间|STRICT|RELAX_BUDGET|rankNo|totalScore" docs backend/src frontend/src
```

如果修改了图片展示，额外检查：

```powershell
rg -n "imageUrl|image_url|carImageSrc|fallbackCarImage|default-car" docs backend/src frontend/src
```

完成后汇总必须包含：

1. 本次修复或实现了什么。
2. 修改了哪些文件。
3. 是否影响 API、数据库、推荐算法、前端路由或文档。
4. 执行了哪些测试、构建、搜索或验证命令。
5. 验证结果是否通过。
6. 是否有未完成事项、风险或需要我确认的问题。
7. 是否操作过数据库、启动过服务或提交过 Git。
```
