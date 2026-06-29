# Agent 技术架构设计 v0.1

## 1. 文档目标

本文描述咖啡品鉴创作 Agent 的第一版技术架构。当前文档只做架构方案和学习路径沉淀，不最终锁定技术选型。涉及框架、数据库、向量库、前端形态等选择，必须在实现前与用户确认。

## 2. 架构目标

- 支持对话模式和结构化模板模式。
- 支持上下文管理、规划、记忆、工具加载、多 Agent 协作五类核心学习目标。
- 支持豆袋图片解析、风味联想、历史记录召回、文案生成、发布包生成。
- 保持第一版足够简单，避免过早引入复杂编排框架。

## 3. 总体分层

```text
Client/UI
  -> API Layer
  -> Agent Orchestrator
      -> Context Manager
      -> Planner
      -> Memory Service
      -> Tool Registry
      -> Agent Roles
  -> Domain Services
      -> Coffee Record Service
      -> Flavor Suggestion Service
      -> Draft Service
      -> Publishing Package Service
  -> Infrastructure
      -> LLM Client
      -> Image Model Client
      -> Database
      -> Kafka
      -> Vector Search
      -> Web/Search Adapter
      -> File Storage
```

持久化分层约定：

- Domain 对象不加 JPA 注解，保持业务语义纯净。
- Infrastructure 负责 JPA Entity、Spring Data JPA Repository、JDBC/SQL、Mapper 和 Repository Adapter。
- 普通业务表使用 Spring Data JPA；pgvector 向量写入、HNSW 索引和相似度查询使用 JDBC/SQL。

事务分层约定：

- Application Service 是事务边界，负责在一个事务内完成核心状态变更和 Outbox 写入。
- Domain Service 和聚合不标注 `@Transactional`，只表达业务规则。
- Repository 参与上层事务，不自行决定跨用例事务范围。
- 外部模型调用、工具调用和 Kafka 投递不放入核心数据库事务。

## 4. 核心模块

### 4.1 API Layer

对外提供接口，承接前端或命令行调用。

API 通信形态采用 REST + SSE：

- REST：处理命令和查询，例如创建会话、提交对话、更新模板、获取 workspace 快照、确认发布。
- SSE：实时推送 Agent 轨迹、模型调用步骤、工具调用进度、记忆召回结果和异步事件状态。
- 前端首屏通过 REST 获取当前状态，再通过 SSE 订阅后续增量。
- SSE 只推送状态，不直接执行高影响动作。
- REST 响应统一使用 `{ requestId, data, error }` envelope；成功时 `error=null`，失败时 `data=null`。
- `requestId` 用于串联前端请求、服务端日志、AgentTrace、ToolCall 和错误记录。

第一版建议接口：

- 创建品鉴会话。
- 提交对话消息。
- 提交模板字段。
- 获取风味联想。
- 生成文案。
- 保存最终记录。
- 生成发布包。
- 订阅会话事件流。

### 4.2 Agent Orchestrator

负责一次任务的主流程编排。

职责：

- 识别用户意图。
- 请求 Planner 生成步骤。
- 调用记忆、工具和角色 Agent。
- 汇总结果并输出给用户。

第一版保留两种 Agent 编排模式，便于同时满足稳定实用和学习对比：

- 显式工作流模式：Planner 输出结构化步骤，Orchestrator 逐步执行。该模式作为默认模式，便于调试、测试和解释。
- 模型自主工具调用模式：基于 Spring AI Advisor / Tool Calling，让模型在受控工具注册表内自主选择工具。该模式用于学习更 AI 原生的工具调用链路。

两种模式都必须经过同一套 `ToolRegistry`、确认机制和 `AgentTrace` 记录，不允许绕过工具安全边界。

### 4.3 Context Manager

维护当前任务上下文。

内容包括：

- 本次咖啡豆信息。
- 冲煮参数。
- 感官评分。
- 高温、中温、低温的香气和味道记录。
- 用户已确认、拒绝或待确认的信息。
- 当前草稿和修改意见。

关键原则：

- 不把未确认的模型推断写成事实。
- 对外部参考内容标记来源。
- 控制进入大模型的上下文长度。

