# Cars Recommend System

基于主客观组合权重与 Pareto-TOPSIS 的可解释汽车购买推荐系统。

本项目的最高优先级是推荐算法及其工程化实现，而不是普通车型 CRUD 或简单条件筛选。系统主链路是：

```text
车型参数 -> 特征评分 -> 用户画像 -> 多指标决策排序 -> 推荐解释 -> 降级推荐 -> 推荐记录追溯
```

## 文档导航

- `docs/README.md`：`docs` 文档总目录索引，说明分类目录结构。
- `docs/constraints/CONSTRAINT_DOCUMENTS.md`：约束性文档清单，说明哪些文档是后续开发必须遵循的规则源。
- `docs/reference/REFERENCE_DOCUMENTS.md`：解释性文档清单，说明哪些文档只用于入口说明、交接、历史追溯或答辩材料。
- `docs/constraints/DEVELOPMENT_GUIDE.md`：新开发者开发指南，说明本地准备、启动、测试和新功能开发流程。
- `docs/constraints/SYSTEM_CONTRACTS.md`：系统规则合同，固定不能随意改动的算法、字段、数据库、API、前端和文档规则。
- `docs/reference/HANDOFF.md`：项目交接说明，帮助朋友快速接手当前项目状态和后续维护重点。
- `docs/constraints/DATABASE_INIT.md`：本地 MySQL 创建、重建和导入 120 条车型测试数据的说明。
- `docs/constraints/PROJECT_SPEC.md`：项目总览、范围边界、技术栈、功能优先级和验收口径。
- `docs/constraints/RECOMMENDATION_DESIGN.md`：推荐闭环概要设计、模块职责和边界。
- `docs/constraints/RECOMMENDATION_ALGORITHM_UPGRADE.md`：当前主算法详细文档，算法版本为 `pareto-topsis-v1`。
- `docs/constraints/RECOMMENDATION_ALGORITHM.md`：车型特征评分规则说明。
- `docs/constraints/DATABASE_DESIGN.md`：数据库表职责、关键字段和推荐追溯数据设计。
- `docs/constraints/API_DESIGN.md`：前后端接口分组、请求响应约定和核心接口字段。
- `docs/constraints/FRONTEND_DESIGN.md`：前端页面展示、交互规则、状态展示和样式规范。
- `docs/reference/IMPLEMENTATION_TASKS.md`：分阶段任务清单、最低完成标准、增强标准和里程碑。
- `docs/reference/COMPLETED_PHASES.md`：已完成阶段、验证结果、当前 MVP 状态和遗留事项。
- `AGENTS.md`：AI 编程代理和协作者必须遵守的项目级规则。

## 新开发者必读

接手开发前先读：

```text
docs/README.md
docs/constraints/CONSTRAINT_DOCUMENTS.md
docs/constraints/DEVELOPMENT_GUIDE.md
docs/constraints/SYSTEM_CONTRACTS.md
docs/reference/HANDOFF.md
docs/constraints/DATABASE_INIT.md
```

当前项目核心算法是 `pareto-topsis-v1`。推荐请求不包含推荐数量字段，反馈只进入统计，不自动影响权重或排序。详细算法公式不在 README 重复维护。

## 不纳入核心实现

以图搜车、Redis 缓存、Swagger/Knife4j 完整文档、Excel 批量导入、复杂权限系统、在线学习推荐和深度学习推荐模型仅作为论文展望。

前端视觉和交互规则以 `docs/constraints/FRONTEND_DESIGN.md` 为准。

## 本地配置与敏感信息

真实数据库账号、密码和其他敏感信息不要写入文档或提交到仓库。

仓库只保留示例配置：

```text
backend/src/main/resources/application-local.example.yml
```

本地真实配置放在：

```text
backend/src/main/resources/application-local.yml
```

并通过 `.gitignore` 忽略。文档和示例配置中只能使用 `your_mysql_password` 等占位符。

## 本地 MySQL 初始化

后端本地联调建议使用 MySQL 8。仓库提供独立初始化脚本，用于新机器或本地开发库需要重建时一次性完成建库、建表和导入当前 120 条车型测试数据。

前置条件：

- 已安装 MySQL 8，并能在 PowerShell 中执行 `mysql` 命令。
- 已创建本地真实配置文件 `backend/src/main/resources/application-local.yml`。
- `application-local.yml` 中的数据库名、用户名、密码和端口与本机 MySQL 一致。

初始化命令：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1
```

脚本会读取 `application-local.yml`，创建数据库，依次执行：

```text
backend/src/main/resources/db/schema.sql
backend/src/main/resources/db/seed-data.sql
```

如果本地库已有旧数据，需要删除并重建本地开发库：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -Recreate
```

如果后端已经用 `local` profile 启动，可以同时触发车型评分重算：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -RecalculateScores
```

如果不使用脚本，也可以手动执行 SQL：

```sql
CREATE DATABASE IF NOT EXISTS cars_recommend_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE cars_recommend_system;
SOURCE backend/src/main/resources/db/schema.sql;
SOURCE backend/src/main/resources/db/seed-data.sql;
```

初始化后必须执行全部车型评分重算，否则 `car_feature_score` 为空或不完整时，推荐候选可能不足：

```text
POST http://localhost:8080/api/admin/cars/scores/recalculate
```

更多说明见 `docs/constraints/DATABASE_INIT.md`。注意：`-Recreate` 只用于本地开发库，会删除本地需求、推荐记录、收藏和反馈等业务数据；不要对生产库或他人共享库执行。脚本不会打印数据库密码。

## 阶段 0 工程启动

后端：

```bash
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

健康检查：

```text
GET http://localhost:8080/api/health
```

前端：

```bash
cd frontend
npm install
npm run dev
```

前端开发服务默认运行在 `http://localhost:5173`，并通过 Vite proxy 将 `/api` 转发到后端 `http://localhost:8080`。
