# 实施计划：Agent 上下文与记忆输入可视化

**分支**：`003-agent-context-visualization` | **日期**：2026-06-30 | **规格**：[spec.md](./spec.md)

**输入**：来自 `/specs/003-agent-context-visualization/spec.md` 的功能规格

**说明**：本文档由 `/speckit-plan` 填充。除代码标识符、命令、API 字段和第三方专有名词外，正文必须使用简体中文。

## 概要

本功能在已有真实 Web 工作台上增加 Agent 上下文与记忆输入可视化。用户在中间主工作区继续只与大模型对话；右侧“当前记录”下方以彩色状态卡片展示当前会话上下文、已确认事实、待确认联想、候选记忆、将发送给模型的上下文预览、模拟模型输出、事实边界检查结果，以及“新建记录 / 清空当前会话”入口。

实现思路是扩展现有工作台快照，而不是引入真实模型、长期数据库或小红书。后端继续复用当前会话和离线 Agent 内核，生成可解释的 `AgentState` 快照、稳定候选记忆样例、上下文预览、模拟输出和事实边界检查结果；前端在右侧当前记录下方渲染状态卡片，保证中间区域只保留对话流、输入框和必要错误提示。清空会话用于重置当前会话边界和浏览器恢复状态，不删除未来可能存在的长期记忆、历史归档或外部平台数据。

主要风险包括：用户把候选记忆误认为真实长期记忆召回、把模拟输出误认为真实模型调用、状态卡片过多导致右侧难以扫描、刷新恢复与清空会话语义冲突，以及上下文预览把待确认联想误表达成已确认事实。

## 技术上下文

**语言/版本**：Java 21 作为后端目标运行基线；TypeScript 5.x 作为前端目标语言；现有本地环境需继续使用满足 Java 21 的 JDK。

**主要依赖**：后端沿用 Spring Boot Web、Maven 和现有应用内 Agent 角色；前端沿用 React、Vite 和现有工作台组件。真实模型仍不接入，模型输出使用稳定模拟结果或固定样例。

**存储**：本切片使用后端内存会话状态和浏览器本地恢复状态；不接入 PostgreSQL、pgvector、Redis、Kafka 或小红书数据源。候选记忆来自当前会话派生、本地示例或稳定测试数据。

**测试**：后端使用现有 Java 行为测试、Spring Boot 契约测试和本地测试 runner；前端使用 Vitest、组件/流程测试、本地脚本检查和浏览器人工验收。后续可在 e2e 中覆盖刷新恢复与清空会话。

**目标平台**：本地开发环境、桌面 Web 浏览器、本地 Spring Boot 服务、本地 Vite 前端服务。

**项目类型**：前后端分离 Web 应用的 Agent 状态可视化垂直切片。

**性能目标**：用户提交消息后 3 秒内看到状态卡片、上下文预览或可恢复错误；用户清空当前会话后 30 秒内回到空记录状态；用户 2 分钟内能区分当前上下文、已确认事实、待确认联想和候选记忆。

**约束**：API Key、账号凭证、Cookie、Authorization Header 和 Session Token 不得进入页面、日志、测试输出或文档；不得执行真实模型调用、真实长期记忆召回、小红书搜索、发布、评论、点赞、收藏或真实生图；待确认联想和候选记忆不得写成用户确认事实。

**规模/范围**：MVP 本地单用户；只扩展当前工作台的右侧状态卡片区、工作台快照契约、模拟 Agent 状态和清空当前会话能力。不做多用户权限、远程部署、真实模型、长期数据库、真实外部平台或完整历史归档。

## 宪法检查

*关卡：Phase 0 调研前必须通过；Phase 1 设计后必须复查。*

- **真实咖啡体验**：PASS。计划将用户确认事实、待确认联想、候选记忆、模拟输出和无依据表达分开展示，事实边界检查必须阻止未确认内容成为事实。
- **可追踪的 Agent 状态**：PASS。计划新增 Agent 状态卡片区和 `AgentState` 快照，覆盖上下文、事实、联想、候选记忆、上下文预览、模拟输出、事实边界检查和会话控制动作。
- **工具安全与用户确认**：PASS。本切片不执行高影响外部工具；清空当前会话需要用户确认，但它只影响当前本地会话和恢复状态，不删除长期记忆或外部平台数据。
- **可验证的垂直切片**：PASS。每个用户故事都可通过工作台快照、前端渲染、清空会话和浏览器流程独立验收。
- **面向学习的克制架构**：PASS。沿用 Spring Boot、React、Vite 和应用内 Agent 角色，不引入真实模型、数据库、独立 Agent 运行时或复杂流式框架。
- **中文文档**：PASS。计划、调研、数据模型、契约、quickstart 和后续任务默认使用简体中文。

## 项目结构

### 当前功能文档

```text
specs/003-agent-context-visualization/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── agent-context-visualization-contract.md
└── checklists/
    └── requirements.md
```

### 源码结构

```text
backend/
├── src/main/java/com/minyuwei/xhs/coffeeagent/
│   ├── workbench/
│   │   ├── api/
│   │   ├── application/
│   │   └── domain/
│   ├── agent/
│   ├── tasting/
│   └── shared/
├── src/main/resources/
└── src/test/java/com/minyuwei/xhs/coffeeagent/

frontend/
├── src/app/
├── src/components/
├── src/features/agent-trace/
├── src/features/conversation/
├── src/features/tasting-form/
├── src/services/
└── src/stores/
```

**结构决策**：本功能继续以 `workbench` 作为后端用户可见快照边界，避免在 003 中新增真实 `memory` 或 `model` 服务依赖。前端在现有 `WorkbenchLayout` 的右侧 `right-inspector` 内扩展状态卡片组件，中间 `main-workspace` 保持对话专用。清空当前会话优先作为工作台会话控制能力接入现有 API 层和本地恢复 store。

## 复杂度追踪

当前无宪法检查 FAIL，无需复杂度例外。

## Phase 0 调研产物

已生成：[research.md](./research.md)

调研已解决以下问题：

- Agent 状态可视化应该扩展现有工作台快照，还是新增独立轨迹服务。
- 候选记忆在不接长期数据库时如何表达来源边界。
- 上下文预览如何区分“会发送”“仅页面观察”和“待确认后发送”。
- 模拟模型输出和事实边界检查如何避免用户误判为真实调用。
- “新建记录 / 清空当前会话”如何与刷新恢复、内存会话和未来长期记忆边界共存。
- 中间对话区与右侧状态卡片区的布局边界如何保持清楚。

## Phase 1 设计产物

已生成：

- [data-model.md](./data-model.md)
- [contracts/agent-context-visualization-contract.md](./contracts/agent-context-visualization-contract.md)
- [quickstart.md](./quickstart.md)

## Phase 1 宪法复查

- **真实咖啡体验**：PASS。数据模型和契约都要求来源类型、确认状态、发送状态和事实边界检查结果，不允许待确认联想成为确认事实。
- **可追踪的 Agent 状态**：PASS。设计产物覆盖 `AgentStateSnapshot`、上下文项、候选记忆、上下文预览、模拟输出、事实边界检查和会话控制动作。
- **工具安全与用户确认**：PASS。契约明确本切片不执行真实模型、小红书和高影响外部动作；清空会话有确认语义且不删除长期数据。
- **可验证的垂直切片**：PASS。quickstart 覆盖空状态、状态分区、上下文预览、模拟输出、事实边界、刷新恢复和清空当前会话。
- **面向学习的克制架构**：PASS。设计仍使用现有前后端运行时和本地模拟数据，不引入新基础设施。
- **中文文档**：PASS。新增计划产物均使用简体中文。
