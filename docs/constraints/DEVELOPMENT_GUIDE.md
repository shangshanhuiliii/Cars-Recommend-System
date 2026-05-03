# 开发指南

本文档面向后续接手开发者，说明如何继续维护本项目。当前开发规则以本文档和 `SYSTEM_CONTRACTS.md` 为准；详细接口、数据库、算法和前端规则仍分别查阅专项文档。

## 1. 项目当前状态

当前项目核心能力已经基本完成：

- 推荐主链路：车型参数、特征评分、用户画像、推荐计算、解释生成、降级推荐、历史追溯和前端展示已贯通。
- 当前主算法：`pareto-topsis-v1`，即基于主客观组合权重与 Pareto-TOPSIS 的可解释汽车推荐算法。
- 用户端推荐流程：购车需求页、推荐结果页、车型详情页和推荐历史页已完成。
- 算法可视化答辩页：`/algorithm-demo` 使用只读接口展示推荐快照对应的算法过程。
- 自然语言辅助填写：规则词典式解析用户一句话需求，回填结构化表单，由用户确认后再生成推荐。
- 车型对比：支持 1-3 款车型横向对比。
- 收藏：支持演示用户收藏、取消收藏和收藏列表。
- 用户反馈：支持推荐记录满意度和原因标签反馈。
- 管理端统计：支持需求、推荐、热门车型和反馈相关统计。

当前不建议继续大幅重构推荐算法。后续优先做小范围 UI 打磨、数据质量完善、论文材料整理和明确 bug 修复。确实需要调整算法时，必须先更新 `RECOMMENDATION_ALGORITHM_UPGRADE.md` 和 `RECOMMENDATION_IMPLEMENTATION_LOGIC.md`，再补测试和实现。

## 2. 本地开发准备

基础环境建议：

- Java 17
- Maven
- Node.js 和 npm
- MySQL 8

后端本地联调使用 `local` profile。本地真实敏感配置放在：

```text
backend/src/main/resources/application-local.yml
```

该文件用于配置本机数据库账号、密码等敏感信息，不能提交到仓库。仓库只保留示例配置：

```text
backend/src/main/resources/application-local.example.yml
```

数据库初始化脚本：

```text
backend/src/main/resources/db/schema.sql
backend/src/main/resources/db/seed-data.sql
```

