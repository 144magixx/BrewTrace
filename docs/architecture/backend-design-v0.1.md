# 后端设计 v0.1

## 1. 文档目标

本文沉淀咖啡品鉴创作 Agent MVP 的后端设计确认结果。后续 speccoding 应以本文、`specs/001-coffee-agent-mvp/plan.md` 和相关契约文档为实现依据。

## 2. 总体原则

- 后端采用 Spring Boot 4.x、Spring AI 2.x、Maven。
- 架构采用模块化单体，偏 DDD 分层。
- MVP 本地单用户运行，固定当前用户为 `local-user`。
- 所有文档、注释和学习材料默认使用简体中文。
- 真实 API Key 只从仓库外私有 env 文件注入，不进入代码、文档、日志或 Agent 轨迹。

## 3. 业务边界与包结构

源码按业务边界优先组织，每个边界内部再分层：

```text
backend/src/main/java/.../coffeeagent/
├── tasting/
│   ├── api/
│   ├── application/
│   ├── domain/
│   └── infrastructure/
├── flavor/
├── copywriting/
├── memory/
├── agent/
├── trace/
├── tools/
├── publishing/
├── user/
└── shared/
```

主要 bounded context：

- `tasting`：品鉴会话、咖啡记录、冲煮参数、感官评分。
- `flavor`：风味联想、温度段风味、候选接受/拒绝。
- `copywriting`：文案草稿、审稿、满意度反馈。
- `memory`：长期记忆、向量检索、相似记录召回、偏好推断。
- `agent`：Orchestrator、Planner、上下文组装、双 Agent 模式。
- `trace`：Agent 轨迹、真实 prompt、工具结果和决策快照。
- `tools`：工具注册、工具适配器、风险策略、工具调用记录。
- `publishing`：发布包、小红书填写、二次确认、发布状态机。
- `user`：本地用户上下文与偏好。
- `shared`：通用错误、时间、ID、事件、响应 envelope。

## 4. 分层规则

- `api`：Controller、REST/SSE DTO、请求校验、响应 envelope。
- `application`：UseCase、Application Service、事务边界、调用领域对象和 Repository。
- `domain`：聚合、值对象、领域服务、领域事件、领域 Repository 接口。
- `infrastructure`：JPA Entity、Mapper、Repository Adapter、JDBC/SQL、模型网关、小红书 skill、文件存储、Kafka。

Domain 对象保持纯净，不添加 JPA 注解，不依赖 Spring 事务和外部 SDK。

## 5. 事务与事件

Application Service 是事务边界：

- 单个用户命令对应一个清晰事务。
- 核心状态变更和 `DomainEventOutbox` 写入必须同事务提交。
- Domain 聚合产生 Domain Event，Application Service 收集事件并写入 Outbox。
- Repository 参与上层事务，不自行扩大事务边界。
- 模型调用、小红书自动化、Kafka 投递、图片生成等外部副作用不放进核心数据库事务。

Outbox 第一版采用 PostgreSQL Outbox 表 + 后端定时 Publisher：

- Publisher 轮询 `PENDING` 和到达 `nextRetryAt` 的 `FAILED_RETRYABLE`。
- 成功投递 Kafka 后标记 `PUBLISHED`。
- 失败按退避策略重试，超过上限进入 `FAILED_DEAD`。
- 多实例使用 `lockedBy` / `lockedAt` 或数据库锁防重复投递。
- 消费者必须幂等。

## 6. 持久化与向量检索

- 普通结构化业务表使用 Spring Data JPA。
- Infrastructure 中定义 JPA Entity、Spring Data Repository、Mapper、Repository Adapter。
- Flyway 管理 DDL、pgvector 扩展、普通索引和 HNSW 索引。
- pgvector 向量写入、HNSW 索引和相似度查询使用 JDBC/SQL。
- `memory_embeddings.vector` 使用 `vector(1024)`、HNSW、cosine 距离、`vector_cosine_ops`。

