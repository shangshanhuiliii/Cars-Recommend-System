# 系统架构

## 系统定位

Cars Recommend System 是面向普通购车用户的汽车购买决策辅助平台。系统通过用户预算、品牌筛选、车型偏好、动力偏好、座位选项、使用场景和九维显式权重生成用户画像，再结合车型参数和车型特征评分，计算并保存可解释的推荐结果。

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
- 图片资源管理：管理端上传、压缩、审核和软删除车型图片资源，审核通过后更新 `car_model.image_url`。
- 登录认证：产品前端使用全局登录 / 注册浮窗承载普通用户登录、普通用户注册和管理员登录模式；未登录访问需要身份的业务路由时，前端保留目标路径作为背景和合法 `redirect`，通过路由状态打开浮窗，不强制切换到首页。普通用户模式调用 `POST /api/auth/user/login`，管理员模式调用 `POST /api/auth/admin/login`，后端也保留 `POST /api/auth/login` 作为可自动识别 `app_user` 或 `admin` 的统一登录接口。普通用户注册只写入 `app_user`；普通用户注册或登录后进入合法 USER redirect 或 `/`，管理员登录后进入合法 ADMIN redirect 或 `/admin/cars`；`AuthInterceptor` 统一校验 token、角色和接口权限，并对未归类 `/api/**` 采用默认拒绝的 fail-closed 策略。
- 管理端用户管理：管理员查看普通用户状态、统计数字、最近需求和推荐历史入口，并维护 `app_user.status`；收藏车型和反馈记录拆分到独立只读页面。
- 车型评分：根据车型参数生成 `car_feature_score`。
- 用户需求：保存结构化需求，生成画像文本和主观权重。
- 自然语言解析：解析文本并返回表单草稿，不保存需求、不生成推荐。
- 推荐生成：加载需求和候选车型，计算价格分、权重、TOPSIS 分、解释文本，并保存快照。
- 推荐历史：读取 `recommend_record` 与 `recommend_item` 快照。
- 算法可视化：只读展示推荐快照中的算法过程。
- 对比：`user_compare_car` 保存当前 USER 的 1-3 款车型对比列表，查询时只读比较车型快照，不触发评分重算。
- 收藏：维护用户关注车型，不影响推荐排序。
- 反馈：维护推荐反馈，进入管理端统计。
- 管理端收藏和反馈：管理员只读查看收藏车型排行、收藏用户和反馈记录，不代用户操作。
- 管理端运营概览：从真实数据库聚合用户、车型、推荐、收藏和反馈数据。

## 前端结构

主要目录：

- `frontend/src/views`：页面视图。
- `frontend/src/api`：接口封装。
- `frontend/src/router`：路由。
- `frontend/src/utils`：推荐展示、对比返回位置、图片兜底等工具函数。
- `frontend/src/styles`：全局样式。
- `frontend/scripts`：前端验证脚本。

前端根布局按身份和路由分流：普通用户端继续使用顶部导航和产品化页面风格；管理员进入 `/admin/**` 或 `/algorithm-demo` 时使用管理端左侧侧边栏布局。管理端侧边栏集中维护车型管理、用户管理、收藏车型、反馈记录、推荐记录、运营概览、系统健康检查和算法可视化入口，主内容区使用后台系统面板和表格风格。该布局分流只影响前端展示，不改变接口、数据库、推荐算法或认证权限边界。

## 路由结构