### 4.4 Planner

将用户输入拆解为可执行步骤。

示例：

```text
用户：别人怎么评价这支豆子？
计划：
1. 从当前上下文提取咖啡豆名和关键风味。
2. 调用外部检索工具。
3. 摘要外部观点。
4. 区分用户真实感受和外部参考。
```

显式工作流模式下，Planner 使用规则 + LLM 输出结构化计划，不急于做复杂推理树。

模型自主工具调用模式下，Planner 不直接展开所有步骤，而是负责生成任务目标、工具边界和安全约束，再由 Spring AI Tool Calling 在允许工具集合内执行。该模式必须把模型选择工具的原因、工具入参、工具结果和后续决策写入轨迹。

### 4.5 Memory Service

负责短期记忆和长期记忆。详细设计见 [memory-design-v0.1.md](./memory-design-v0.1.md)。

### 4.6 Tool Registry

工具注册中心，用统一协议描述可调用工具。

工具元信息：

- `name`：工具名。
- `description`：何时使用。
- `inputSchema`：输入结构。
- `outputSchema`：输出结构。
- `requiresConfirmation`：是否需要用户确认。

工具系统采用 `ToolRegistry + ToolAdapter + ToolCallPolicy + ToolCallRecorder`：

- `ToolRegistry`：注册所有可用工具定义，提供给显式工作流和模型自主工具调用模式。
- `ToolAdapter`：封装具体工具实现，例如小红书 CLI、图片生成、豆袋图片解析、记忆召回。
- `ToolCallPolicy`：判断当前模式、用户确认状态、风险等级和工具是否允许执行。
- `ToolCallRecorder`：记录工具调用目的、入参摘要、返回摘要、确认状态、失败原因和耗时。

Agent 不直接调用 CLI、HTTP SDK 或模型 SDK。所有外部能力必须通过工具适配器进入，便于测试、审计和前端轨迹展示。

候选工具：

- 豆袋图片解析。
- 风味联想。
- 历史记录检索。
- 外部内容检索。
- 文案相似度检测。
- 图片生成。
- 发布包生成。
- 小红书搜索和发布工具，默认基于本地 `xiaohongshu-skills` 适配。

### 4.7 Agent Roles

第一版建议使用“逻辑多 Agent”，即同一后端进程中的多个角色服务：

- 采访 Agent：补齐信息。
- 感官 Agent：细化风味。
- 冲煮 Agent：分析参数。
- 检索 Agent：查外部参考。
- 文案 Agent：生成标题、正文、标签。
- 审稿 Agent：检查虚构、重复、风格偏差。
- 记忆 Agent：归档和召回。
- 发布 Agent：生成发布包或调用发布工具。

后续如果角色间协作变复杂，再升级为独立 agent 编排。

### 4.8 Event Bus

后端引入 Kafka 作为异步事件总线，承载核心业务提交后的后置副作用。

适用事件：

- `CoffeeRecordArchivedEvent`：归档后生成 embedding、写入长期记忆、推断用户偏好候选。
- `DraftSetGeneratedEvent`：文案生成后触发审稿、重复表达检测或候选归档。
- `PublishingPackageConfirmedEvent`：用户确认发布包后准备发布临时文件、推进填写发布页。
- `ToolCallCompletedEvent`：工具调用完成后更新轨迹、状态和失败处理。
- `AgentTraceCompletedEvent`：一次 Agent workflow 结束后用于后续学习分析。

设计原则：

- Kafka 处理后置副作用，不替代同步用例的核心事务。
- 公开发布、评论、点赞、收藏、生图等高影响或产生成本动作，即使由事件触发，也必须验证用户确认状态。
- 可靠事件发布使用 PostgreSQL Outbox 表 + 后端定时 Publisher。业务事务中先写入 outbox，再由后台定时 publisher 投递到 Kafka。
- Kafka 消费者必须支持幂等处理，避免重复消费导致重复发布、重复写入偏好或重复生成向量。

Outbox Publisher 规则：