记忆召回采用混合检索：

1. 结构化字段检索：豆名、产区、处理法、风味关键词、感官评分。
2. 向量检索：历史文案、风味描述、偏好摘要、外部参考摘要。
3. 合并、去重、排序。
4. 输出相似原因，避免把召回内容写成用户事实。

## 7. Agent 编排

当前实现已收敛为唯一 GPT-5.5 文本模型链路，不再保留本地追问、本地文案生成或本地替代分支。MVP 仍保留编排模式字段，用于表达产品形态：

- `EXPLICIT_WORKFLOW`：默认模式，服务端显式组装上下文并调用模型网关。
- `MODEL_TOOL_CALLING`：实验模式，让模型在受控工具集合内选择工具。

后续多 Agent 应通过真实模型网关和可追踪工具边界实现，不再恢复旧的应用内硬编码角色。规划中的逻辑能力包括：

- 追问和信息补齐。
- 风味联想。
- 记忆召回和相似记录解释。
- 通过真实模型生成克制版、夸张版、锐评版文案。
- 事实边界、风险和重复表达检查。
- 发布包和发布流程推进。

所有模式共用工具注册、确认策略、模型网关和 Agent 轨迹。

## 8. 模型、Prompt 与多模态

业务代码只依赖 `ModelGateway`，不直接使用 Spring AI Client。

`ModelGateway` 负责：

- 文本模型 `gpt-5.5`。
- 图片模型 `gpt-image-2`，仅用户主动请求生图时调用。
- 豆袋图片解析，结果全部标记为 `PENDING_CONFIRMATION`。
- 统一超时、2 次短重试、错误分类、轨迹记录和敏感信息遮蔽。
- 文本模型返回必须先解析为 `ModelAgentMessage`，再由后端按 `messageType` 路由。`CONVERSATION` 进入继续追问链路，`POST` 进入草稿链路；前端聊天框只展示 `talk`。

模型消息契约：

- `ModelAgentMessage`：顶层消息，包含 `messageType`、`talk`、`post`、`conversation`、`warnings`。
- `ConversationModelMessage`：继续对话消息，包含 `questions`、`pendingConfirmations`、`warnings`，不得生成草稿。
- `PostModelMessage`：发布草稿消息，包含三版 `variants` 和 `warnings`，仅表示草稿就绪，不触发公开发布。
- `CopyVariant`：三版文案必须刚好覆盖 `RESTRAINED`、`EXAGGERATED`、`SHARP_REVIEW`，后端校验缺失、重复、空标题正文和事实边界风险。

Prompt 放在 `src/main/resources/prompts/`：

```text
prompts/
├── agent/openai-responses-copy-v1.md
├── agent/openai-responses-copy-task-v1.md
├── style/restrained-style-v1.md
├── style/exaggerated-style-v1.md
├── style/sharp-review-style-v1.md
├── flavor/suggest-v1.md
├── draft/generate-v1.md
├── review/fact-boundary-v1.md
├── memory/compress-v1.md
└── publishing/package-v1.md
```

Prompt 需要版本化，模型调用轨迹记录 prompt 版本、输入摘要和原始快照。模型路由 prompt 由 `PromptComposer` 在后端动态组合：基础路由模板、三种风格提示词正文、当前上下文、已确认事实、待确认联想、候选记忆边界和动态约束一起进入最终请求。Java、TypeScript 和测试代码不得硬编码成段系统提示词、字段解释或风格要求，只允许保留稳定资源路径、占位符名和枚举值。

## 9. 工具系统

工具系统采用 `ToolRegistry + ToolAdapter + ToolCallPolicy + ToolCallRecorder`。

小红书能力封装成细粒度工具：

- `xiaohongshu.checkLogin`
- `xiaohongshu.searchFeeds`
- `xiaohongshu.getFeedDetail`
- `xiaohongshu.fillPublish`
- `xiaohongshu.clickPublish`
- `xiaohongshu.saveDraft`