| 路由 | 页面 | 边界 |
| --- | --- | --- |
| `/` | 首页 | USER 路由；面向普通用户的产品首页，由数据库随机车辆图片轮播、唯一核心购车推荐入口和特色介绍跳转组成；轮播点击进入车型详情，不放推荐、历史、收藏或对比功能入口。 |
| `/login` | 登录浮窗入口 | 兼容入口；重定向到合法 `redirect` 背景或首页，并通过 `auth=login` 打开认证浮窗。 |
| `/register` | 注册浮窗入口 | 兼容入口；通过 `auth=register` 打开注册浮窗，只创建普通 `USER` 账号。 |
| `/recommend` | 购车需求页 | 产品化结构化需求表单，不展示自然语言解析入口。 |
| `/recommend/result/:recordId` | 推荐结果页 | 读取推荐详情，车名可点击进入 `/car/{id}?recordId={recordId}`，按 `rankNo` 展示，并可在当前推荐车型中进行价格从低到高演示。 |
| `/car/:id` | 车型详情页 | USER 路由；展示横屏大图、基础信息、参数和特征评分。 |
| `/features` | 特色介绍页 | USER 路由；集中说明结构化需求、推荐结果、车型详情、收藏对比和历史回看，不展示用户端复杂算法术语。 |
| `/history` | 推荐历史页 | 展示当前用户推荐历史列表。 |
| `/admin/login` | 管理员登录浮窗入口 | 兼容入口；通过 `auth=admin` 打开管理员登录模式，登录后进入合法 admin redirect 或 `/admin/cars`。 |
| `/compare` | 车型对比页 | 读取当前 USER 后端持久化对比列表，只读比较 1-3 款车型，不影响推荐排序。 |
| `/favorites` | 我的收藏页 | 展示收藏车型，收藏不参与推荐排序。 |
| `/me` | 我的页面 | 当前登录 USER 的个人资料维护页，可保存昵称、邮箱和手机号，不接受 `userId` 参数。 |
| `/algorithm-demo` | 算法可视化页面 | 管理端导航中的只读工具页，展示推荐快照中的权重、矩阵、Pareto 和 TOPSIS 过程。 |
| `/admin/cars` | 管理端车型管理 | 维护车型、参数和评分。 |
| `/admin/users` | 管理端用户管理 | 查看普通用户状态、统计数字、最近需求和推荐入口，支持启用 / 禁用。 |
| `/admin/favorites` | 管理端收藏车型 | 只读查看车型收藏排行和收藏用户。 |
| `/admin/feedbacks` | 管理端反馈记录 | 只读查看用户反馈记录。 |
| `/admin/recommend-records` | 管理端推荐记录 | 查看需求、权重、分数、理由和匹配状态。 |
| `/admin/dashboard` | 管理端运营概览 | 展示用户、车型、推荐、收藏和反馈统计。 |
| `/admin/health` | 管理端系统健康检查 | 调用健康检查接口查看后端服务和数据库状态。 |

## 数据流

推荐生成数据流：

```text
未登录访问业务路由 -> 保留当前路径并打开登录 / 注册浮窗
-> USER 登录或注册 -> 前端保存 token -> 进入合法 redirect 或默认首页 /
-> 用户从首页进入 /recommend 并填写结构化需求
-> Authorization: Bearer <token>
-> POST /api/user/demand（userId 来自 JWT）
-> POST /api/recommend/generate（demandId 必须属于当前用户）
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
ADMIN 登录 -> /algorithm-demo
-> GET /api/admin/recommend-records
-> GET /api/admin/recommend-records/{recordId}/algorithm-visualization
-> 基于推荐快照展示算法过程
```

认证与权限数据流：

```text
POST /api/auth/user/login 或 POST /api/auth/admin/login
-> 后端按登录模式查询 app_user 或 admin
-> PasswordHasher 校验 PBKDF2 hash 并识别 USER / ADMIN
-> JwtTokenService 签发带 principalType 的 HS256 token
-> 前端保存 token / principal / permissions / menus
-> 前端按 principalType 跳转：USER 默认 /，ADMIN 默认 /admin/cars
-> 后续请求由 Axios 附加 Authorization
-> AuthInterceptor 校验 token、principalType 和角色权限
-> AuthContext 暴露当前用户或管理员 ID
-> 未归类 /api/** 默认拒绝，新增 API 必须显式归类为 public / USER / ADMIN
```

