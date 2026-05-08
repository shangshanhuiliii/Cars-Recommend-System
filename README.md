# Cars Recommend System

Cars Recommend System 是一个可运行、可继续维护的汽车购买推荐系统。系统基于车型参数、车型特征评分、用户结构化购车需求和 `pareto-topsis-v1` 推荐算法，输出可解释、可追溯的车型推荐结果。

推荐主链路：

```text
车型参数 -> 特征评分 -> 用户画像 -> 多维匹配 -> 推荐解释 -> 补充推荐 -> 推荐记录追溯
```

## 核心能力

- 车型基础信息、参数和特征评分维护。
- 结构化购车需求表单，支持预算、品牌正向筛选、车型级别、动力、座位选项、场景和九维偏好。
- 用户显式偏好权重与场景权重生成。
- 主客观组合权重 + Pareto-TOPSIS 推荐算法，版本为 `pareto-topsis-v1`。
- 推荐结果包含 `totalScore`、`rankNo`、推荐标签、推荐理由、不足提醒和维度评分。
- 推荐历史读取 `recommend_record` 与 `recommend_item` 快照，不重新计算覆盖历史结果。
- 后端保留自然语言解析接口；当前产品前端使用结构化表单作为主推荐入口，不展示自然语言解析入口。
- 用户级后端持久化车型对比、收藏、反馈和管理端运营概览。
- `/algorithm-demo` 算法可视化页面以只读方式展示推荐快照中的算法过程。
- 用户注册、普通用户登录、管理员独立登录、JWT 鉴权、当前身份识别、USER / ADMIN 接口权限和菜单权限。
- 管理端用户管理可查看普通用户状态、统计数字、最近需求和推荐入口，并支持启用 / 禁用普通用户。
- 管理端提供只读“收藏车型”和“反馈记录”页面，运营概览聚合用户、车型、推荐、收藏和反馈指标。

## 技术栈

- 后端：Java 17、Spring Boot 3、Maven、MyBatis-Plus、MySQL 8。
- 前端：Vue 3、Vite、Element Plus、Axios、Vue Router、Pinia。
- 本地脚本：PowerShell 数据库初始化脚本 `scripts/init-dev-db.ps1`。

## 快速启动

准备环境：

- Java 17
- Maven
- Node.js 与 npm
- MySQL 8

创建本地敏感配置：

```powershell
Copy-Item backend/src/main/resources/application-local.example.yml backend/src/main/resources/application-local.yml
```

编辑 `backend/src/main/resources/application-local.yml`，写入本机 MySQL 连接信息。该文件不能提交到 Git。

本地 seed 默认账号（注册入口只能创建普通 USER 账号）：

| 类型 | 用户名 | 密码 |
| --- | --- | --- |
| 普通用户 | `demo_user` | `demo123456` |
| 管理员 | `demo_admin` | `admin123456` |

JWT 配置位于 `app.auth`，`jwt-secret` 至少 32 bytes；生产环境必须使用独立密钥，不要提交真实密钥。管理员禁用用户后，该用户不能再次登录；当前轻量 JWT 不维护服务端黑名单，已签发 token 在过期前仍可能有效。

前端登录入口：

- `/login`：普通用户登录，不展示默认测试账号，保留注册入口，登录成功默认进入首页 `/`。
- `/admin/login`：管理员登录，不展示注册入口，登录后默认进入 `/admin/cars`。
- `/register`：普通用户注册，只创建 USER 账号，注册成功默认进入首页 `/`。

初始化本地开发库：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1
```

后端启动：

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

健康检查：

```text
GET http://localhost:8080/api/health
```

前端启动：

```powershell
cd frontend
npm install
npm run dev
```

前端开发服务默认运行在 `http://localhost:5173`，并通过 Vite proxy 将 `/api` 转发到 `http://localhost:8080`。

数据库导入后需要执行全部车型评分重算，否则 `car_feature_score` 为空时推荐候选不足：

```text
POST http://localhost:8080/api/admin/cars/scores/recalculate
```

## 文档入口

- `docs/README.md`：文档目录。
- `docs/DEVELOPMENT.md`：本地开发、测试和新功能流程。
- `docs/ARCHITECTURE.md`：当前系统架构、模块边界和数据流。
- `docs/API.md`：当前接口、响应结构和核心字段。
- `docs/DATABASE.md`：当前数据库表、字段、快照和初始化说明。
- `docs/RECOMMENDATION.md`：推荐算法、评分规则、排序和解释规则。
- `docs/FRONTEND.md`：前端页面、路由、交互和展示规范。
- `docs/OPERATIONS.md`：运行、配置、健康检查和常见问题。
- `docs/ROADMAP.md`：未实现功能和后续计划。
- `AGENTS.md`：AI 代理和协作者规则。

## 安全规则

不要提交以下内容：

- `backend/src/main/resources/application-local.yml`
- `.env`、`.env.local`
- 真实数据库密码、token、密钥
- `target/`、`dist/`、`node_modules/`

真实 MySQL 的迁移、重建、清空或删除数据必须先获得明确确认。当前未实现功能统一维护在 `docs/ROADMAP.md`。
