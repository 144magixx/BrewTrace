# 技术选型决策记录 v0.1

## 1. 文档目标

本文记录咖啡品鉴创作 Agent 的技术选型决策。当前只更新文档，不进入代码实现。

## 2. 已确认选型

| 领域 | 决策 | 说明 |
|---|---|---|
| 后端框架 | Spring Boot 4.x | 选择最新 4.x 能力线，贴近 Spring Framework 7 生态。 |
| AI 集成框架 | Spring AI 2.x | 选择最新 2.x 能力线，用于学习 LLM、Embedding、Vector Store、Tool Calling、Advisors 和 MCP 等能力。 |
| 构建工具 | Maven | 稳定、清晰，适合第一版工程搭建和学习。 |
| 持久化策略 | 纯 Domain + JPA Entity + JDBC | Domain 对象不加 JPA 注解；业务表用 Spring Data JPA；pgvector/HNSW 用 JDBC/SQL。 |
| 事务边界 | Application Service | 应用服务声明事务，覆盖核心状态变更和 Outbox 写入；Domain 不依赖 Spring 事务。 |
| 领域事件 | 聚合事件 + Outbox | 聚合产生 Domain Event，Application Service 收集并写入 Outbox。 |
| 关系数据库 | PostgreSQL | 承载咖啡记录、会话、文案归档、用户偏好等结构化数据。 |
| 向量检索 | pgvector | 与 PostgreSQL 统一部署，适合 MVP 阶段做记忆召回和相似文案检索。 |
| 向量索引 | HNSW | MVP 初期即为 `memory_embeddings.vector` 建 HNSW 索引，优先学习可扩展向量召回。 |
| 异步事件 | Kafka | 用于归档后生成 embedding、偏好推断、发布流程、工具结果处理等后置副作用，便于后续扩展。 |
| API 通信 | REST + SSE | REST 负责命令/查询，SSE 实时推送 Agent 轨迹、工具调用和异步事件进度。 |
| API 响应格式 | 统一 Envelope | REST 响应统一为 `{ requestId, data, error }`，便于前端处理和 Agent 轨迹关联。 |
| 用户模型 | 本地单用户 | MVP 固定 `local-user`，不做登录鉴权；数据模型保留 `userId` 便于后续升级。 |
| 本地基础设施 | Docker Compose | 本地启动 PostgreSQL/pgvector、Kafka、Kafka UI 与 Redis，应用密钥通过仓库外私有 env 文件注入。 |
| 配置绑定 | `@ConfigurationProperties` | 按子系统组织类型安全配置，避免业务代码散落读取环境变量。 |
| 模型网关 | `ModelGateway` | 业务不直接依赖 Spring AI Client，统一模型重试、错误分类和轨迹记录。 |
| Prompt 管理 | resources 文件版本化 | Prompt 放在 `src/main/resources/prompts/`，按场景和版本管理。 |
| 文件存储 | 本地文件系统 | 豆袋图片、生图候选和发布临时文件落本地路径，路径入库。 |
| Redis 第一版 | 仅预留和健康检查 | 不进入核心链路，不保存核心事实或工作台快照。 |
| API 契约来源 | Markdown 优先，编码时补 OpenAPI | Spec Kit 契约先行，Controller 不作为唯一事实来源。 |
| 测试策略 | 分层测试 + Fake Adapter + Testcontainers | 覆盖领域规则、用例编排、Repository、Kafka、SSE、Agent contract 和前端 E2E。 |
| 前端形态 | 完整工作台 | 需要支持对话模式、模板填写、感官维度图、风味联想、发布预览。 |
| 前端框架 | React + Vite + TypeScript | 前端主要由 Codex 维护，优先选择适合复杂 Agent 工作台的生态。 |
| 文本模型 | 用户提供 `gpt-5.5` API Key | 用于对话、规划、文案生成、审稿、信息抽取。 |
| 图像模型 | 用户提供 `gpt-image-2` API Key | 用于豆袋图片解析相关多模态能力和风味视觉化生图。 |
| Embedding 模型 | 阿里云百炼 `text-embedding-v4` | 使用 OpenAI 兼容 Embedding 接口，默认 1024 维，用于 pgvector 记忆检索。 |
| 小红书能力 | `xiaohongshu-skills` | 用于后续抓帖、详情获取、发布页填写和发布动作。 |