- 定时扫描 `PENDING` 和到达 `nextRetryAt` 的 `FAILED_RETRYABLE` 事件。
- 每批按 `createdAt` 正序读取，避免旧事件长期饥饿。
- 发送成功后标记 `PUBLISHED` 并写入 `publishedAt`。
- 发送失败时增加 `retryCount`，按退避策略写入 `nextRetryAt`。
- 超过重试上限后标记 `FAILED_DEAD`，等待人工处理。
- 多实例运行时必须通过数据库锁或状态抢占避免同一 outbox 事件被多个 publisher 同时投递。

### 4.9 Error Handling

错误处理采用“分级错误 + 可恢复降级”。

错误等级：

- `USER_FIXABLE`：用户可处理，例如缺字段、未登录、验证码、未确认发布。
- `RETRYABLE`：可重试，例如模型超时、网络波动、Kafka 临时失败。
- `DEGRADED`：可降级，例如外部参考失败、Embedding 失败、SSE 断线。
- `FATAL`：致命错误，例如数据库不可用、关键配置缺失。
- `SAFETY_BLOCKED`：安全阻断，例如未确认高影响动作、疑似泄露 Key。

处理原则：

- 模型失败：保留会话和已填信息，允许重试。
- Embedding 失败：咖啡记录照常归档，事件进入重试或失败终止状态。
- Kafka/Outbox 失败：不回滚已提交核心业务记录，记录失败原因并重试。
- 小红书未登录、验证码、风控：停止发布流程，保留发布包并提示用户处理。
- SSE 断线：前端通过 REST workspace 快照恢复状态，再重新订阅。
- 外部参考失败：降级为无外部参考创作。
- 高影响动作未确认：直接阻断，不允许自动降级执行。

所有错误都必须写入 Agent 轨迹或工具调用记录，前端用系统状态卡片展示。

### 4.10 Testing

测试采用分层策略：

- 领域层单元测试：验证聚合状态变化、事实边界、风味候选接受/拒绝、发布确认状态。
- 应用层用例测试：验证 `SubmitConversationTurnUseCase`、`GenerateDraftSetUseCase`、`ArchiveCoffeeRecordUseCase` 等编排逻辑。
- Agent contract 测试：用 `FakeModelGateway` 固定模型输出，用 `FakeToolAdapter` 固定工具返回，验证 prompt、工具调用、记忆召回和 AgentTrace。
- 基础设施集成测试：用 Testcontainers 验证 PostgreSQL、pgvector、Kafka、Outbox Publisher、SSE event stream。
- 人工/E2E 验证：验证真实模型调用、小红书登录/验证码/发布页填写和前端 Playwright 流程。

关键原则：

- 自动化测试默认不调用真实模型、不操作真实小红书账号。
- 高影响动作必须通过 fake adapter 验证确认边界，再通过人工流程验证真实工具。
- 每个可交付切片必须至少覆盖后端行为、前端行为、持久化影响和 Agent 轨迹。

## 5. 技术选型决策

详细决策见 [technology-decisions-v0.1.md](./technology-decisions-v0.1.md)。

### 5.1 后端框架

已确认：Spring Boot 4.x。

原因：

- 用户是 Java 后端开发，Spring Boot 学习迁移成本低。
- 生态成熟，便于接入 Web API、数据库、测试、配置管理。
- 后续可结合 Spring AI 2.x 学习 LLM、Embedding、Vector Store、Tool Calling、Advisors 和 MCP。

### 5.2 构建工具

已确认：Maven。

### 5.3 数据库

已确认：PostgreSQL。

### 5.4 向量检索

已确认：pgvector。

索引策略已确认：HNSW。

第一版约定：

- `memory_embeddings.vector` 使用 `vector(1024)`。
- 默认使用 cosine 距离和 `vector_cosine_ops`。
- Flyway 初始化时创建 HNSW 索引。
- 初始参数建议 `m = 16`、`ef_construction = 64`，后续通过 Testcontainers 集成测试和真实记录调优。
- HNSW 负责加速语义召回，不替代结构化字段检索、事实边界和相似原因解释。

### 5.5 前端形态

已确认：完整工作台。

工作台需要支持：

- 对话模式。
- 结构化模板模式。
- 感官维度图。
- 风味联想选择。
- 历史记忆召回。
- 文案版本对比。
- 小红书发布预览。