`POST /api/auth/login` 作为统一登录接口保留；产品前端使用全局登录 / 注册浮窗承载普通用户登录、普通用户注册和管理员登录模式。公开后端接口只用于登录注册、健康检查、车辆图片和必要车型资源加载，不代表前端存在游客产品身份。

用户注册与管理数据流：

```text
POST /api/auth/user/register
-> 校验 username / password / confirmPassword / nickname / email / phone
-> 确认 username 不与 app_user 或 admin 冲突，email 在 app_user 内唯一
-> PasswordHasher 生成 PBKDF2 hash
-> 写入 app_user，保存 email，status = ACTIVE
-> 签发 USER token 并自动登录
-> 前端进入首页 /

ADMIN 登录 -> /admin/cars
-> GET /api/admin/users 或 /api/admin/users/{userId}
-> 按用户读取 user_demand / recommend_record 摘要
-> 从用户详情跳转 /admin/favorites?userId={id} 或 /admin/feedbacks?userId={id}
-> PUT /api/admin/users/{userId}/status 更新 ACTIVE / DISABLED
```

## 当前已实现功能

- 后端健康检查和管理端系统健康检查页面。
- 本地数据库初始化脚本。
- 用户注册、用户登录、管理员登录、JWT 鉴权、当前身份识别、USER / ADMIN 接口权限和菜单权限。
- 管理端用户管理、用户详情、用户推荐历史入口、收藏车型只读页、反馈记录只读页和启用 / 禁用。
- 本地 seed 默认账号 `user` 和 `admin`；固定 ID 只作为 seed 主键，不再作为接口默认身份来源。
- 120 条车型基础数据和 120 条车型参数种子数据。
- 车型管理、车型参数维护、车型评分查询和评分重算。
- 车型图片上传、压缩、本地静态访问、资源审核和软删除。
- 用户端车型详情、品牌选项和车型选项。
- 结构化购车需求保存、最近需求查询和按 ID 查询。
- 自然语言解析后端接口保留；当前产品前端不展示入口，主推荐流程只使用结构化表单。
- `pareto-topsis-v1` 推荐生成、补充推荐、解释生成和快照保存。
- 推荐历史列表和历史详情。
- 只读算法可视化接口和 `/algorithm-demo` 页面。
- 用户级后端持久化车型对比、收藏、反馈。
- 管理端车型、收藏车型、反馈记录、推荐记录、运营概览和系统健康检查。

## 功能边界

- 推荐算法只在后端执行。
- 用户端推荐结果页展示用户可理解的分数、标签、理由、不足和维度评分。
- 推荐结果页的“价格从低到高演示”只作为当前推荐车型的辅助查看视图，不覆盖推荐记录中的 `rankNo` 权威排序。
- 管理端和算法可视化页面可以展示权重、候选、Pareto、TOPSIS 等算法细节。
- 收藏和反馈不改变 `pareto-topsis-v1` 的排序结果。
- 推荐历史以保存快照为准，当前评分规则变化不会覆盖历史结果。
## 车型数据源导入链路

车型种子数据仍用于本地最小开发和验证，但车型维护不再只依赖 `seed-data.sql`。管理端通过 `POST /api/admin/cars/data-source/import` 上传结构化 JSON 文件，后端 `CarDataSourceImportService` 负责解析、校验和 upsert。

导入链路为：

```text
ADMIN 上传 JSON -> Controller -> CarDataSourceImportService -> car_model / car_param -> 管理端摘要与错误列表 -> 管理员触发车型评分重算
```

导入只维护车型基础信息和车型参数，不触碰收藏、对比、反馈、推荐历史和推荐明细快照。自然键匹配规则为 `brand + series + modelName + launchYear`；同一文件内重复自然键会跳过并进入导入问题列表。新增或更新车型后不会自动改写 `car_feature_score`，管理员需要通过现有评分重算接口生成最新评分。
