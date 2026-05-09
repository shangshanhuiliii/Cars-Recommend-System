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

认证配置位于 `app.auth`：

```yaml
app:
  auth:
    enabled: true
    jwt-secret: change-this-local-development-secret-please-keep-at-least-32-bytes
    token-expire-seconds: 7200
```

`jwt-secret` 至少 32 bytes，本地开发可以使用示例值或自行覆盖；生产环境必须使用独立密钥并保存在本地配置或环境变量中。`token-expire-seconds` 默认 7200 秒。

普通用户注册只创建 `USER` 账号，密码以 PBKDF2 hash 写入 `app_user.password`，账号状态默认为 `ACTIVE`。管理员将用户状态改为 `DISABLED` 后，该用户不能再次登录；当前轻量 JWT 不维护服务端黑名单，已签发 token 在过期前仍可能可用，如需立即失效需后续增加 tokenVersion 或 token blacklist。

前端登录入口统一为首页右上角登录 / 注册弹窗：后端通过 `POST /api/auth/login` 根据账号密码识别普通用户或管理员。普通用户登录或注册成功默认进入首页 `/`；管理员登录成功默认进入 `/admin/cars`。旧 `/login` 和 `/admin/login` URL 保留兼容，访问时重定向到首页并打开登录弹窗；管理员界面不展示首页入口，访问首页会重定向到车型管理。

注册表单会保存普通用户邮箱到 `app_user.email`，用于后续找回密码能力基础。本阶段忘记密码入口只展示占位提示，不发送邮件、不重置密码，也不需要 SMTP 配置或 Redis。

不要提交：

- `backend/src/main/resources/application-local.yml`
- `.env`
- `.env.local`
- 真实密码、token、密钥

## 本地图片存储

车型图片资源默认使用本地文件系统存储。安全默认配置在 `backend/src/main/resources/application.yml` 中：

```yaml
app:
  storage:
    car-image-root: .run/uploads/car-images
    car-image-public-path: /uploads/car-images
    car-image-max-size-bytes: 5242880
    car-image-max-edge: 1600
    car-image-jpeg-quality: 0.82
```

说明：

- `app.storage.car-image-root` 是压缩/缩放后图片文件的存储目录，可在 `application-local.yml` 中按本机路径覆盖。
- `.run/` 已被 Git 忽略，不提交上传文件。
- 后端通过 Spring MVC 静态资源映射暴露 `/uploads/car-images/{storedFilename}`。
- 管理端上传资源默认状态为 `PENDING`，审核通过后才会更新 `car_model.image_url`。
- 当前不需要也不允许提交真实对象存储密钥；云对象存储、CDN 和迁移策略属于后续增强。

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

`-Recreate` 会删除目标库中的需求、推荐记录、对比、收藏、反馈、评分和图片资源等数据。只对自己的本地开发库使用；真实库、共享库或生产库必须先明确确认。

## 种子数据

`seed-data.sql` 当前包含：

- 本地默认普通用户 `demo_user / demo123456`，`status = ACTIVE`，密码以 PBKDF2 hash 保存。
- 本地默认管理员 `demo_admin / admin123456`，`role = ADMIN`，密码以 PBKDF2 hash 保存。
- 120 条车型基础数据。
- 120 条车型参数数据。

种子数据不包含：

- 车型评分。
- 推荐记录。
- 推荐明细。
- 用户车型对比。
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
- `/login`（兼容跳转到首页登录弹窗）
- `/recommend`
- `/recommend/result/:recordId`
- `/history`
- `/compare`
- `/favorites`
- `/features`
- `/admin/login`（兼容跳转到首页登录弹窗）
- `/admin/favorites`
- `/admin/feedbacks`
- `/algorithm-demo`
- `/admin/cars`
- `/admin/recommend-records`
- `/admin/dashboard`

## 常用接口

- 健康检查：`GET /api/health`
- 统一登录：`POST /api/auth/login`
- 普通用户登录兼容接口：`POST /api/auth/user/login`
- 管理员登录兼容接口：`POST /api/auth/admin/login`
- 当前身份：`GET /api/auth/me`
- 管理端图片资源：`POST /api/admin/car-images`、`GET /api/admin/car-images`、`PUT /api/admin/car-images/{id}/audit`、`DELETE /api/admin/car-images/{id}`
- 用户需求保存：`POST /api/user/demand`
- 自然语言解析：`POST /api/user/demand/parse-text`，后端保留，当前产品前端不展示入口。
- 推荐生成：`POST /api/recommend/generate`
- 推荐详情：`GET /api/recommend/{recordId}`
- 推荐历史：`GET /api/recommend/history`
- 算法可视化：`GET /api/admin/recommend-records/{recordId}/algorithm-visualization`
- 车型详情：`GET /api/car/{id}`
- 首页车辆轮播：`GET /api/car/home-carousel?limit=6`
- 用户级车型对比：`GET /api/user/compare`、`POST /api/user/compare/{carId}`、`DELETE /api/user/compare/{carId}`、`DELETE /api/user/compare`
- 静态车型对比查询：`GET /api/car/compare`
- 收藏：`POST /api/user/favorites/{carId}`、`DELETE /api/user/favorites/{carId}`、`GET /api/user/favorites`
- 反馈：`POST /api/recommend/{recordId}/feedback`、`GET /api/recommend/{recordId}/feedback`
- 管理端推荐记录：`GET /api/admin/recommend-records`、`GET /api/admin/recommend-records/{recordId}`
- 管理端收藏车型：`GET /api/admin/favorites/cars`、`GET /api/admin/favorites/cars/{carId}/users`
- 管理端反馈记录：`GET /api/admin/feedbacks`
- 管理端运营概览：`GET /api/admin/stat/overview`

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
- 上传图片文件
- 日志文件
- 含真实密码、token、密钥的任何文件
