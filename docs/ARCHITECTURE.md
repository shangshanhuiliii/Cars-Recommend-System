# 系统架构

## 系统定位

Cars Recommend System 是面向普通购车用户的汽车购买决策辅助平台。系统通过用户预算、车型偏好、动力偏好、使用场景、最低座位数和九维显式权重生成用户画像，再结合车型参数和车型特征评分，计算并保存可解释的推荐结果。

系统不涉及下单、支付、贷款、保险、经销商结算、营销活动或真实成交价预测。

## 推荐主链路

```text
车型参数 -> 特征评分 -> 用户画像 -> 多维匹配 -> 推荐解释 -> 补充推荐 -> 推荐记录追溯
```

链路职责：

- `car_model` 与 `car_param` 提供车型基础数据和配置参数。
- `car_feature_score` 保存八维车型静态评分。
- `user_demand` 保存结构化购车需求、画像文本和主观权重。
- 推荐服务动态计算 `priceScore`，生成主客观组合权重，并用 Pareto-TOPSIS 计算 `totalScore`。
- `recommend_record` 和 `recommend_item` 保存推荐快照，供历史详情、管理端和 `/algorithm-demo` 读取。

## 后端分层

```text
controller   接收请求、参数校验、统一响应
service      业务逻辑和推荐算法
mapper       数据访问
entity       数据库实体
dto          请求对象
vo           响应对象
common       统一响应、异常处理、分页对象
config       跨域、MyBatis-Plus、认证配置
util         评分、解析、权重归一化等工具
```

推荐算法、画像生成、候选过滤、权重计算、排序、解释和快照保存均由后端 Service 层负责。Controller 只处理接口入口和参数校验，前端只负责展示。

## 后端模块边界

- 车型管理：维护 `car_model`、`car_param`，并提供车型详情、品牌选项和车型选项。
- 车型评分：根据车型参数生成 `car_feature_score`。
- 用户需求：保存结构化需求，生成画像文本和主观权重。
- 自然语言解析：解析文本并返回表单草稿，不保存需求、不生成推荐。
- 推荐生成：加载需求和候选车型，计算价格分、权重、TOPSIS 分、解释文本，并保存快照。
- 推荐历史：读取 `recommend_record` 与 `recommend_item` 快照。
- 算法可视化：只读展示推荐快照中的算法过程。
- 对比：只读比较 1-3 款车型，不触发评分重算。
- 收藏：维护用户关注车型，不影响推荐排序。
- 反馈：维护推荐反馈，进入管理端统计。
- 管理端统计：从真实数据库聚合需求、推荐、车型和反馈数据。

## 前端结构

主要目录：

- `frontend/src/views`：页面视图。
- `frontend/src/api`：接口封装。
- `frontend/src/router`：路由。
- `frontend/src/utils`：推荐展示、对比选择等工具函数。
- `frontend/src/styles`：全局样式。
- `frontend/scripts`：前端验证脚本。

## 路由结构

| 路由 | 页面 | 边界 |
| --- | --- | --- |
| `/` | 首页 | 面向普通用户的产品首页，提供轮播引导、购车推荐、历史、收藏和对比入口。 |
| `/recommend` | 购车需求页 | 结构化需求表单，自然语言解析只辅助填表。 |
| `/recommend/result/:recordId` | 推荐结果页 | 读取推荐详情快照，按 `rankNo` 展示。 |
| `/car/:id` | 车型详情页 | 展示车型基础信息、参数和评分来源。 |
| `/history` | 推荐历史页 | 展示当前用户推荐历史列表。 |
| `/compare` | 车型对比页 | 只读比较 1-3 款车型，不影响推荐排序。 |
| `/favorites` | 我的收藏页 | 展示收藏车型，收藏不参与推荐排序。 |
| `/algorithm-demo` | 算法可视化页面 | 管理端导航中的只读工具页，展示推荐快照中的权重、矩阵、Pareto 和 TOPSIS 过程。 |
| `/admin/cars` | 管理端车型管理 | 维护车型、参数和评分。 |
| `/admin/recommend-records` | 管理端推荐记录 | 查看需求、权重、分数、理由和匹配状态。 |
| `/admin/dashboard` | 管理端统计仪表盘 | 展示需求、推荐、车型和反馈统计。 |
| `/admin/health` | 管理端系统健康检查 | 调用健康检查接口查看后端服务和数据库状态。 |

## 数据流

推荐生成数据流：

```text
用户填写 /recommend
-> POST /api/user/demand
-> POST /api/recommend/generate
-> 写入 recommend_record / recommend_item
-> 前端跳转 /recommend/result/:recordId
-> GET /api/recommend/{recordId}
-> 按快照展示结果
```

历史详情数据流：

```text
/history
-> GET /api/recommend/history
-> 用户选择记录
-> GET /api/recommend/{recordId}
-> 读取保存快照，不重新计算
```

算法可视化数据流：

```text
/algorithm-demo
-> GET /api/recommend/history
-> GET /api/recommend/{recordId}/algorithm-visualization
-> 基于推荐快照展示算法过程
```

## 当前已实现功能

- 后端健康检查和管理端系统健康检查页面。
- 本地数据库初始化脚本。
- 默认用户上下文 `app_user.id = 1` 和默认管理员上下文 `admin.id = 1`。
- 120 条车型基础数据和 120 条车型参数种子数据。
- 车型管理、车型参数维护、车型评分查询和评分重算。
- 用户端车型详情、品牌选项和车型选项。
- 结构化购车需求保存、最近需求查询和按 ID 查询。
- 自然语言解析辅助填表。
- `pareto-topsis-v1` 推荐生成、补充推荐、解释生成和快照保存。
- 推荐历史列表和历史详情。
- 只读算法可视化接口和 `/algorithm-demo` 页面。
- 车型对比、收藏、反馈。
- 管理端车型、推荐记录、统计仪表盘和系统健康检查。

## 功能边界

- 推荐算法只在后端执行。
- 用户端推荐结果页展示用户可理解的分数、标签、理由、不足和维度评分。
- 管理端和算法可视化页面可以展示权重、候选、Pareto、TOPSIS 等算法细节。
- 收藏和反馈不改变 `pareto-topsis-v1` 的排序结果。
- 推荐历史以保存快照为准，当前评分规则变化不会覆盖历史结果。