## 3. 已确认与仍需确认的细节

### 3.1 Spring Boot 与 Spring AI 版本

已确认：

- 后端使用 Spring Boot 4.x。
- AI 集成使用 Spring AI 2.x。
- 优先使用稳定发布版本，不以 snapshot 作为默认依赖来源。

依据：

- Spring AI 官方文档说明 Spring AI 2.0.x 支持 Spring Boot 4.0.x 和 4.1.x。
- Spring AI 2.x 提供 Chat、Embedding、Text-to-Image、Vector Store、Tool Calling、Advisors、MCP 等 Agent 开发相关能力。

实现前仍需确认：

- 具体 Spring Boot 4.x 补丁版本。
- 具体 Spring AI 2.x 补丁版本。
- Java 版本，默认建议跟随 Spring Boot 4.x 要求选择 Java 21 或更高版本。

参考资料：

- Spring AI 项目页：https://spring.io/projects/spring-ai
- Spring AI Reference：https://docs.spring.io/spring-ai/reference/getting-started.html
- Spring AI API 能力概览：https://docs.spring.io/spring-ai/reference/api/index.html
- Spring AI Upgrade Notes：https://docs.spring.io/spring-ai/reference/upgrade-notes.html

### 3.2 前端技术栈

已确认：React + Vite + TypeScript。

推荐配套：

- React：构建复杂交互工作台。
- Vite：本地开发和构建。
- TypeScript：保持接口类型清晰。
- TanStack Query：管理服务端数据、请求状态和缓存。
- Zustand：管理工作台本地状态。
- React Hook Form + Zod：管理模板表单和校验。
- ECharts 或 Recharts：绘制感官维度图。
- shadcn/ui + Radix UI：优先作为组件体系候选。

说明：

- 用户不希望深度参与前端开发，前端工程复杂度由 Codex 主要承担。
- 前端仍需保持清晰结构和文档，方便用户理解整体系统如何工作。
- UI 组件库最终在初始化前确认，默认倾向 shadcn/ui。

### 3.2.1 用户模型与鉴权边界

已确认：MVP 使用本地单用户模式。

规则：

- 后端固定使用 `local-user` 作为当前用户标识。
- API 暂不要求登录、注册、Token、Session 或 RBAC 权限。
- 数据模型、事件、记忆、偏好和发布记录仍保留 `userId` 字段，默认写入 `local-user`。
- Controller 或应用层可通过 `CurrentUserProvider` 获取当前用户，避免业务代码硬编码字符串。
- 小红书登录态属于外部平台工具状态，不等同于本系统用户登录。

暂不做：

- 多用户隔离。
- 团队协作。
- 用户权限管理。
- 前端登录页。

### 3.2.2 后端包结构

已确认：按业务边界优先组织包结构。

第一版边界：

- `tasting`
- `flavor`
- `copywriting`
- `memory`
- `agent`
- `trace`
- `tools`
- `publishing`
- `user`
- `shared`

每个主要边界内部再按 `api/application/domain/infrastructure` 分层。

### 3.3 API Key 与配置管理

用户可提供 `sk-xxxxx` 形式的 API Key。

安全约束：

- API Key 不得写入代码、文档示例真实值或 Git。
- 本地开发使用仓库外私有 env 文件注入环境变量，当前约定路径为 `~/.config/xhs-coffee-agent/env`。
- 仓库内可提供 `.env.example` 说明变量名，但不得提供真实值。
- 文档中只写占位符，例如 `OPENAI_API_KEY=sk-xxxxx`、`EMBEDDING_API_KEY=sk-xxxxx`。
- 后端日志不得打印完整 Key。

模型配置：