前端框架已确认：React + Vite + TypeScript。

推荐配套：

- TanStack Query：服务端状态和接口缓存。
- Zustand：工作台本地状态。
- React Hook Form + Zod：结构化模板和校验。
- ECharts 或 Recharts：感官维度图。
- shadcn/ui + Radix UI：组件体系候选。

### 5.6 模型与密钥

已确认：

- 文本模型：用户提供 `gpt-5.5` 接口。
- 图像模型：用户提供 `gpt-image-2` 接口。
- Embedding 模型：阿里云百炼 `text-embedding-v4`，OpenAI 兼容接口，默认 1024 维。
- API Key 形式：`sk-xxxxx`。

安全约束：

- API Key 只通过环境变量或本地配置注入。
- 不写入 Git。
- 不在日志中打印完整 Key。
- 文档和示例只使用占位符。
- 本地真实密钥放在仓库外私有 env 文件，例如 `~/.config/xhs-coffee-agent/env`。

配置约定：

- `OPENAI_API_KEY`、`OPENAI_BASE_URL`、`TEXT_MODEL`：文本模型。
- `IMAGE_MODEL`：图像模型。
- `EMBEDDING_API_KEY`、`EMBEDDING_BASE_URL`、`EMBEDDING_MODEL`、`EMBEDDING_DIMENSIONS`：Embedding 模型。
- 后端使用 Spring Boot `@ConfigurationProperties` 按子系统绑定配置，不在业务代码中散落使用 `@Value`。
- 建议配置类包括 `ModelProperties`、`EmbeddingProperties`、`XiaohongshuProperties`、`AgentProperties`、`StorageProperties`、`RedisProperties`。

说明：

- 文本/图像模型与 Embedding 模型可以使用不同网关和不同 Key。
- Embedding 调用用于长期记忆、相似文案、用户偏好摘要和外部参考摘要向量化。
- `application.yml` 只保存非敏感默认值和环境变量占位符；真实 Key 从仓库外 env 文件注入。

### 5.6.1 本地基础设施

已确认：Docker Compose。

第一版只用 Compose 管理基础设施：

- PostgreSQL + pgvector。
- Kafka。
- Kafka UI，用于学习和观察事件流。
- Redis，用于后续会话缓存、SSE 连接状态、轻量分布式锁或限流，不作为核心事实存储。

后端、前端、小红书自动化和真实模型调用不放进 Compose。后端使用本机 `mvn spring-boot:run` 或 IDE 启动，前端使用 Vite，本机 Chrome 继续承载 `xiaohongshu-skills` 自动化。

### 5.6.2 用户模型

已确认：MVP 本地单用户。

设计边界：

- 系统固定当前用户为 `local-user`。
- 不实现登录、注册、Token、Session、RBAC 或多用户权限。
- 所有核心数据仍保留 `userId` 字段，第一版默认写入 `local-user`。
- 应用层通过 `CurrentUserProvider` 获取用户标识，避免散落硬编码，后续可替换为真实认证上下文。
- 小红书账号登录态只归属于 `xiaohongshu-skills` 工具适配器，不作为本系统鉴权来源。

### 5.7 小红书能力

已确认：后续抓帖和发帖优先使用本地 `xiaohongshu-skills`。

### 5.8 异步事件

已确认：Kafka。

原因：

- 用户希望后端具备后续扩展能力。
- 记忆向量生成、偏好推断、工具结果处理和发布状态推进适合异步化。
- Kafka 能帮助学习真实后端系统中的事件流、消费幂等、失败重试和最终一致性。

## 6. 关键流程

### 6.1 Agent 编排模式

#### 6.1.1 显式工作流模式

```text
User Message
-> Context Manager 更新会话
-> Planner 输出结构化 PlanStep
-> Orchestrator 按步骤调用记忆、工具和角色 Agent
-> Review Agent 检查事实边界
-> Trace Recorder 记录每一步
-> Workspace View 返回前端
```

适用场景：

- 默认日常使用。
- 需要稳定、可解释、易测试的流程。
- 学习上下文管理、规划和显式工具编排。