Agent 不直接调用 skill、脚本、HTTP 或 SDK，只调用后端注册工具。高影响工具必须校验确认状态。

## 10. 发布状态机

发布包状态：

```text
DRAFT_PACKAGE
-> PACKAGE_CONFIRMED
-> XHS_FILLED
-> PREVIEW_CONFIRMED
-> PUBLISHED
```

失败或取消可进入：

- `FAILED`
- `CANCELLED`

公开发布必须在发布包确认、发布页填写、浏览器预览后二次确认后执行。

## 11. API 与错误

REST JSON 响应统一使用：

```json
{ "requestId": "uuid", "data": {}, "error": null }
```

错误响应：

```json
{ "requestId": "uuid", "data": null, "error": { "code": "...", "category": "RETRYABLE", "message": "...", "recoverable": true, "nextActions": [], "details": {} } }
```

错误分类：

- `USER_FIXABLE`
- `RETRYABLE`
- `DEGRADED`
- `FATAL`
- `SAFETY_BLOCKED`

SSE 只推状态，不执行高影响动作；断线后前端通过 REST workspace 快照恢复。

## 12. 文件与 Redis

文件存储第一版使用本地文件系统：

- 上传豆袋图片。
- 生成图片候选。
- 小红书发布临时标题、正文和图片文件。

文件路径入库，按会话和资源类型分目录。真实发布包失败时保留本地文件，便于重试和排查。

Redis 第一版只预留配置和健康检查，不进入核心链路。核心事实、工作台快照、记忆和发布状态都以 PostgreSQL 为准。

## 13. 测试策略

- Unit Test：领域聚合、值对象、工具策略、错误分类。
- Application Test：UseCase 编排、事务边界、Domain Event 到 Outbox。
- Persistence Test：JPA Repository Adapter 和 pgvector JDBC/SQL。
- Kafka Test：Outbox Publisher、消费者幂等和失败重试。
- Agent Contract Test：FakeModelGateway、FakeToolAdapter、FakeMemoryRetriever。
- API Contract Test：REST envelope、SSE 事件、错误分类。
- Manual / Explicit Integration：真实模型、小红书登录、发布页填写和公开发布。

默认自动化测试不得依赖真实 API Key、真实模型响应或真实小红书账号。

## 14. 实现差异记录（2026-06-30）

### 已确认事实

- 当前仓库已实现 `backend/` 与 `frontend/` 双工程，并按 `agent/tasting/flavor/copywriting/memory/tools/publishing/trace/user/shared` 业务边界组织。
- 后端自动化验证使用 Java 21 应用内核、Fake Adapter 和行为测试 runner；前端自动化验证使用 Node 脚本检查核心工作台组件和流程约束。
- 当前本机默认 `java` 指向 Java 8，但 Maven 使用 Homebrew OpenJDK 25；后端行为测试命令已在 `backend/README.md` 中显式使用 Homebrew OpenJDK。

### Codex 判断

- 由于依赖仓库和当前环境对 Spring Boot 4.x / Spring AI 2.x 的解析不稳定，第一轮实现没有把真实 Spring Boot/Spring AI 依赖放入主构建链路。
- 当前代码保留了 Spring 风格的 `Controller`、`ApplicationService`、`RepositoryAdapter`、配置对象、模型网关和工具适配器边界，后续接入 Spring Boot 时可以把现有业务内核挂到真实 Web/DI/配置/持久化运行时。

### 待补升级

- 接入 Spring Boot 4.x、Spring AI 2.x、Flyway、Spring Data JPA、Spring JDBC、Kafka 和 Testcontainers 的真实依赖。
- 将当前 Controller 形状替换为真实 REST Controller 注解和 HTTP 测试。
- 将 `MemoryEmbeddingJdbcRepository` 从内存余弦检索替换为 PostgreSQL/pgvector SQL 实现，并保留 HNSW 集成测试。
