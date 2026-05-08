# 开发指南

本文档说明当前工程的本地准备、启动、测试和新功能开发流程。

## 环境要求

- Java 17
- Maven
- Node.js
- npm
- MySQL 8
- PowerShell

## 本地配置

后端本地联调使用 `local` profile。本机真实配置文件路径：

```text
backend/src/main/resources/application-local.yml
```

仓库只保留示例配置：

```text
backend/src/main/resources/application-local.example.yml
```

首次准备本地配置：

```powershell
Copy-Item backend/src/main/resources/application-local.example.yml backend/src/main/resources/application-local.yml
```

然后将 `application-local.yml` 中的数据库名、用户名、密码、端口改成本机 MySQL 配置。该文件不能提交。

## 数据库初始化

本地开发库初始化脚本：

```text
scripts/init-dev-db.ps1
```

初始化本地开发库：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1
```

确认可以删除本地旧数据后重建开发库：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -Recreate
```

导入数据后需要启动后端并执行全部车型评分重算：

```text
POST http://localhost:8080/api/admin/cars/scores/recalculate
```

如果后端已经启动，也可以让脚本在导入后触发评分重算：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -RecalculateScores
```

`-Recreate` 会删除本地需求、推荐记录、车型对比、收藏、反馈、评分和图片资源等业务数据。不要对真实库或他人共享库执行，除非已经明确确认。

## 后端启动

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

健康检查：

```text
GET http://localhost:8080/api/health
```

## 前端启动

```powershell
cd frontend
npm install
npm run dev
```

前端开发服务默认运行在 `http://localhost:5173`，并通过 Vite proxy 将 `/api` 转发到 `http://localhost:8080`。

## 常用测试命令

后端：

```powershell
cd backend
mvn test
mvn package
```

指定测试：

```powershell
cd backend
mvn test "-Dtest=RecommendationControllerTest"
mvn test "-Dtest=AuthControllerTest"
```

非认证专项 Controller 测试可以设置 `app.auth.enabled=false`，避免测试目标被登录细节污染。认证专项测试必须启用 `app.auth.enabled=true`，并使用真实登录 token 或测试辅助 token 覆盖 401 / 403 场景。

前端：

```powershell
cd frontend
npm run build
node scripts/verifyRecommendPresentation.mjs
node scripts/verifyAlgorithmDemo.mjs
node scripts/verifyStage11Features.mjs
node scripts/verifyAdminExperience.mjs
```

文档变更检查：

```powershell
git diff --check
```

## 新功能开发流程

1. 先确认需求是否属于当前系统边界。
2. 查阅 `AGENTS.md` 和对应专项文档。
3. 若需求与当前规则冲突，先说明冲突点并等待确认。
4. 先更新需要变更的文档。
5. 再修改代码、数据库脚本或前端页面。
6. 补充或更新测试。
7. 执行与改动范围匹配的验证命令。
8. 用 `git status --short` 检查工作区。

推荐算法、接口、数据库和前端展示互相关联。修改任一处时，需要同步检查：

- `docs/API.md`
- `docs/DATABASE.md`
- `docs/RECOMMENDATION.md`
- `docs/FRONTEND.md`
- 后端测试
- 前端构建或验证脚本

## 开发规则

- 推荐相关逻辑必须在后端 Service 层实现。
- 前端不得重新计算或重新排序推荐结果。
- 推荐结果展示排序以后端 `rankNo` 为准。
- 推荐历史读取保存快照，不重新计算覆盖。
- 自然语言解析后端接口保留，但当前产品前端不展示入口；它不直接保存需求或生成推荐。
- 算法可视化接口只读，不写数据库。
- 用户端接口身份来自 JWT 当前 `USER`，管理端接口身份来自 JWT 当前 `ADMIN`；不要在前端或请求参数中依赖默认 `userId` / `admin.id`。
- 用户级车型对比必须写入 `/api/user/compare` 和 `user_compare_car`，不得使用固定 localStorage key 保存车型 ID，不得通过 `userId` 参数操作他人对比。
- 普通用户注册只能创建 `USER`，`/login` 只处理普通用户登录，登录和注册成功默认进入首页 `/`；`/admin/login` 只处理管理员登录。
- 当前购车需求字段以 `budgetMin`、`budgetMax`、`brands`、`bodyTypes`、`energyTypes`、`seatOptions`、`scenes`、`factorWeights` 为主；`minSeats`、`excludedBrands`、`excludedCarIds` 是兼容字段。
- 管理员默认进入 `/admin/cars`，不显示首页入口；管理员用户管理只能维护 `app_user.status` 和查看用户数据，不得隐式生成推荐或改变推荐排序。
- 管理端收藏车型和反馈记录第一版只读，不允许取消收藏、删除反馈或代用户操作。
- 收藏不影响推荐排序。
- 反馈只进入统计，不做在线学习。
- 不引入与当前需求无关的复杂依赖。

## 提交前检查

提交前先查看工作区：

```powershell
git status --short
```

不要提交：

- `backend/src/main/resources/application-local.yml`
- `.env`
- `.env.local`
- 真实密码、token、密钥
- `target/`
- `dist/`
- `node_modules/`
- `.run/`
- 日志文件

只改 Markdown 文档时，不需要执行 `mvn test`、`mvn package` 或 `npm run build`，但应执行文本搜索和 `git diff --check`。
