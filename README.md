# Cars Recommend System

基于多维偏好建模与可解释推荐的汽车购买推荐系统。

本项目的最高优先级是推荐算法及其工程化实现，而不是普通车型 CRUD 或简单条件筛选。系统主链路是：

```text
车型参数 -> 特征评分 -> 用户画像 -> 多维匹配 -> 推荐解释 -> 降级推荐 -> 推荐记录追溯
```

## 文档导航

- `docs/PROJECT_SPEC.md`：项目总览、范围边界、技术栈、功能优先级和验收口径。
- `docs/RECOMMENDATION_DESIGN.md`：推荐算法详细设计，包括车型评分、用户画像、匹配计算、解释生成和降级策略。
- `docs/DATABASE_DESIGN.md`：数据库表职责、关键字段和推荐追溯数据设计。
- `docs/API_DESIGN.md`：前后端接口分组、请求响应约定和核心接口字段。
- `docs/FRONTEND_DESIGN.md`：前端页面展示、交互规则、状态展示和样式规范。
- `docs/IMPLEMENTATION_TASKS.md`：分阶段任务清单、最低完成标准、增强标准和里程碑。
- `AGENTS.md`：AI 编程代理和协作者必须遵守的项目级规则。

## 不纳入核心实现

以图搜车、Redis 缓存、Swagger/Knife4j 完整文档、Excel 批量导入、复杂权限系统、在线学习推荐和深度学习推荐模型仅作为论文展望。

前端视觉和交互规则以 `docs/FRONTEND_DESIGN.md` 为准。

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

后端本地联调建议使用 MySQL 8。初始化流程只需要在本机创建数据库、执行表结构脚本和种子数据脚本，不要把真实账号密码提交到仓库。

1. 创建空数据库：

```sql
CREATE DATABASE IF NOT EXISTS cars_recommend_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

2. 在 MySQL 客户端中执行初始化脚本：

```sql
USE cars_recommend_system;
SOURCE backend/src/main/resources/db/schema.sql;
SOURCE backend/src/main/resources/db/seed-data.sql;
```

如果从 `backend` 目录启动 MySQL 客户端，也可以使用：

```sql
USE cars_recommend_system;
SOURCE src/main/resources/db/schema.sql;
SOURCE src/main/resources/db/seed-data.sql;
```

`schema.sql` 使用 `CREATE TABLE IF NOT EXISTS`，可重复检查表结构；`seed-data.sql` 面向空库初始化，包含固定 ID 的演示用户、管理员和 20 条车型数据，不建议在已有同 ID 数据的库中重复执行。

3. 创建本地真实配置文件：

```text
backend/src/main/resources/application-local.yml
```

内容参考 `backend/src/main/resources/application-local.example.yml`，将 `your_mysql_password` 替换为本机密码。该文件已被 `.gitignore` 忽略，不能提交。

4. 使用 local profile 启动后端：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

健康检查：

```text
GET http://localhost:8080/api/health
```

## 阶段 0 工程启动

后端：

```bash
cd backend
mvn spring-boot:run
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
