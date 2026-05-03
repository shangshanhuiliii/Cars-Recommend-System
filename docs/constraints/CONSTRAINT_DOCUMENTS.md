# 约束性文档清单

本文档用于说明哪些文档对工程开发有实际约束，后续开发必须遵循。新增功能、修改接口、调整数据库、改推荐算法或改前端体验前，先查本清单，再进入对应专项文档。

## 1. 判定原则

约束性文档具备以下特征：

- 定义当前系统必须遵守的规则、边界或禁止事项。
- 定义接口字段、数据库字段、算法排序、推荐快照或前端展示规则。
- 明确写有“以本文档为准”“必须”“不得”“不能”等工程约束。
- 被 `AGENTS.md` 或 `docs/constraints/SYSTEM_CONTRACTS.md` 指定为当前规则源。

如果约束性文档之间出现冲突，优先级为：

1. `AGENTS.md` 和 `docs/constraints/SYSTEM_CONTRACTS.md`
2. 对应专项文档，例如 API、数据库、算法、前端文档
3. 开发指南和项目规格
4. 解释性、交接性、历史性文档

冲突无法直接判断时，先停下来说明冲突点，不直接改代码或文档。

## 2. 最高规则入口

| 文档 | 约束范围 | 后续开发要求 |
| --- | --- | --- |
| `AGENTS.md` | 项目级协作规则、AI 代理规则、文档分工、禁止事项 | 所有 AI 代理和协作者必须遵守 |
| `docs/constraints/SYSTEM_CONTRACTS.md` | 算法、字段、数据库、API、前端和文档的系统规则合同 | 新功能开发前必须先查 |

## 3. 开发流程与项目边界

| 文档 | 约束范围 | 后续开发要求 |
| --- | --- | --- |
| `docs/constraints/DEVELOPMENT_GUIDE.md` | 本地准备、启动、测试、提交前检查、新功能开发流程 | 接手项目和开始开发前必须阅读 |
| `docs/constraints/PROJECT_SPEC.md` | 项目定位、范围边界、功能优先级、验收口径 | 涉及功能取舍和范围变更时必须遵循 |

## 4. 专项工程规则

| 文档 | 约束范围 | 后续开发要求 |
| --- | --- | --- |
| `docs/constraints/API_DESIGN.md` | 接口分组、统一响应、请求字段、响应字段和核心接口 | 修改或新增接口前必须遵循 |
| `docs/constraints/DATABASE_DESIGN.md` | 数据库表职责、关键字段、推荐追溯数据设计 | 修改表结构、字段或数据追溯规则前必须遵循 |
| `docs/constraints/RECOMMENDATION_DESIGN.md` | 推荐闭环概要、模块边界、STRICT 与推荐组规则 | 修改推荐主链路或模块职责前必须遵循 |
| `docs/constraints/RECOMMENDATION_ALGORITHM_UPGRADE.md` | 当前主算法 `pareto-topsis-v1` 的公式、流程、排序和快照规则 | 修改推荐算法、权重、排序或解释生成前必须遵循 |
| `docs/constraints/RECOMMENDATION_IMPLEMENTATION_LOGIC.md` | 当前后端推荐算法实际调用链和 15 步实现流程 | 维护推荐服务、排查推荐结果或做代码审计时必须遵循 |
| `docs/constraints/RECOMMENDATION_ALGORITHM.md` | 车型特征评分规则，包含空间、安全、能耗、智能、舒适、动力、口碑、热度 | 修改车型评分规则或评分字段前必须遵循 |
| `docs/constraints/FRONTEND_DESIGN.md` | 前端页面展示、交互状态、推荐结果呈现和样式规范 | 修改用户端、管理端或算法可视化页面前必须遵循 |

## 5. 本地数据库初始化规则

| 文档 | 约束范围 | 后续开发要求 |
| --- | --- | --- |
| `docs/constraints/DATABASE_INIT.md` | 本地 MySQL 创建、重建、导入测试数据和评分重算说明 | 新机器初始化或本地开发库重建时必须遵循 |

数据库真实结构仍以 `docs/constraints/DATABASE_DESIGN.md` 和 `backend/src/main/resources/db/schema.sql` 为准；测试数据仍以 `backend/src/main/resources/db/seed-data.sql` 为准。

## 6. 按任务查文档

| 任务类型 | 必查文档 |
| --- | --- |
| 新增功能 | `docs/constraints/SYSTEM_CONTRACTS.md`、`docs/constraints/DEVELOPMENT_GUIDE.md`、对应专项文档 |
| 修改接口 | `docs/constraints/API_DESIGN.md`、`docs/constraints/SYSTEM_CONTRACTS.md` |
| 修改数据库 | `docs/constraints/DATABASE_DESIGN.md`、`docs/constraints/DATABASE_INIT.md`、`docs/constraints/SYSTEM_CONTRACTS.md` |
| 修改推荐算法 | `docs/constraints/RECOMMENDATION_ALGORITHM_UPGRADE.md`、`docs/constraints/RECOMMENDATION_IMPLEMENTATION_LOGIC.md`、`docs/constraints/SYSTEM_CONTRACTS.md` |
| 修改车型评分 | `docs/constraints/RECOMMENDATION_ALGORITHM.md`、`docs/constraints/DATABASE_DESIGN.md`、`docs/constraints/SYSTEM_CONTRACTS.md` |
| 修改前端页面 | `docs/constraints/FRONTEND_DESIGN.md`、`docs/constraints/API_DESIGN.md`、`docs/constraints/SYSTEM_CONTRACTS.md` |
| 初始化本地数据库 | `docs/constraints/DATABASE_INIT.md`、`docs/reference/DATABASE_SCRIPT_README.md` |

