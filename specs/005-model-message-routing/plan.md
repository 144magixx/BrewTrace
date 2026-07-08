# 实施计划：模型驱动消息路由

**分支**：`005-model-message-routing` | **日期**：2026-06-30 | **规格**：[spec.md](./spec.md)

**输入**：来自 `/specs/005-model-message-routing/spec.md` 的功能规格

**说明**：本文档由 `/speckit-plan` 填充。除代码标识符、命令、API 字段和第三方专有名词外，正文必须使用简体中文。

## 概要

本功能把当前“模型返回即三版文案”的单一路径升级为“模型驱动消息路由”：真实模型先返回结构化消息，顶层用 `messageType` 区分 `CONVERSATION` 和 `POST`，并用统一的 `talk` 字段作为聊天框唯一展示内容。`CONVERSATION` 表示信息不足或仍需用户确认，系统继续引导用户补充；`POST` 表示可以进入发布草稿链路，系统展示克制版、夸张版和锐评版三版文案，但仍不自动发布。

实现思路是在现有 `workbench` 快照、`ModelGateway`、模型请求预览和模型输出面板上扩展业务契约：新增模型消息结果、POST/CONVERSATION 两套消息数据、统一 `talk` 展示字段和路由校验；提示词继续资源化管理，并由后端按当前上下文、事实边界和风格模板动态组合。前端不推断模型意图，只按后端返回的消息类型和 `talk` 展示聊天状态；只有 POST 结果生成草稿和发布前确认入口。

主要风险包括：模型把追问和草稿混在同一次返回中；POST 信息不足时模型补写未确认事实；前端继续把草稿正文塞进聊天气泡；动态 prompt 模板分散导致不可复盘；以及未来新增图片、记忆、工具等链路时绕开统一消息路由模式。

## 技术上下文

**语言/版本**：后端沿用 Java 21；前端沿用 TypeScript 5.x；文档与计划默认使用简体中文。

**主要依赖**：后端使用 Spring Boot 4.1.x、Spring AI 2.0.x、Maven、现有 `ModelGateway`、Spring AI `ChatClient`、Responses API 兼容 `ChatModel` 和工作台应用服务；前端沿用 React、Vite、现有会话工作台、Agent 状态卡片、上下文预览和模型输出组件；提示词模板沿用 `backend/src/main/resources/prompts/` 的版本化资源管理方式。

**存储**：本切片不新增数据库表和持久化依赖；继续使用当前后端内存会话状态与浏览器本地恢复状态。不接入真实长期记忆数据库，不写入小红书发布状态。

**测试**：后端使用现有 Java 行为测试、Spring Boot 契约测试和本地 `TestRunner`；新增模型解析与路由测试覆盖 POST、CONVERSATION、格式异常和事实边界；前端使用 Vitest 组件/流程测试覆盖 `talk` 聊天气泡、POST 草稿展示和 CONVERSATION 继续输入状态；quickstart 做本地人工验证。

**目标平台**：本地开发环境、本地 Spring Boot 服务、本地 Vite 前端服务、桌面 Web 浏览器；真实模型继续通过已配置的 OpenAI-compatible 文本模型链路访问。

**项目类型**：前后端分离 Web 应用中的 Agent 模型输出契约与工作台路由垂直切片。

**性能目标**：真实模型返回后，用户 1 分钟内能判断当前是“继续补充信息”还是“草稿已生成”；模型结果解析与状态更新不得额外引入用户可感知的长等待；失败时一次交互内展示可恢复提示并保留当前会话。

**约束**：API Key、Authorization、Cookie、Session Token 和可复用凭证不得进入请求预览、响应预览、日志、测试快照或文档；POST 只进入草稿和发布前确认，不自动公开发布；CONVERSATION 不生成可发布草稿；提示词不得硬编码在 Java、TypeScript 或测试中；未确认内容不得写成用户确认事实。

**规模/范围**：MVP 本地单用户；本功能只支持 `CONVERSATION` 和 `POST` 两类模型消息。图片生成、记忆写入、工具确认、公开发布等后续动作沿用同一模式扩展，但不在本切片实现。

