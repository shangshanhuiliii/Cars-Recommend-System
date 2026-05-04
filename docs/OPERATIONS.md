# 运行与运维说明

本文档说明当前系统在本地环境中的配置、启动、数据库初始化、健康检查和常见问题。

## 本地环境

需要准备：

- Java 17
- Maven
- Node.js
- npm
- MySQL 8
- PowerShell

服务默认端口：

| 服务 | 默认地址 |
| --- | --- |
| 后端 | `http://localhost:8080` |
| 前端 | `http://localhost:5173` |
| API 基础路径 | `http://localhost:8080/api` |

## 本地敏感配置

本地真实配置文件：

```text
backend/src/main/resources/application-local.yml
```

示例配置：

```text
backend/src/main/resources/application-local.example.yml
```

首次创建：

```powershell
Copy-Item backend/src/main/resources/application-local.example.yml backend/src/main/resources/application-local.yml
```

然后填写本机 MySQL 配置。真实密码只能保存在本地文件或环境变量中，不能提交到仓库。

不要提交：

- `backend/src/main/resources/application-local.yml`
- `.env`
- `.env.local`
- 真实密码、token、密钥

## 数据库初始化脚本

脚本路径：

```text
scripts/init-dev-db.ps1
```

脚本会读取 `application-local.yml` 中的 MySQL 连接信息，创建数据库，并执行：

```text
backend/src/main/resources/db/schema.sql
backend/src/main/resources/db/seed-data.sql
```

首次初始化：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1
```

重建本地开发库：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -Recreate
```

重建时跳过二次确认：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -Recreate -Force
```

手动指定连接参数：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 `
  -HostName localhost `
  -Port 3306 `
  -DatabaseName cars_recommend_system `
  -User root `
  -Recreate
```

脚本不会打印数据库密码。若缺少密码，会用安全输入提示。

`-Recreate` 会删除目标库中的需求、推荐记录、收藏、反馈和评分等数据。只对自己的本地开发库使用；真实库、共享库或生产库必须先明确确认。

## 种子数据

`seed-data.sql` 当前包含：

- 默认演示用户 `app_user.id = 1`。
- 默认演示管理员 `admin.id = 1`。
- 120 条车型基础数据。
- 120 条车型参数数据。

种子数据不包含：

- 车型评分。
- 推荐记录。
- 推荐明细。
- 收藏。
- 反馈。

导入后需要执行全部车型评分重算：

```text
POST http://localhost:8080/api/admin/cars/scores/recalculate
```

如果后端已经启动，可以让脚本自动触发评分重算：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -RecalculateScores
```

## 后端启动

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

常用验证：

```text
GET http://localhost:8080/api/health
```

健康检查返回后端运行状态和数据库连接状态。

## 前端启动

```powershell
cd frontend
npm install
npm run dev
```

默认访问：

```text
http://localhost:5173
```

Vite proxy 会将 `/api` 转发到 `http://localhost:8080`。

## 常用页面

- `/`
- `/recommend`
- `/recommend/result/:recordId`
- `/history`
- `/compare`
- `/favorites`
- `/algorithm-demo`
- `/admin/cars`
- `/admin/recommend-records`
- `/admin/dashboard`

## 常用接口

- 健康检查：`GET /api/health`
- 用户需求保存：`POST /api/user/demand`
- 自然语言解析：`POST /api/user/demand/parse-text`
- 推荐生成：`POST /api/recommend/generate`
- 推荐详情：`GET /api/recommend/{recordId}`
- 推荐历史：`GET /api/recommend/history`
- 算法可视化：`GET /api/recommend/{recordId}/algorithm-visualization`
- 车型详情：`GET /api/car/{id}`
- 车型对比：`GET /api/car/compare`
- 收藏：`POST /api/user/favorites/{carId}`、`DELETE /api/user/favorites/{carId}`、`GET /api/user/favorites`
- 反馈：`POST /api/recommend/{recordId}/feedback`、`GET /api/recommend/{recordId}/feedback`
- 管理端统计：`GET /api/admin/stat/overview`

## 常见问题

### PowerShell 提示不能执行脚本

使用文档中的命令：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1
```

该命令只对本次执行绕过策略，不修改系统全局策略。

### 提示找不到 `mysql`

说明 MySQL 客户端没有加入 `PATH`。安装 MySQL 8 客户端后重新打开 PowerShell，并验证：

```powershell
mysql --version
```

### 导入时出现 `Duplicate entry`

目标库里已经有固定 ID 的种子数据。确认可以删除本地库后使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -Recreate
```

### 推荐结果为空或候选很少

通常是 `car_feature_score` 为空。启动后端后执行：

```text
POST /api/admin/cars/scores/recalculate
```

### 健康检查显示数据库不可用

检查：

- MySQL 是否启动。
- `application-local.yml` 中数据库名、端口、用户名和密码是否正确。
- 本地数据库是否已创建。
- 后端是否使用 `local` profile 启动。

## 不应提交的文件

- `backend/src/main/resources/application-local.yml`
- `.env`
- `.env.local`
- `target/`
- `dist/`
- `node_modules/`
- `.run/`
- 日志文件
- 含真实密码、token、密钥的任何文件