- 文本模型：`TEXT_MODEL=gpt-5.5`，通过 `OPENAI_BASE_URL` 和 `OPENAI_API_KEY` 注入。
- 图像模型：`IMAGE_MODEL=gpt-image-2`，通过图像模型配置注入。
- Embedding 模型：`EMBEDDING_MODEL=text-embedding-v4`，`EMBEDDING_DIMENSIONS=1024`，通过 `EMBEDDING_BASE_URL` 和 `EMBEDDING_API_KEY` 注入。
- 文本/图像模型和 Embedding 模型允许使用不同 API 网关与 Key。

配置绑定方式：

- 使用 Spring Boot `@ConfigurationProperties` 按子系统声明类型安全配置类。
- 建议配置类包括 `ModelProperties`、`EmbeddingProperties`、`XiaohongshuProperties`、`AgentProperties`、`StorageProperties`、`RedisProperties`。
- 业务服务依赖这些配置对象或更上层的网关，不直接使用 `@Value` 散读环境变量。
- 配置类需要覆盖单元测试，至少验证必填项、默认值和敏感字段不进入日志。

参考资料：

- 阿里云百炼 OpenAI Embedding 接口兼容文档：https://help.aliyun.com/zh/model-studio/embedding-interfaces-compatible-with-openai

### 3.3.0 配置绑定规范

已确认：后端配置使用 `@ConfigurationProperties`。

分组建议：

- `coffee.model`：文本模型、图像模型、base URL、fallback URL、超时、重试。
- `coffee.embedding`：Embedding base URL、模型名、维度、超时、批量大小。
- `coffee.xiaohongshu`：本地 skill 路径、浏览器策略、发布确认策略、命令超时。
- `coffee.agent`：默认 Agent 模式、上下文窗口、轨迹开关、工具风险策略。
- `coffee.storage`：上传图片目录、发布临时文件目录、归档保留策略。
- `coffee.redis`：缓存前缀、TTL、SSE 状态 TTL、轻量锁 TTL。

原则：

- `application.yml` 只保存非敏感默认值和环境变量占位符。
- 真实 Key 从仓库外 env 文件进入进程环境，再由 Spring 配置系统绑定到配置类。
- Controller、Application Service、Domain Service 不直接读取环境变量。
- 敏感字段在日志、异常和 Agent 轨迹中必须被遮蔽或完全不输出。

### 3.3.1 模型网关与 Prompt 管理

已确认：

- 业务代码依赖自定义 `ModelGateway`。
- `ModelGateway` 封装 Spring AI 文本、图片和多模态能力。
- 模型调用失败时默认最多 2 次短重试；仍失败则返回分级错误。
- Prompt 放在 `src/main/resources/prompts/`，按场景和版本管理。

好处：

- 模型调用的超时、重试、错误分类、敏感信息遮蔽和 Agent 轨迹记录集中处理。
- 后续更换模型或网关时减少业务代码改动。
- Prompt 可以独立复盘和迭代。

### 3.3.2 本地基础设施与环境变量

已确认：本地开发使用 Docker Compose 管理 PostgreSQL/pgvector、Kafka、Kafka UI 与 Redis，应用密钥放在仓库外私有 env 文件。

第一版 Docker Compose 边界：

- 启动 PostgreSQL，并启用 pgvector 扩展。
- 启动 Kafka，供 Outbox Publisher、记忆生成、偏好推断和发布流程事件使用。
- 启动 Kafka UI，便于学习和调试事件流。
- 启动 Redis，作为后续会话缓存、SSE 连接状态、轻量分布式锁或限流的扩展基础；MVP 不把真实咖啡记录、文案归档或长期记忆只存 Redis。
- Redis 第一版只做配置预留和健康检查，不进入核心工作流。
- 不把后端、前端、真实模型网关或小红书自动化放进 Compose；这些由本机开发命令和本机 Chrome 环境运行。

密钥注入规则：

- 真实 Key 只存在于仓库外，例如 `~/.config/xhs-coffee-agent/env`。
- 后端启动脚本或本地说明可以 `source ~/.config/xhs-coffee-agent/env` 后启动应用。
- `.gitignore` 必须忽略 `.env`、`.env.*`、`backend/.env*`、`frontend/.env*`。
- 文档、测试、示例配置和 Agent 轨迹不得出现完整 Key。

