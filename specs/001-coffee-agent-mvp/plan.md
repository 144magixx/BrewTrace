# 实施计划：咖啡品鉴创作 Agent MVP

**分支**：`001-coffee-agent-mvp` | **日期**：2026-06-30 | **规格**：[spec.md](./spec.md)

**输入**：来自 `/specs/001-coffee-agent-mvp/spec.md` 的功能规格

**说明**：本文档由 `/speckit-plan` 填充。除代码标识符、命令、API 字段和第三方专有名词外，正文必须使用简体中文。

## 概要

本 MVP 要交付一个本地单用户咖啡品鉴创作工作台：用户可通过对话或结构化模板记录咖啡体验，获得风味联想、历史记忆召回、三类文案草稿、发布包、小红书外部参考和可选生图能力。实现上采用前后端分离 Web 应用：后端用 Spring Boot 4.x + Spring AI 2.x 编排 Agent 逻辑角色、模型调用、记忆、工具适配和发布确认；前端用 React + Vite + TypeScript 构建完整工作台，并提供 Agent 真实交互侧边栏。

主要风险集中在三处：小红书自动化依赖登录态和页面稳定性；模型联想可能越过用户事实边界；真实 prompt 与工具结果侧边栏不脱敏，需要明确限定为本地单人使用且仍不得暴露 API Key。

## 技术上下文

**语言/版本**：Java 21 LTS（建议基线，满足 Spring Boot 4.x 并便于长期维护）、TypeScript 5.x

**主要依赖**：Spring Boot 4.x、Spring AI 2.x、Spring Web MVC、Spring Boot Configuration Processor、Spring Data JPA、Spring JDBC、Flyway、PostgreSQL Driver、pgvector、Spring for Apache Kafka、Kafka、React、Vite、TanStack Query、Zustand、React Hook Form、Zod、ECharts 或 Recharts、shadcn/ui + Radix UI（组件库初始化前再确认）

**模型配置**：文本模型使用 `gpt-5.5`；图像模型使用 `gpt-image-2`；Embedding 使用阿里云百炼 OpenAI 兼容接口 `text-embedding-v4`，默认 1024 维。三类模型均通过环境变量注入真实 Key，文档和代码不得写入真实 Key。

**用户模型**：MVP 使用本地单用户模式，固定当前用户为 `local-user`，不实现登录、注册、Token、Session、RBAC 或多用户权限。数据模型、事件、记忆、偏好和发布记录保留 `userId`，应用层通过 `CurrentUserProvider` 注入当前用户，便于后续升级真实认证。

**配置绑定**：后端使用 Spring Boot `@ConfigurationProperties` 按子系统绑定配置，建议包括 `ModelProperties`、`EmbeddingProperties`、`XiaohongshuProperties`、`AgentProperties`、`StorageProperties`、`RedisProperties`。业务代码不得散落使用 `@Value` 读取环境变量；敏感字段不得进入日志、异常响应或 Agent 轨迹。

**本地基础设施**：使用 Docker Compose 启动 PostgreSQL/pgvector、Kafka、Kafka UI 与 Redis。Kafka UI 用于观察事件流；Redis 作为后续会话缓存、SSE 连接状态、轻量分布式锁或限流的扩展基础，不作为核心事实存储。真实 API Key 和模型网关配置放在仓库外私有 env 文件，例如 `~/.config/xhs-coffee-agent/env`，后端启动前从该文件注入环境变量。

**前端设计依据**：[前端工作台设计 v0.1](../../docs/architecture/frontend-design-v0.1.md)。后续任务拆分和实现必须遵循该文档已确认的三栏工作台、Codex 式对话入口、当前记录紧凑面板、Agent 轨迹彩色卡片流、风味候选 chips、迷你雷达图、文案 Tabs 和发布包检查页。

**Agent 编排模式**：MVP 保留两种模式。默认使用显式工作流模式，由 Planner 输出结构化步骤并由 Orchestrator 执行；同时提供模型自主工具调用模式，基于 Spring AI Advisor / Tool Calling 在受控工具集合内让模型选择工具。两种模式都必须进入同一套工具确认、工具适配器和 Agent 轨迹记录。

