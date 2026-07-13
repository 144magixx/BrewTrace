# 项目文档导航

本文只承担文档导航职责。仓库当前技术状态、运行方式、能力边界和下一阶段重点统一维护在：

- [项目当前上下文](./architecture/current-project-context.md)

不要把任意一个 `specs/*/plan.md` 当作整个项目的长期总入口。Spec Kit 文档只描述对应功能切片。

## 1. 建议阅读顺序

1. [项目当前上下文](./architecture/current-project-context.md)：先确认当前已经实现和尚未接通的能力。
2. [AGENTS.md](../AGENTS.md)：确认启动、分支、编码、Prompt、测试和 CR 约束。
3. [咖啡品鉴创作 Agent PRD](./prd/coffee-note-agent-prd-v0.1.md)：理解产品目标和业务边界。
4. 根据当前任务进入具体架构、研究、评审或 `specs/` 文档。

## 2. 目录职责

| 目录 | 职责 | 维护规则 |
|---|---|---|
| `docs/prd/` | 产品需求、范围和里程碑 | 记录“做什么”和“为什么” |
| `docs/architecture/` | 当前上下文、架构、模块设计、运行指南和技术决策 | 实现变化后同步核对 |
| `docs/research/` | 外部平台、风味、样本和可行性调研 | 区分来源事实与工程判断 |
| `docs/learn/` | 学习材料、技术选型讲解和实现检查清单 | 不替代运行时事实来源 |
| `docs/review/` | 新的业务链路 CR 文档 | 遵循 `AGENTS.md` 的 CR 结构和准确行号规则 |
| `docs/code-review/` | 早期代码评审材料 | 历史目录，保留追溯价值；新 CR 不再放入此处 |
| `docs/meeting-notes/` | 讨论纪要和需求对齐 | 记录决策时间、参与者和待办 |
| `specs/` | 按功能切片保存的 spec、plan、tasks、contract 和 quickstart | 只在处理对应功能时阅读 |

## 3. 核心架构文档

- [项目当前上下文](./architecture/current-project-context.md)
- [Agent 技术架构设计 v0.1](./architecture/agent-architecture-v0.1.md)
- [后端设计 v0.1](./architecture/backend-design-v0.1.md)
- [前端工作台设计 v0.1](./architecture/frontend-design-v0.1.md)
- [当前后端接口清单 v0.1](./architecture/api-interface-list-v0.1.md)
- [技术选型决策记录 v0.1](./architecture/technology-decisions-v0.1.md)
- [长短期记忆设计 v0.1](./architecture/memory-design-v0.1.md)
- [Spring AI Agent 能力演进建议 v0.1](./architecture/spring-ai-agent-evolution-plan-v0.1.md)
- [IDEA 本地运行与验收指南](./architecture/idea-local-run-guide.md)

其中带 `v0.1` 的文档记录特定阶段设计，可能包含尚未实现的目标。判断当前状态时，以“项目当前上下文”、源码、自动测试和运行态为准。

## 4. 产品、研究与学习资料

### 产品

- [咖啡品鉴创作 Agent PRD v0.1](./prd/coffee-note-agent-prd-v0.1.md)
- [MVP 需求拆解与里程碑 v0.1](./prd/mvp-scope-v0.1.md)

### 研究

- [小红书发布能力调研 v0.1](./research/xiaohongshu-publish-research-v0.1.md)
- [风味联想体系调研 v0.1](./research/flavor-system-research-v0.1.md)

### 学习

- [技术选型学习文档 v0.1](./learn/technology-selection-learning-v0.1.md)
- [后端实现约束验证清单 v0.1](./learn/backend-implementation-checklist-v0.1.md)

## 5. 维护原则

- 新文档放入职责明确的子目录，不直接散落在仓库根目录。
- 持续维护的当前状态文档可以不带版本号；阶段性设计、调研和评审应保留版本或日期。
- 跨模块改造完成后，先更新 CR，再同步更新“项目当前上下文”中的能力状态和链路。
- 涉及未验证信息时，明确标记为“工程判断”“待调研”或“待用户确认”。
- 历史 plan 不删除，但不得用历史计划覆盖当前代码和运行态事实。