### 3.4 Kafka 事件机制

已确认：后端引入 Kafka 作为异步事件基础设施。

使用边界：

- 核心业务事务仍以 PostgreSQL 为准。
- Kafka 用于业务提交后的后置副作用，例如生成记忆向量、偏好推断、小红书发布状态推进、工具结果处理和后续可扩展任务。
- 涉及业务状态与事件发布一致性时，采用 PostgreSQL Outbox 表 + 后端定时 Publisher 的 Transactional Outbox 模式，避免“数据库提交成功但事件发送失败”或“事件发送成功但事务回滚”的双写问题。
- 本地 Kafka 由 Docker Compose 启动，避免手动安装和环境漂移。
- Outbox Publisher 第一版使用后端定时任务轮询 `PENDING` / `FAILED_RETRYABLE` 事件，成功投递 Kafka 后更新状态。

暂不用于：

- 替代同步 HTTP API。
- 承载用户正在等待的低延迟同步响应。
- 绕过公开发布、评论、点赞、收藏、生图等高影响动作的用户确认。

### 3.5 API 通信形态

已确认：REST + SSE。

使用边界：

- REST 用于创建会话、提交消息、更新模板、生成文案、确认发布、查询工作台快照等命令和查询。
- SSE 用于向前端实时推送 Agent 轨迹卡片、模型步骤、工具调用状态、记忆召回结果、Outbox/Kafka 异步事件进度。
- SSE 不承载高影响动作确认；确认仍通过 REST 命令提交。
- 前端初次进入页面先用 REST 拉取 workspace 快照，再订阅 SSE 增量事件。

### 3.5.1 REST 响应 Envelope

已确认：REST API 使用统一响应包。

格式：

```json
{
  "requestId": "uuid",
  "data": {},
  "error": null
}
```

错误时：

```json
{
  "requestId": "uuid",
  "data": null,
  "error": {
    "code": "XHS_LOGIN_REQUIRED",
    "category": "USER_FIXABLE",
    "message": "小红书未登录，已保留发布包。",
    "recoverable": true,
    "nextActions": ["CHECK_LOGIN"],
    "details": {}
  }
}
```

规则：

- `requestId` 必须存在，用于关联前端请求、服务端日志和 Agent 轨迹。
- 成功时 `error=null`；失败时 `data=null`。
- 业务 DTO 不再重复携带顶层 `requestId`。
- 文件下载或 SSE 可不使用该 JSON envelope，但必须保留可追踪的 `requestId` 或事件 `id`。

### 3.5.2 API 契约来源

已确认：先维护 Markdown 契约，编码时同步生成或补充 OpenAPI。

契约位置：

- `specs/001-coffee-agent-mvp/contracts/api-contract.md`
- `specs/001-coffee-agent-mvp/contracts/tool-contracts.md`
- `specs/001-coffee-agent-mvp/contracts/event-contracts.md`

Controller 不作为唯一契约来源。

### 3.6 测试策略

已确认：分层测试 + Fake Adapter + Testcontainers。

分层：

- Unit Test：覆盖 Domain 聚合、值对象、UseCase 编排、ToolCallPolicy、错误分类。
- Slice / Integration Test：覆盖 Controller、Repository、PostgreSQL/pgvector、Kafka Outbox publisher/consumer、SSE event stream。
- Agent Contract Test：使用 `FakeModelGateway`、`FakeToolAdapter`、`FakeMemoryRetriever` 固定模型和工具输出，验证 prompt 约束、事实边界、工具调用和 AgentTrace。
- Manual / E2E：覆盖真实小红书登录/验证码/发布页填写、真实模型调用、前端 Playwright 流程。

原则：

- 自动化测试不得依赖真实 API Key、真实小红书账号或不可控外部模型响应。
- 真实模型和小红书能力作为人工验证或显式集成验证。
- Kafka、PostgreSQL 和 pgvector 优先用 Testcontainers 提供接近真实的本地环境。

