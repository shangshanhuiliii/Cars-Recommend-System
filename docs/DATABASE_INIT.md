# 本地数据库初始化说明

本文档说明新接手者如何在本机创建 MySQL 数据库，并导入当前项目的测试数据。当前测试数据来源为：

```text
backend/src/main/resources/db/seed-data.sql
```

该文件当前包含：

- 默认演示用户 `app_user.id = 1`
- 默认演示管理员 `admin.id = 1`
- 120 条车型基础数据
- 120 条车型参数数据

`seed-data.sql` 不预置车型评分、不预置推荐记录、不预置收藏或反馈。车型评分必须在数据库初始化后由后端评分接口重算。

## 1. 前置条件

本机需要准备：

- MySQL 8
- PowerShell
- 可在 PowerShell 中执行 `mysql`
- 本地真实配置文件 `backend/src/main/resources/application-local.yml`

如果没有本地配置文件，先复制示例：

```powershell
Copy-Item backend/src/main/resources/application-local.example.yml backend/src/main/resources/application-local.yml
```

然后把 `application-local.yml` 中的数据库用户名、密码、端口和数据库名改成本机 MySQL 配置。真实密码只写在本地文件中，不提交 Git。

默认数据库名：

```text
cars_recommend_system
```

## 2. 一键初始化脚本

脚本位置：

```text
scripts/init-dev-db.ps1
```

第一次在新机器上创建数据库并导入数据：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1
```

脚本会：

1. 读取 `backend/src/main/resources/application-local.yml` 中的 MySQL 连接信息。
2. 如果缺少密码，使用安全输入提示用户输入。
3. 创建数据库。
4. 执行 `schema.sql` 建表。
5. 执行 `seed-data.sql` 导入 120 条车型测试数据和对应参数。
6. 输出 `app_user`、`admin`、`car_model`、`car_param`、`car_feature_score` 的行数。

## 3. 重建本地开发库

如果本地库已经有旧数据，直接导入种子数据可能因为固定 ID 冲突失败。需要重建本地开发库时使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -Recreate
```

脚本会要求输入数据库名确认后才删除并重建。

注意：`-Recreate` 会删除本地需求、推荐记录、收藏、反馈和评分等数据。只对自己的本地开发库使用，不要对他人的共享库或生产库使用。

如果已经确认目标库可以删除，并希望跳过二次确认：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -Recreate -Force
```

## 4. 手动指定连接参数

如果不想依赖 `application-local.yml`，可以在命令中指定：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 `
  -HostName localhost `
  -Port 3306 `
  -DatabaseName cars_recommend_system `
  -User root `
  -Recreate
```

脚本会提示输入 MySQL 密码，不会把密码打印到终端。

## 5. 初始化后重算车型评分

导入种子数据后，`car_feature_score` 仍为空。必须启动后端后执行全部车型评分重算：

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

后端启动后调用：

```text
POST http://localhost:8080/api/admin/cars/scores/recalculate
```

如果后端已经启动，也可以让脚本在导入后自动调用评分重算接口：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -Recreate -RecalculateScores
```

评分重算后，`car_feature_score` 应生成与车型数量一致的评分记录。推荐生成依赖这些评分记录。

## 6. 验证数据是否导入成功

可以用 MySQL 客户端检查：

```sql
USE cars_recommend_system;
SELECT COUNT(*) FROM app_user;
SELECT COUNT(*) FROM admin;
SELECT COUNT(*) FROM car_model;
SELECT COUNT(*) FROM car_param;
SELECT COUNT(*) FROM car_feature_score;
```

初始化完成但未重算评分时，预期：

```text
app_user = 1
admin = 1
car_model = 120
car_param = 120
car_feature_score = 0
```

执行全部车型评分重算后：

```text
car_feature_score = 120
```

## 7. 当前数据边界

- 120 条车型数据是开发、联调和答辩演示数据，不代表真实市场结论。
- `car_model.energy_type` 只保存 `燃油 / 纯电 / 插混 / 增程`。
- `新能源` 只作为用户需求侧宽泛动力偏好，后端展开为 `纯电 / 插混 / 增程`。
- `priceScore` 不在种子数据中导入，只在推荐阶段动态计算。
- 推荐历史、收藏和反馈不通过种子数据预置，应由系统功能生成。

## 8. 常见问题

### PowerShell 提示不能执行脚本

使用本项目文档中的命令：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1
```

该命令只对本次执行绕过策略，不修改系统全局策略。

### 提示找不到 mysql

说明 MySQL 客户端没有加入 `PATH`。安装 MySQL 8 客户端后，重新打开 PowerShell，再执行：

```powershell
mysql --version
```

### 导入时出现 Duplicate entry

目标库里已经有固定 ID 的种子数据。确认可以删除本地库后，使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -Recreate
```

### 推荐结果为空或候选很少

通常是 `car_feature_score` 为空。启动后端后执行全部车型评分重算。