本地开发库可使用独立初始化脚本重建：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1
```

如果后端已经启动，可以追加 `-RecalculateScores` 让脚本在导入数据后调用评分重算接口。否则初始化后需要手动执行全部车型评分重算，否则推荐候选可能因为缺少 `car_feature_score` 无法进入推荐。

## 3. 启动方式

后端：

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

前端：

```powershell
cd frontend
npm run dev
```

当前提供的是数据库初始化脚本 `scripts/init-dev-db.ps1`，不是后端和前端一键启动脚本。后端和前端仍按上面的命令分别启动。

## 4. 文档阅读顺序

新开发者建议按以下顺序阅读：

1. `README.md`
2. `AGENTS.md`
3. `docs/README.md`
4. `docs/constraints/CONSTRAINT_DOCUMENTS.md`
5. `docs/reference/REFERENCE_DOCUMENTS.md`
6. `docs/constraints/DEVELOPMENT_GUIDE.md`
7. `docs/constraints/SYSTEM_CONTRACTS.md`
8. `docs/reference/HANDOFF.md`
9. `docs/constraints/DATABASE_INIT.md`
10. `docs/constraints/API_DESIGN.md`
11. `docs/constraints/DATABASE_DESIGN.md`
12. `docs/constraints/RECOMMENDATION_IMPLEMENTATION_LOGIC.md`
13. `docs/constraints/FRONTEND_DESIGN.md`

需要查算法公式时再读 `docs/constraints/RECOMMENDATION_ALGORITHM_UPGRADE.md`；需要查阶段历史时再读 `docs/reference/IMPLEMENTATION_TASKS.md` 和 `docs/reference/COMPLETED_PHASES.md`。

## 5. 代码结构说明

仓库主要目录：

- `backend`：Spring Boot 后端。
- `frontend`：Vue 3 前端。
- `docs`：项目规格、接口、数据库、算法、前端和交接文档。
- `backend/src/main/resources/db/schema.sql`：数据库表结构初始化脚本。
- `backend/src/main/resources/db/seed-data.sql`：演示种子数据初始化脚本，当前包含 120 条车型和对应参数。
- `scripts/init-dev-db.ps1`：本地 MySQL 建库、建表和导入种子数据脚本。

后端主要分层：

- `controller`：接收请求、参数校验、统一响应。
- `service`：业务逻辑和推荐算法。
- `mapper`：数据访问。
- `entity`：数据库实体。
- `dto`：请求对象。
- `vo`：响应对象。
- `common`：统一响应、异常处理、分页对象。
- `config`：跨域、MyBatis-Plus、认证等配置。

前端主要结构：

- `views`：页面视图。
- `api`：接口封装。
- `router`：路由。
- `utils`：工具函数。
- `styles`：全局样式。
- `scripts`：前端验证脚本。

## 6. 推荐算法开发规则

- 当前主算法是 `pareto-topsis-v1`。
- 不要随意修改推荐算法、排序规则、权重生成规则或降级阶段。
- 修改推荐算法前必须先更新设计文档，再写代码。
- 修改推荐算法必须补充或更新测试。
- 历史推荐必须读取 `recommend_record` 和 `recommend_item` 快照，不重新计算覆盖历史结果。
- 前端不能重新排序推荐结果，必须按后端 `rankNo` 展示。
- 推荐生成请求不包含推荐数量字段，后端按候选集、分组和排序规则返回结果。

## 7. API 开发规则

- 接口约定以 `docs/constraints/API_DESIGN.md` 为准。
- 所有接口使用统一响应结构。
- 默认演示用户为 `app_user.id = 1`。
- 新接口必须补测试。
- 用户需求 API 使用 `bodyTypes`、`energyTypes`、`scenes`、`factorWeights`、`minSeats`、`budgetMin`、`budgetMax`、`excludedBrands` 和 `excludedCarIds`。
- 只读接口不能写库，例如算法可视化接口。
- 自然语言解析接口只生成表单草稿，不保存需求、不生成推荐。

## 8. 数据库开发规则

- 数据库结构以 `docs/constraints/DATABASE_DESIGN.md` 和 `schema.sql` 为准。
- 新增表必须同步更新 `DATABASE_DESIGN.md`。
- 新增或修改表必须更新 `DatabaseSchemaSeedTest`。
- 真实 MySQL 迁移、重建、清空或删除数据前必须先说明影响并获得确认。
- `application-local.yml` 不得提交。
- `priceScore` 不写入 `car_feature_score`，只在推荐阶段动态计算。

## 9. 前端开发规则

- 前端体验以 `docs/constraints/FRONTEND_DESIGN.md` 为准。
- 用户端保持简洁，不展示 TOPSIS、Pareto、熵权等复杂算法术语。
- 管理端可以展示推荐追溯信息。
- 算法可视化页可以展示算法细节，但必须只读。
- 普通操作反馈不使用顶部 toast，使用页面内状态表达。
- 前端不重新排序推荐结果。
- 不引入新依赖，除非先说明必要性并获得确认。

## 10. 测试和验证

常用后端命令：

```powershell
cd backend
mvn test
mvn package
```

常用前端命令：

```powershell
cd frontend
npm run build
node scripts/verifyRecommendPresentation.mjs
node scripts/verifyAlgorithmDemo.mjs
node scripts/verifyStage11Features.mjs
```

验证原则：

- 改后端跑后端测试。
- 改前端跑前端构建和相关验证脚本。
- 改数据库跑数据库相关测试。
- 改推荐算法跑全量测试。
- 本次只改文档时，不需要执行 `mvn test`、`mvn package` 或 `npm run build`。

## 11. 新功能开发流程

1. 先看 `SYSTEM_CONTRACTS.md`。
2. 查对应专项文档是否已有规则。
3. 如果需求、建议或实现方式与规则合同冲突，先讨论，不直接写代码。
4. 先更新文档。
5. 再写代码。
6. 补测试。
7. 跑验证。
8. 更新 `COMPLETED_PHASES.md`。
9. 再提交。

## 12. 提交前检查

提交前先看工作区：

```powershell
git status --short
```

不要提交：

- `target/`
- `dist/`
- `node_modules/`
- `.run/`
- 日志文件
- `application-local.yml`
- 真实数据库密码或其他敏感信息
