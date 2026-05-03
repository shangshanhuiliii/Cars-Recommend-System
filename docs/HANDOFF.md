# 项目交接说明

本文档给接手项目的开发者快速了解当前状态、运行方式、已知边界和后续建议。具体开发规则见 `DEVELOPMENT_GUIDE.md` 和 `SYSTEM_CONTRACTS.md`。

## 1. 当前完成状态

当前已完成阶段 0-11 的主要能力：

- 工程骨架、数据库和种子数据。
- 车型管理、参数维护和车型特征评分。
- 用户需求保存、用户画像和推荐主链路。
- 推荐算法升级为 `pareto-topsis-v1`。
- 推荐解释、降级推荐和历史快照追溯。
- 用户端推荐流程。
- 管理端车型、推荐记录和统计页面。
- 算法可视化答辩页。
- 自然语言辅助填写。
- 车型对比、收藏和用户反馈。
- 本地 MySQL 联调已在阶段记录中验证过；新机器仍需重新配置本地数据库。

## 2. 当前核心亮点

- 多维车型特征评分。
- 用户主观权重。
- 熵权法客观权重。
- 主客观组合权重。
- Pareto 非支配辅助排序。
- TOPSIS 推荐分。
- 推荐理由和不足提醒。
- 历史快照追溯。
- 算法可视化答辩页。

这些能力共同证明系统不是普通车型筛选列表，而是可解释、可降级、可追溯的汽车购买推荐系统。

## 3. 当前运行方式

后端启动：

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

前端启动：

```powershell
cd frontend
npm run dev
```

本地数据库配置：

```text
backend/src/main/resources/application-local.yml
```

不要把真实数据库密码写入仓库。示例配置参考：

```text
backend/src/main/resources/application-local.example.yml
```

数据库初始化脚本：

```text
backend/src/main/resources/db/schema.sql
backend/src/main/resources/db/seed-data.sql
```

新机器或本地库需要重建时，优先使用独立初始化脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1
```

如果本地库已有旧数据并确认可以删除，使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -Recreate
```

初始化后执行全部车型评分重算：

```text
POST /api/admin/cars/scores/recalculate
```

常用访问路径：

- 后端健康检查：`GET /api/health`
- 前端开发服务默认：`http://localhost:5173`
- 后端接口默认：`http://localhost:8080/api`

## 4. 默认数据和账号

- 默认演示用户：`app_user.id = 1`
- 默认演示管理员：`admin.id = 1`
- 当前 `seed-data.sql` 包含 120 条车型基础数据和 120 条车型参数数据。
- `car_model.energy_type` 不含 `新能源`，只保存 `燃油 / 纯电 / 插混 / 增程`。
- `新能源` 只作为用户需求侧宽泛动力偏好。

## 5. 常用页面

- `/`
- `/recommend`
- `/recommend/result/:recordId`
- `/algorithm-demo`
- `/compare`
- `/favorites`
- `/history`
- `/admin/cars`
- `/admin/recommend-records`
- `/admin/dashboard`

## 6. 常用接口

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

## 7. 已知边界

- 自然语言解析是规则词典和正则表达式实现，只作为表单草稿辅助。
- 车型数据是开发测试数据，不代表真实市场结论。
- 反馈不进入在线学习，不自动更新权重、车型评分或推荐排序。
- 当前不是深度学习推荐，也不是协同过滤推荐。
- 图片仍主要使用默认占位或已有静态资源。
- 本地真实数据库需要自行配置 `application-local.yml`。
- 若 schema 变化，需要先确认迁移或重建方式。

## 8. 后续建议

建议优先做：

- 手工验收。
- UI 细节打磨。
- 数据质量完善。
- 论文和答辩材料。
- 小 bug 修复。

不建议优先做：

- 再重构推荐算法。
- 引入复杂权限。
- 引入大模型。
- 引入在线学习。
- 大规模改数据库。

## 9. 接手开发流程

1. 先读 `DEVELOPMENT_GUIDE.md`。
2. 再读 `SYSTEM_CONTRACTS.md`。
3. 确认要改的功能属于哪个专项文档。
4. 先改文档，再写代码。
5. 跑测试。
6. 提交前检查敏感文件和临时文件。