**多 Agent 运行形态**：采用应用内逻辑角色，不拆独立进程。Orchestrator 调度 `InterviewAgent`、`FlavorAgent`、`MemoryAgent`、`DraftAgent`、`ReviewAgent`、`PublishingAgent` 等 Spring Service 或组件。

**模型网关与 Prompt**：业务代码只依赖自定义 `ModelGateway`，由其封装 Spring AI 文本、图片和多模态调用。Prompt 放在 `src/main/resources/prompts/`，按场景和版本管理，例如 `flavor/suggest-v1.md`、`draft/generate-v1.md`、`review/fact-boundary-v1.md`。

**工具系统**：采用 `ToolRegistry + ToolAdapter + ToolCallPolicy + ToolCallRecorder`。Agent 只能调用注册工具；具体实现由适配器封装；执行前由策略层判断风险和确认要求；执行后记录工具轨迹，供右侧 Agent 轨迹栏展示。

**持久化策略**：Domain 对象保持纯净，不添加 JPA 注解；各 bounded context 暴露领域 Repository 接口；Infrastructure 层实现 JPA Entity、Spring Data JPA Repository、Mapper 和 Repository Adapter。普通结构化业务表使用 Spring Data JPA；`memory_embeddings.vector`、HNSW 索引和向量相似度查询使用 JDBC/SQL；所有 DDL 由 Flyway 管理。

**事务边界**：Application Service 是事务边界，使用 `@Transactional` 覆盖单个用户命令的核心状态变更和 `DomainEventOutbox` 写入。Domain 对象和 Domain Service 不直接依赖 Spring 事务；Repository 参与上层事务；模型调用、小红书工具、Kafka 投递和图片生成等外部副作用不得放入核心数据库事务。

**领域事件表达**：聚合产生 Domain Event，Application Service 收集事件并写入 `DomainEventOutbox`。不使用 Controller 或 Repository 直接拼事件，也不依赖 Spring ApplicationEvent 作为主链路。

**事务与事件机制**：核心业务状态同步写入 PostgreSQL；后置副作用通过 Kafka 异步处理。归档后生成 embedding、偏好推断、工具结果处理、发布流程推进等事件采用 PostgreSQL Outbox 表 + 后端定时 Publisher 的 Transactional Outbox 模式，保证数据库事务与 Kafka 投递的最终一致性。

**API 通信形态**：采用 REST + SSE。REST 用于命令和查询，响应统一为 `{ requestId, data, error }`；SSE 用于向前端实时推送 Agent 轨迹、工具调用状态、记忆召回结果和 Kafka 异步事件进度。高影响动作确认只能通过 REST 提交。

**存储**：PostgreSQL 保存结构化记录、会话、偏好、工具调用和 Agent 轨迹；pgvector 保存咖啡记录、文案、偏好摘要和外部参考摘要的 embedding，向量维度默认 1024；`memory_embeddings.vector` 使用 HNSW 索引，默认 cosine 距离和 `vector_cosine_ops`；本地文件系统保存临时上传图片、生图候选和小红书发布临时文件，路径入库并按会话/资源类型分目录；Redis 第一版只预留配置和健康检查，不进入核心链路。

**测试**：分层测试 + Fake Adapter + Testcontainers。后端使用 JUnit 5、Spring Boot Test、Testcontainers PostgreSQL/pgvector/Kafka、后端契约测试和 Agent contract test；JPA Repository Adapter 覆盖结构化表映射，JDBC/SQL 测试覆盖 pgvector 写入、HNSW 索引和相似度查询；前端使用 Vitest、React Testing Library、Playwright；模型/小红书相关能力用 fake adapter 与人工验证补足。

**目标平台**：本地开发环境、桌面 Web 浏览器、Spring Boot 后端服务、本机 Chrome + `xiaohongshu-skills`、Docker Compose 管理的 PostgreSQL/pgvector、Kafka、Kafka UI 与 Redis

**项目类型**：前后端分离 Web 应用 + Agent 工具适配后端

**性能目标**：用户 5 分钟内完成一次记录并获得文案草稿；风味联想接口在本地词库路径下 1 秒内返回；一次外部参考最多摘要 5 条；工作台能展示本次流程至少 5 类 Agent 轨迹信息