#### 6.1.2 模型自主工具调用模式

```text
User Message
-> Context Manager 更新会话
-> Agent Orchestrator 组装任务目标、上下文和工具约束
-> Spring AI Tool Calling / Advisor 让模型选择工具
-> Tool Registry 校验工具、确认要求和风险等级
-> Review Agent 检查输出
-> Trace Recorder 记录模型选择、工具调用和结果
-> Workspace View 返回前端
```

适用场景：

- 学习模型自主工具选择。
- 对比显式工作流和 Tool Calling 的行为差异。
- 探索 Spring AI Advisor、Tool Calling 和受控工具边界。

约束：

- 默认模式仍为显式工作流模式。
- 模型自主工具调用模式不得自动执行公开发布、评论、点赞、收藏、生图等高影响或产生成本动作。
- 高影响动作仍必须走用户确认和后端工具适配器。
- 前端必须展示当前使用的编排模式。

### 6.2 对话模式

```text
User Message
-> Context Manager 更新会话
-> Planner 判断是否需要追问
-> Interview Agent 生成问题或交给文案 Agent
-> Draft Agent 生成草稿
-> Review Agent 检查
-> 用户确认
-> Memory Agent 存档
```

### 6.3 模板模式

```text
Template Input
-> 字段校验
-> Flavor Agent 根据输入做联想
-> 用户选择风味
-> Draft Agent 生成文案
-> Review Agent 检查
-> Publishing Package Service 生成发布包
-> Memory Agent 存档
```

### 6.4 外部参考模式

```text
用户请求别人评价
-> Planner 生成检索计划
-> Search Tool 检索
-> Retrieval Agent 摘要
-> Context Manager 标记为外部参考
-> Draft Agent 只借鉴表达方向，不复制内容
```

第一版小红书外部参考优先通过 `xiaohongshu-skills` 的 `search-feeds` 和 `get-feed-detail` 实现。检索结果只作为创作参考，不直接复制正文。

### 6.5 小红书发布模式

```text
用户确认发布
-> Publishing Package Service 生成标题、正文、标签、图片
-> Xhs Publish Tool 调用 xiaohongshu-skills 的 fill-publish
-> 用户在浏览器中确认预览
-> 用户二次确认
-> Xhs Publish Tool 调用 click-publish
-> 记录发布结果或保存草稿
```

发布工具必须满足：

- 发布前检查登录状态。
- 发布和评论操作必须用户确认。
- 优先分步发布，不直接跳过预览。
- 失败时降级为发布包或草稿。

### 6.6 归档与记忆异步流程

```text
用户确认归档
-> ArchiveCoffeeRecordUseCase 写入 CoffeeRecord / DraftSet 状态 / AgentTrace
-> 同一事务写入 Outbox Event
-> 后端定时 Outbox Publisher 投递 CoffeeRecordArchivedEvent 到 Kafka
-> Memory Consumer 生成 embedding 并写入 memory_embeddings
-> Preference Consumer 生成用户偏好候选
-> Trace Consumer 更新可学习过程摘要
```

说明：

- 用户归档的核心结果不等待 embedding 完成。
- embedding 或偏好推断失败时，保留失败记录并允许重试。
- 任何消费者不得把模型推断直接写成用户确认事实。

## 7. 学习路径

建议按以下顺序学习和实现：

1. 先理解一次 agent 调用的生命周期：输入、上下文、计划、工具、输出。
2. 实现一个无记忆的对话式文案生成。
3. 加入短期上下文，支持多轮追问。
4. 加入结构化模板，学习如何把业务对象转成模型上下文。
5. 加入长期记忆，学习检索增强生成。
6. 加入工具注册中心，学习 function calling/tool calling 思想。
7. 加入审稿 Agent，理解多角色协作。
8. 最后再研究外部发布工具和自动化风险。

## 8. 待确认问题

- Spring Boot 4.x 与 Spring AI 2.x 的具体补丁版本。
- UI 组件库最终使用 shadcn/ui 还是其他方案。
- `xiaohongshu-skills` 集成的安全边界和确认流程细节。
- pgvector MVP 初期已确认启用 embedding，并使用 HNSW 索引。
