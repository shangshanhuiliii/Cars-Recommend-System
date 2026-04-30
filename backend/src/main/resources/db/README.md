# 数据库脚本说明

阶段 1 只建立核心表和测试数据基础，不预置车型评分结果或推荐结果。

- `schema.sql`：MySQL 8 兼容核心表结构，包含 `app_user`、`admin`、`car_model`、`car_param`、`car_feature_score`、`user_demand`、`recommend_record`、`recommend_item`。
- `seed-data.sql`：默认演示用户、默认演示管理员、20 条车型基础数据和对应参数。
- `car_model.energy_type` 只允许 `燃油`、`纯电`、`插混`、`增程`；`新能源` 只允许出现在用户需求侧。
- `car_feature_score` 不插入种子评分，后续阶段必须由评分规则引擎根据参数计算。
- `recommend_item` 不插入种子推荐明细，后续阶段必须由推荐算法生成 `total_score`、`tags`、理由和不足快照。

测试数据仅用于开发、联调和答辩流程验证，不代表真实市场结论。