**约束**：API Key 不入 Git、不写入文档真实值、不打印完整日志；真实密钥只从仓库外私有 env 文件注入，并通过类型安全配置类绑定；公开发布、评论、点赞、收藏等高影响动作必须二次确认；外部参考不得混写成用户事实；MVP 侧边栏默认不脱敏但仅面向本地单人使用；小红书能力必须通过封装后的工具适配器调用

**Agent 模式约束**：模式切换只影响后续对话轮次，不改写历史轨迹。模型自主工具调用模式不得绕过高影响工具确认，不得让模型直接执行 `xiaohongshu-skills` 脚本，只能调用后端封装后的工具适配器。

**事件约束**：Kafka 消费者必须幂等；高影响动作即使由 Kafka 消费者触发，也必须重新校验用户确认状态；事件失败不得破坏已提交的核心咖啡记录；Outbox Publisher 必须支持失败重试、退避、失败终止状态和多实例防重复投递。

**SSE 约束**：SSE 连接只用于状态推送；断线重连后前端必须能通过 REST workspace 快照恢复当前状态；SSE 事件不得包含完整 API Key、Authorization Header、Cookie、Session Token。

**API Envelope 约束**：所有 JSON REST 响应必须包含 `requestId`、`data`、`error` 三个顶层字段。成功响应 `error=null`，失败响应 `data=null`；错误对象必须包含 `code`、`category`、`message`、`recoverable`、`nextActions`、`details`。业务 DTO 不重复携带顶层 `requestId`。

**API 契约来源**：MVP 先维护 Markdown 契约，位于 `specs/001-coffee-agent-mvp/contracts/`；编码时同步生成或补充 OpenAPI。Controller 不作为唯一契约来源。

**错误与降级策略**：采用 `USER_FIXABLE`、`RETRYABLE`、`DEGRADED`、`FATAL`、`SAFETY_BLOCKED` 五级错误。模型、Embedding、Kafka、外部参考、小红书和 SSE 故障必须保留用户已填写内容，写入 Agent 轨迹，并给出重试、降级或用户处理路径。

**模型重试策略**：`ModelGateway` 统一处理模型调用超时和短暂网络错误，默认最多 2 次短重试；仍失败时返回分级错误，用户可手动再次触发。

**测试约束**：自动化测试默认不得依赖真实模型网关、真实小红书账号或真实 API Key。Agent 行为用 `FakeModelGateway`、`FakeToolAdapter`、`FakeMemoryRetriever` 固定输入输出；PostgreSQL、pgvector、Kafka 用 Testcontainers；真实模型和小红书发布能力作为显式集成或人工验证。

**规模/范围**：MVP 单用户、本地运行、桌面 Web 工作台；多 Agent 先作为应用内逻辑角色实现，不拆独立运行时；暂不做登录鉴权、多用户权限、移动端 App、完整 SCA 杯测系统和复杂工作流引擎

## 宪法检查

*关卡：Phase 0 调研前必须通过；Phase 1 设计后必须复查。*

- **真实咖啡体验**：PASS。计划将用户确认事实、模型联想、外部参考、图片创意和待确认信息作为不同来源类型保存并展示。
- **可追踪的 Agent 状态**：PASS。计划把上下文组装、短期记忆、长期召回、工具调用、接受/拒绝建议、审稿判断和归档结果写入 Agent 轨迹。
- **工具安全与用户确认**：PASS。发布和互动类能力通过 `xiaohongshu-skills` 适配器暴露，公开发布必须经过发布包确认和发布页预览后二次确认。
- **可验证的垂直切片**：PASS。MVP 按对话创作、模板记录、记忆召回、真实交互侧边栏、外部参考与发布确认拆分，每个切片都有后端、前端、持久化和验证路径。
- **面向学习的克制架构**：PASS。采用已确认技术栈；多 Agent 以应用内角色服务实现；暂不引入独立编排运行时。
- **中文文档**：PASS。计划、调研、数据模型、契约、快速开始和后续任务默认使用简体中文。

## 项目结构

### 当前功能文档

```text
specs/001-coffee-agent-mvp/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── api-contract.md
│   ├── tool-contracts.md
│   └── event-contracts.md
└── tasks.md
```

### 源码结构