## 4. 对架构的影响

### 4.1 后端模块

后端按 Spring Boot 应用组织：

- Controller：提供工作台 API。
- Application Service：编排 Agent 用例。
- Domain Service：处理咖啡记录、风味、记忆、发布包。
- Infrastructure：封装模型、数据库、向量库、小红书 skill。

持久化边界：

- Domain 对象保持纯净，不添加 JPA 注解。
- Domain Repository 是领域接口，放在各 bounded context 的 domain 或 application 边界。
- Infrastructure 中提供 JPA Entity、Spring Data JPA Repository、Mapper 和 Repository Adapter。
- 普通结构化业务表通过 Spring Data JPA 落库，例如会话、咖啡记录、偏好、工具调用、Agent 轨迹和 Outbox。
- `memory_embeddings.vector`、HNSW 索引和向量相似度查询通过 JDBC/SQL 实现，避免 JPA 对 pgvector 表达能力不足。
- Flyway 管理所有表结构、pgvector 扩展、普通索引和 HNSW 索引。

事务边界：

- `@Transactional` 放在 Application Service 或明确的应用用例入口上。
- 一个用户命令对应一个清晰事务，例如归档记录、确认发布包、写入反馈。
- 领域对象和 Domain Service 只表达业务规则，不直接开启事务，也不依赖 Spring 注解。
- Repository 不自行扩大事务边界，只参与上层应用事务。
- 核心状态变更与 `DomainEventOutbox` 写入必须在同一个事务中提交。
- 模型调用、小红书自动化、Kafka 投递、图片生成等外部副作用不放进核心数据库事务；需要可靠异步时通过 Outbox 触发。

领域事件：

- 聚合产生 Domain Event。
- Application Service 收集聚合事件。
- Application Service 将事件转换并写入 `DomainEventOutbox`。
- 不通过 Controller 或 Repository 拼装领域事件。
- 不使用 Spring ApplicationEvent 作为 MVP 主链路。

### 4.2 记忆系统

PostgreSQL 保存结构化记录，pgvector 保存语义向量。

第一版同时保留：

- 字段检索：豆名、产区、处理法、风味关键词。
- 向量检索：历史文案、风味描述、用户偏好摘要，默认使用 HNSW 索引。
- Embedding 生成：默认使用阿里云百炼 `text-embedding-v4`，1024 维，写入 `memory_embeddings.vector`。

HNSW 约定：

- 向量列类型使用 `vector(1024)`。
- 默认距离使用 cosine，相应索引使用 `vector_cosine_ops`。
- 第一版建议索引示例：`CREATE INDEX memory_embeddings_vector_hnsw_idx ON memory_embeddings USING hnsw (vector vector_cosine_ops) WITH (m = 16, ef_construction = 64);`
- 查询侧可按需设置 `hnsw.ef_search`，默认先使用 pgvector 默认值，后续通过测试调优召回率和延迟。
- HNSW 不是事实来源，只是召回加速结构；召回结果仍要和结构化字段检索合并、去重、解释原因。

### 4.3 前端工作台

完整工作台至少包含：

- 对话创作区。
- 结构化模板区。
- 感官维度图。
- 风味联想选择器。
- 历史记录与相似记录面板。
- 文案版本对比区。
- 小红书发布预览区。
- Agent 执行过程面板，用于展示规划、工具调用、记忆召回和审稿结果。

## 5. 学习重点

这套选型适合学习：

- Spring Boot 如何组织一个真实业务后端。
- Spring AI 2.x 如何接入 LLM、Embedding、Tool Calling、Advisors、MCP 和 Vector Store。
- PostgreSQL + pgvector 如何支撑长期记忆。
- Kafka 如何承载后置副作用和可扩展事件流。
- REST + SSE 如何支撑工作台快照和实时 Agent 轨迹。
- 分层测试、Fake Adapter 与 Testcontainers 如何验证 Agent 全链路。
- React 工作台如何把 Agent 的中间状态可视化。
- API Key、外部平台工具和用户确认机制如何安全落地。