## 宪法检查

*关卡：Phase 0 调研前必须通过；Phase 1 设计后必须复查。*

- **真实咖啡体验**：PASS。计划要求 CONVERSATION 先追问缺失事实，POST 保留事实边界并禁止补写未确认豆庄、海拔、处理法、产区、风味或偏好。
- **可追踪的 Agent 状态**：PASS。计划把 `messageType`、`talk`、POST/CONVERSATION 消息、请求预览、响应预览、事实边界和草稿状态纳入工作台可见快照。
- **工具安全与用户确认**：PASS。POST 只进入草稿和发布前确认；不执行小红书公开发布、点赞、评论、收藏或其他高影响工具。
- **可验证的垂直切片**：PASS。CONVERSATION、POST、格式异常、事实边界和前端展示都能独立通过自动测试与 quickstart 验证。
- **面向学习的克制架构**：PASS。沿用现有模型网关、工作台快照和提示词资源目录，不新增独立 Agent 运行时或复杂编排框架。
- **中文文档**：PASS。计划、调研、数据模型、契约、quickstart 和后续任务均使用简体中文。

## 项目结构

### 当前功能文档

```text
specs/005-model-message-routing/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── model-message-routing-contract.md
└── checklists/
    └── requirements.md
```

### 源码结构

```text
backend/
├── src/main/java/com/minyuwei/xhs/coffeeagent/
│   ├── agent/
│   │   ├── application/
│   │   └── infrastructure/
│   ├── workbench/
│   │   ├── api/
│   │   ├── application/
│   │   └── domain/
│   └── shared/
├── src/main/resources/
│   └── prompts/
└── src/test/java/com/minyuwei/xhs/coffeeagent/

frontend/
├── src/app/
├── src/features/
│   ├── agent-trace/
│   └── conversation/
├── src/services/
└── src/stores/
```

**结构决策**：本功能继续以 `workbench` 作为用户可见状态边界；`agent/application` 承担模型消息、POST/CONVERSATION 消息和文案变体的业务契约；`agent/infrastructure` 负责真实模型请求、结构化响应解析和模板加载；`workbench/application` 根据模型消息类型生成快照、草稿和状态；前端只扩展现有会话和 Agent 状态组件，不新增页面。

## 复杂度追踪

当前无宪法检查 FAIL，无需复杂度例外。

## Phase 0 调研产物

已生成：[research.md](./research.md)

调研已解决以下问题：

- 模型路由字段应放在顶层还是每个文案变体中。
- CONVERSATION 和 POST 是否应共用一个 DTO 或拆成两套消息。
- 聊天框展示字段如何命名和约束。
- 提示词动态组合如何避免硬编码与不可复盘。
- POST 信息不足时如何允许生成但保留事实边界。
- 后续模型驱动能力如何复用同一消息路由模式。

## Phase 1 设计产物

已生成：

- [data-model.md](./data-model.md)
- [contracts/model-message-routing-contract.md](./contracts/model-message-routing-contract.md)
- [quickstart.md](./quickstart.md)

## Phase 1 宪法复查

- **真实咖啡体验**：PASS。数据模型明确区分用户确认事实、模型推断、待确认项和风险提醒；契约要求 POST 文案必须保留事实边界。
- **可追踪的 Agent 状态**：PASS。设计把模型消息、`talk`、路由状态、请求/响应预览、POST 草稿和 CONVERSATION 追问都纳入可见快照。
- **工具安全与用户确认**：PASS。契约规定 POST 不等于公开发布，发布前确认仍是后续链路的前置条件。
- **可验证的垂直切片**：PASS。quickstart 覆盖信息不足追问、信息完备生成、异常返回阻断、聊天框只展示 `talk` 和提示词模板检查。
- **面向学习的克制架构**：PASS。设计沿用现有工作台与模型网关，新增的是稳定消息契约和模板组合规则。
- **中文文档**：PASS。新增计划产物均使用简体中文。