```text
backend/
├── src/
│   ├── main/java/
│   │   └── .../coffeeagent/
│   │       ├── agent/
│   │       ├── tasting/
│   │       ├── flavor/
│   │       ├── copywriting/
│   │       ├── memory/
│   │       ├── tools/
│   │       ├── publishing/
│   │       ├── trace/
│   │       ├── user/
│   │       └── shared/
│   ├── main/resources/
│   │   ├── prompts/
│   │   └── db/migration/
│   └── test/java/
└── pom.xml

frontend/
├── src/
│   ├── app/
│   ├── components/
│   ├── features/
│   │   ├── conversation/
│   │   ├── tasting-form/
│   │   ├── flavor-suggestions/
│   │   ├── memory/
│   │   ├── publishing/
│   │   └── agent-trace/
│   ├── services/
│   └── stores/
└── package.json

docs/
├── prd/
├── architecture/
├── research/
└── learn/
```

**结构决策**：spec-kit 产物只放在 `specs/001-coffee-agent-mvp/`；长期 PRD、架构、调研和学习文档继续放在 `docs/` 对应目录。源码初始化时采用 `backend/` 与 `frontend/` 分离，便于用户重点学习 Java 后端，同时让 Codex 承担前端工作台实现。后端按业务边界优先组织，每个边界内部再分 `api/application/domain/infrastructure`。

**前端设计决策**：本 feature 的前端实现必须读取并遵循 `docs/architecture/frontend-design-v0.1.md`。该文档是后续 speccoding 的 UI/UX 约束来源，不得在未更新设计文档的情况下改成其他布局、配色或主要交互。

## 复杂度追踪

当前无宪法检查 FAIL，无需复杂度例外。

## Phase 0 调研产物

已生成：[research.md](./research.md)

调研已解决以下问题：

- Java 基线与 Spring Boot 4.x、Spring AI 2.x 的版本策略。
- PostgreSQL + pgvector 如何承载结构化记忆和语义召回。
- 阿里云百炼 `text-embedding-v4` 如何作为 pgvector 的 embedding 来源。
- React 工作台的状态、表单、可视化和 Agent 轨迹展示策略。
- `xiaohongshu-skills` 如何封装成后端工具适配器。
- Agent 真实交互侧边栏如何持久化和展示真实 prompt、工具调用、记忆召回与决策。
- 显式工作流模式和模型自主工具调用模式如何共用工具安全、轨迹和前端模式切换。
- Kafka 如何承载归档、记忆、偏好推断、工具结果和发布流程的异步事件。
- REST + SSE 如何支撑工作台快照、Agent 轨迹实时更新和断线恢复。
- 分级错误与可恢复降级如何覆盖模型、工具、Kafka、SSE 和小红书故障。
- 分层测试、Fake Adapter、Testcontainers 和人工验证如何覆盖 Agent 全链路。

## Phase 1 设计产物

已生成：

- [data-model.md](./data-model.md)
- [contracts/api-contract.md](./contracts/api-contract.md)
- [contracts/tool-contracts.md](./contracts/tool-contracts.md)
- [contracts/event-contracts.md](./contracts/event-contracts.md)
- [quickstart.md](./quickstart.md)
- [../../docs/architecture/frontend-design-v0.1.md](../../docs/architecture/frontend-design-v0.1.md)
- [../../docs/architecture/backend-design-v0.1.md](../../docs/architecture/backend-design-v0.1.md)
- [../../docs/learn/technology-selection-learning-v0.1.md](../../docs/learn/technology-selection-learning-v0.1.md)

## Phase 1 宪法复查

- **真实咖啡体验**：PASS。数据模型中的 `sourceType`、`confirmationStatus`、`externalReference` 和审稿提示覆盖事实边界。
- **可追踪的 Agent 状态**：PASS。`AgentTrace`、`AgentTraceStep`、`ToolCallRecord`、`MemoryRecall` 进入数据模型和接口契约。
- **工具安全与用户确认**：PASS。工具契约中 `requiresConfirmation`、`riskLevel`、`confirmationId` 和发布二次确认流程为强制字段。
- **可验证的垂直切片**：PASS。quickstart 覆盖核心 MVP 验证路径和前端工作台布局验证，并区分自动测试与人工验证。
- **面向学习的克制架构**：PASS。设计没有引入独立多 Agent 运行时，先通过 Java 服务和清晰契约呈现 Agent 链路。
- **中文文档**：PASS。新增计划产物均使用简体中文，保留必要英文技术名词和 API 字段。
