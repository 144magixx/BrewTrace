# Phase 0 调研：咖啡品鉴创作 Agent MVP

## 调研范围

本文件解决 MVP 实施计划中的关键技术与集成不确定性。结论服务于 `spec.md` 和 `plan.md`，不包含具体实现代码。

## 1. Java 与 Spring 版本基线

**Decision**：后端建议使用 Java 21 LTS，Spring Boot 4.x，Spring AI 2.x；补丁版本在初始化工程当天按官方稳定版确认。

**Rationale**：Java 21 是当前适合作为新项目基线的 LTS 版本，能覆盖 Spring Boot 4.x 的运行要求，并减少后续升级压力。Spring AI 2.x 面向 Spring Boot 4.0/4.1 能力线，适合学习 Chat、Embedding、Vector Store、Tool Calling、Advisors、MCP 和 Image API 等 Agent 相关能力。

**Alternatives considered**：

- Java 17：兼容性更保守，但新项目学习价值和长期窗口不如 Java 21。
- Java 25：更激进，可能增加本地工具链和库兼容成本。
- Spring Boot 3.x + Spring AI 1.x：更稳定成熟，但不符合用户已确认的“学习最新能力”目标。

参考：

- Spring Boot Reference：https://docs.spring.io/spring-boot/reference/
- Spring AI Reference：https://docs.spring.io/spring-ai/reference/

## 2. Agent 编排方式

**Decision**：MVP 使用“应用内逻辑多 Agent”方式：Orchestrator 负责主流程，Interview、Flavor、Memory、Draft、Review、Publishing 等角色作为 Spring Service 或组件实现。同时保留两种编排模式：默认的显式工作流模式，以及实验性的模型自主工具调用模式。

**Rationale**：用户目标是学习完整 Agent 链路，而不是一开始维护复杂运行时。应用内角色能清楚展示上下文管理、规划、记忆、工具加载和角色协作，同时便于测试和调试。显式工作流模式稳定、可解释、适合作为默认生产路径；模型自主工具调用模式能学习 Spring AI Advisor / Tool Calling 下模型如何选择工具。两种模式共用工具注册、确认机制和 Agent 轨迹，便于对比。

**Alternatives considered**：

- 独立多 Agent 进程：扩展性强，但 MVP 运维和调试成本过高。
- 外部 Agent 框架全托管编排：抽象较重，不利于用户理解每一步真实发生了什么。
- 纯单体 prompt：实现最快，但无法体现规划、工具、记忆和角色边界。
- 只保留显式工作流：更简单，但无法满足用户学习模型自主工具调用的诉求。
- 只保留模型自主工具调用：更 AI 原生，但对事实边界、工具安全和可测试性不如显式工作流稳定。

## 3. 记忆与检索策略

**Decision**：PostgreSQL 保存结构化长期记忆，pgvector 保存语义索引；召回时采用字段检索 + 向量检索 + 结果压缩的混合策略。Embedding 模型使用阿里云百炼 OpenAI 兼容接口 `text-embedding-v4`，默认 1024 维。MVP 初期即为 `memory_embeddings.vector` 创建 HNSW 索引，默认使用 cosine 距离和 `vector_cosine_ops`。

**Rationale**：咖啡记录天然包含豆名、产区、处理法、风味、感官评分、文案等结构化字段，不能只靠 embedding。pgvector 与 PostgreSQL 同库部署，适合 MVP 降低基础设施复杂度。阿里云百炼文档说明其 Embedding 模型兼容 OpenAI 接口，只需替换 `base_url`、`api_key` 和 `model`；`text-embedding-v4` 支持 1024 默认维度，适合第一版记忆检索。HNSW 查询性能和召回表现通常优于 IVFFlat，但构建更慢、内存占用更高；它不需要训练步骤，适合 MVP 从空表开始逐步写入记忆。

**Alternatives considered**：

- 只用 PostgreSQL 字段检索：简单，但对相似文案和模糊风味召回不够好。
- 独立向量库：能力更强，但增加部署和学习负担。
- 只把历史全文塞进 prompt：不可控，容易混淆事实并浪费上下文。
- 继续用文本模型网关生成 embedding：配置更少，但文本生成和向量化的供应商、成本、维度控制会耦合。
- MVP 初期不建 ANN 索引：最简单，但无法提前学习向量索引、参数和执行计划。
- IVFFlat 索引：资源占用相对可控，但需要 lists/probes 和数据量调参，空表阶段不如 HNSW 顺手。

参考：

- 阿里云百炼 OpenAI Embedding 接口兼容文档：https://help.aliyun.com/zh/model-studio/embedding-interfaces-compatible-with-openai
- pgvector HNSW 官方说明：https://github.com/pgvector/pgvector#hnsw

## 3.1 持久化层策略

**Decision**：Domain 对象保持纯净，不添加 JPA 注解；普通结构化业务表使用 Spring Data JPA；pgvector 向量写入、HNSW 索引和相似度查询使用 JDBC/SQL。Infrastructure 层负责 JPA Entity、Spring Data JPA Repository、Mapper 和 Repository Adapter。

**Rationale**：用户希望后端更偏 DDD，领域模型应该表达咖啡记录、风味、文案、记忆、工具调用和发布确认的业务规则，而不是被 ORM 注解和数据库字段牵着走。Spring Data JPA 适合普通业务表的 CRUD、聚合加载和事务管理；pgvector 的向量类型、距离操作符、HNSW 参数和执行计划需要直接 SQL 才清晰可控。这个组合既能学习 DDD 分层，也能学习真实数据库能力。

**Alternatives considered**：

- Domain 直接加 JPA 注解：开发快，但领域对象会被持久化细节污染，后续改表结构容易影响业务规则。
- 全部使用 MyBatis/JDBC：SQL 可控，但样板更多，普通业务表的 Repository 和聚合映射成本更高。
- 完全依赖 JPA 处理 pgvector：抽象统一，但向量操作符、HNSW 索引和查询调参不够直观。

## 3.2 事务边界

**Decision**：Application Service 作为事务边界，使用 `@Transactional` 覆盖单个用户命令的核心状态变更和 `DomainEventOutbox` 写入。Domain 对象和 Domain Service 不开启事务；Repository 参与上层事务。

**Rationale**：本项目很多用例需要同时保存聚合状态、Agent 轨迹、工具调用记录和 Outbox 事件。事务放在 Application Service 能清楚表达“一个用户命令”的一致性边界，也能避免领域模型依赖 Spring。Outbox 必须和核心业务状态同事务提交；模型调用、小红书自动化、Kafka 投递、图片生成等外部副作用不应卡在数据库事务中。

**Alternatives considered**：

- Repository 方法自己控制事务：局部简单，但难以保证跨 Repository 的聚合状态和 Outbox 一致性。
- Domain Service 上直接加事务：使用方便，但领域逻辑会绑定 Spring 框架，不利于纯领域测试。
- Controller 作为事务边界：事务范围容易过大，也会把 Web 层和业务一致性耦合。

## 3.3 领域事件表达

**Decision**：聚合产生 Domain Event，Application Service 收集事件并写入 `DomainEventOutbox`。

**Rationale**：领域事件应该来自业务状态变化，例如咖啡记录归档、发布包确认、工具调用完成。由聚合产生事件，可以让事件和业务规则贴近；由 Application Service 统一收集并写入 Outbox，可以保证事件和核心状态同事务提交。

**Alternatives considered**：

- Application Service 手写 Outbox Event：简单直接，但事件容易变成技术消息，领域语义弱。
- Spring ApplicationEvent 再转 Outbox：框架感强，但链路多一层，MVP 可解释性下降。

## 4. 风味联想体系

**Decision**：采用“基础词库 + 规则扩展 + LLM 辅助描述”的混合方式。候选必须带名称、描述、温度段、香气/味道、正负倾向、适用感知维度和来源类型。

**Rationale**：风味联想的目标是打开表达空间，不是严格复刻 SCA。词库保证常见词稳定返回，LLM 负责更细腻的描述和创作变化，用户确认前一律不能进入事实记录。

**Alternatives considered**：

- 严格 SCA 风味轮：专业但不够自由，和用户“主要发挥想象力”的诉求不完全一致。
- 纯 LLM 联想：表达丰富但稳定性和事实边界不足。
- 纯静态词库：稳定但容易重复，无法解决用户“词穷”痛点。

## 5. 小红书能力封装

**Decision**：后端建立 `XiaohongshuToolAdapter`，封装本机 `xiaohongshu-skills` 的 CLI 能力，例如 `checkLogin`、`searchFeeds`、`getFeedDetail`、`fillPublish`、`clickPublish`、`saveDraft`。Agent 只调用后端工具接口，不直接执行 skill 流程。

**Rationale**：`xiaohongshu-skills` 已提供搜索、详情、发布和认证能力，CLI 输出 JSON，适合封装成结构化工具。适配器能统一确认、限流、错误处理、日志和 Agent 轨迹记录。

**Alternatives considered**：

- Agent 每次临时调用 skill：灵活但不可测试、不可追踪，不利于产品化。
- 自写浏览器自动化：重复造轮子且维护风险更高。
- 官方发布 API：合规性更理想，但当前未确认个人创作者可用的一键发布 API。

## 5.0 模型网关与 Prompt 管理

**Decision**：业务代码只依赖自定义 `ModelGateway`，由其封装 Spring AI 的文本、图片和多模态调用。Prompt 放在 `src/main/resources/prompts/`，按场景和版本管理。

**Rationale**：模型调用需要统一处理超时、2 次短重试、错误分类、轨迹记录和敏感信息遮蔽。如果业务直接依赖 Spring AI Client，模型细节会扩散到各个用例。Prompt 文件化后，可以按风味联想、文案生成、审稿、记忆压缩等场景独立维护，并在 Agent 轨迹里记录 prompt 版本。

**Alternatives considered**：

- 业务直接使用 Spring AI Client：实现快，但后续换模型、做 fake 测试和记录轨迹都更分散。
- 自己写 HTTP Client，不用 Spring AI：控制力强，但放弃 Spring AI 学习目标。
- Prompt 硬编码在 Java 类里：短期简单，但难以复盘、版本化和调整。
- Prompt 存数据库并后台编辑：灵活，但 MVP 暂无后台管理需求。

## 5.1 工具系统抽象

**Decision**：工具系统采用 `ToolRegistry + ToolAdapter + ToolCallPolicy + ToolCallRecorder`。

**Rationale**：显式工作流模式和模型自主工具调用模式都需要共用同一套工具边界。`ToolRegistry` 负责描述工具，`ToolAdapter` 负责封装实际执行，`ToolCallPolicy` 负责风险和确认判断，`ToolCallRecorder` 负责记录轨迹。这样 Agent 不需要知道小红书 CLI、图片模型、pgvector 查询等基础设施细节。

**Alternatives considered**：

- Agent 直接调用具体工具实现：实现快，但不可控、难测试，也不利于右侧轨迹栏展示。
- 每个 UseCase 自己封装工具：局部简单，但确认、限流、错误处理和记录会重复。
- 只依赖 Spring AI Tool Calling 自动注册工具：AI 原生，但显式工作流模式和高风险确认策略不够统一。

## 6. Agent 真实交互侧边栏

**Decision**：将 Agent 轨迹作为一等数据模型持久化。每一步记录 `stepType`、时间、输入摘要、真实 prompt 或引用、模型响应摘要、工具调用、工具结果、记忆召回、审稿判断和最终决策。MVP 默认不脱敏，但 API Key 仍不能写入轨迹。轨迹采用“摘要字段入库 + 原始快照 JSONB 入库”的策略。

**Rationale**：用户明确要求学习 Agent 全链路，侧边栏需要看到真实交互，而不是只看友好解释。摘要字段便于前端卡片列表展示，JSONB 快照便于点击详情查看真实 prompt、模型输出、工具入参、工具返回和记忆召回。轨迹持久化也能支撑后续调试、复盘和文案质量分析。

**Alternatives considered**：

- 只展示自然语言过程说明：安全但学习价值不足。
- 只写日志不做 UI：开发简单，但无法服务工作台学习体验。
- 默认脱敏：更适合多用户产品，但用户已确认 MVP 本地单人使用暂不脱敏。
- 原始全文写本地文件、数据库只存引用：适合更大规模日志，但 MVP 会增加文件生命周期管理复杂度。

## 6.0 文件存储与 Redis 第一版边界

**Decision**：文件存储第一版使用本地文件系统，路径入库，按会话和资源类型分目录。Redis 第一版只预留配置和健康检查，不进入核心链路。

**Rationale**：MVP 面向本地单人使用，豆袋图片、生成图片候选和小红书发布临时文件都适合先落本地文件系统。核心事实仍在 PostgreSQL，避免文件和数据库状态混乱。Redis 未来可用于 SSE 状态、轻量锁或限流，但第一版不必抢主线。

**Alternatives considered**：

- PostgreSQL `bytea` 保存文件：事务一致性强，但文件膨胀数据库，不利于发布临时文件管理。
- MinIO/S3：更生产化，但 MVP 本地运维成本高。
- Redis 缓存工作台快照：性能好，但第一版会增加状态一致性问题。

## 6.1 事务与异步事件

**Decision**：引入 Kafka 作为后端异步事件基础设施。核心业务事务同步写入 PostgreSQL；归档后生成 embedding、偏好推断、发布流程推进、工具结果处理等后置副作用通过 Kafka 事件异步执行。涉及数据库与 Kafka 双写一致性时，采用 PostgreSQL Outbox 表 + 后端定时 Publisher 的 Transactional Outbox 模式。

**Rationale**：用户希望系统便于后续扩展，Kafka 能支撑真实后端系统中的事件流、消费幂等、失败重试和最终一致性学习。核心业务不应该等待 embedding、偏好推断或外部工具操作完成，异步事件可以降低主流程阻塞。PostgreSQL Outbox + 定时 Publisher 比 Debezium CDC 更适合 MVP：部署简单、逻辑可见、便于用户理解事务一致性。

**Alternatives considered**：

- 同步 Application Service + Spring Application Event：更轻，但扩展到独立消费者和失败重试时能力不足。
- 全部同步一个事务做完：简单但会让模型、向量、小红书工具失败影响核心记录保存。
- 直接业务事务后发送 Kafka、不做 outbox：实现较快，但存在数据库提交和事件发送不一致风险。
- Debezium CDC Outbox：更生产级，但 MVP 本地环境和学习成本更高，可作为后续升级方向。

## 6.2 本地基础设施与密钥管理

**Decision**：MVP 本地基础设施使用 Docker Compose 管理 PostgreSQL/pgvector、Kafka、Kafka UI 与 Redis；真实模型 Key 和网关配置放在仓库外私有 env 文件，例如 `~/.config/xhs-coffee-agent/env`，应用启动前注入环境变量。

**Rationale**：用户需要学习 PostgreSQL、pgvector、Kafka、Outbox 和 Agent 工具链，但不需要在 MVP 阶段手动安装和维护这些基础设施。Docker Compose 能固定本地依赖版本，也方便后续测试环境复现。Kafka UI 能直观看到事件投递、消费和失败重试；Redis 为后续会话缓存、SSE 连接状态、轻量分布式锁或限流预留基础，但核心事实仍以 PostgreSQL 为准。密钥放在仓库外可以避免误提交，同时保留本地真实模型验证能力。

**Alternatives considered**：

- 手动安装 PostgreSQL、pgvector 和 Kafka：学习基础设施细节更多，但环境差异大，排错成本高。
- 把后端、前端也放进 Compose：更接近部署，但本地开发调试 Spring Boot 与 Vite 不够轻便。
- 仓库内 `.env.local` 保存真实 Key：启动方便，但误提交风险高，不符合项目安全约束。
- 暂不引入 Redis：MVP 更轻，但后续做 SSE 状态、限流或锁时需要再补基础设施。
- 暂不引入 Kafka UI：组件更少，但不利于学习和调试事件流。

## 6.3 后端配置绑定

**Decision**：后端使用 Spring Boot `@ConfigurationProperties` 按子系统绑定配置，例如 `ModelProperties`、`EmbeddingProperties`、`XiaohongshuProperties`、`AgentProperties`、`StorageProperties`、`RedisProperties`。

**Rationale**：本项目配置项会覆盖模型网关、Embedding、小红书 skill、Agent 策略、上传目录、Redis TTL、Kafka/Outbox 等多个边界。类型安全配置能集中表达必填项、默认值、校验规则和敏感字段处理，也更利于用户学习 Spring Boot 工程化配置。业务服务只依赖配置对象或网关抽象，避免到处散落 `@Value`。

**Alternatives considered**：

- 直接使用 `@Value`：简单，但配置项增长后难以测试、难以发现缺失项，也容易把敏感值打进日志。
- 自定义配置中心抽象：扩展性强，但 MVP 本地单用户阶段过重。
- 在业务代码里直接读取环境变量：短期最快，但破坏 Spring 配置体系，也不利于单元测试。

## 6.4 用户模型与鉴权边界

**Decision**：MVP 使用本地单用户模式，固定当前用户为 `local-user`，不实现登录、注册、Token、Session、RBAC 或多用户权限。数据模型和事件仍保留 `userId` 字段，第一版默认写入 `local-user`。

**Rationale**：当前项目主要服务作者本人，重点是学习 Agent 上下文、规划、记忆、工具调用、多 Agent 协作、Kafka/Outbox 和工作台可视化。登录鉴权不会提升主流程学习价值，反而会分散 MVP 精力。保留 `userId` 字段可以避免后续升级多用户时大规模重构。

**Alternatives considered**：

- 单用户但增加本地密码或 Token：安全性略高，但当前本地个人使用收益有限。
- 一开始做多用户和 RBAC：更产品化，但会显著增加表结构、前端状态和测试复杂度。
- 完全不保留 `userId`：实现更简单，但后续扩展多用户、偏好隔离和记忆隔离会更痛。

## 7. 前端工作台状态方案

**Decision**：React + Vite + TypeScript 作为已确认前端栈；服务端状态使用 TanStack Query，本地工作台状态使用 Zustand，复杂模板表单使用 React Hook Form + Zod，感官维度图使用 ECharts 或 Recharts。

**Rationale**：工作台包含对话流、模板表单、风味候选、历史召回、文案版本、发布确认和 Agent 轨迹侧边栏，需要明确区分服务端数据、本地 UI 状态和表单状态。

**Alternatives considered**：

- 只用 React state：初期快，但复杂工作台状态会很快混乱。
- Redux Toolkit：规范但样板偏多，MVP 学习成本较高。
- Next.js：适合全栈 Web，但当前后端学习重点在 Spring Boot，Vite 更轻。

## 7.1 REST + SSE 通信形态

**Decision**：后端 API 采用 REST + SSE。REST 负责命令和查询，SSE 负责实时推送 Agent 轨迹、工具调用状态、记忆召回结果和 Kafka 异步事件进度。

**Rationale**：工作台需要在右侧轨迹栏实时展示 Agent 正在做什么。纯 REST 轮询会增加无效请求，WebSocket 对 MVP 来说偏重且需要双向协议管理。SSE 对“服务端持续推状态、前端展示增量”的场景更简单，且浏览器原生支持。

**Alternatives considered**：

- 纯 REST 轮询：实现简单，但实时性和请求效率较差。
- WebSocket：双向能力更强，但 MVP 当前不需要复杂双向长连接。
- 只等 workflow 结束后返回完整结果：实现最简单，但无法满足学习侧边栏实时观察诉求。

## 7.1.1 API 响应 Envelope

**Decision**：JSON REST 响应统一使用 `{ requestId, data, error }` envelope。成功时 `error=null`，失败时 `data=null`，错误对象包含 `code`、`category`、`message`、`recoverable`、`nextActions`、`details`。

**Rationale**：工作台需要把用户动作、后端请求、Agent 轨迹、工具调用和错误卡片串起来。统一 envelope 能让前端请求处理、错误提示、调试侧边栏和日志关联保持一致，也方便后续做全局异常处理和契约测试。

**Alternatives considered**：

- 成功直接返回业务 DTO、错误返回统一结构：更常见，但前端需要分别处理成功和失败的追踪字段。
- 完全按 Spring 默认响应：最快，但 `requestId`、错误分类和 Agent 轨迹关联会变弱。
- 每个业务 DTO 自带 `requestId`：局部可行，但会污染业务对象，也容易出现字段遗漏。

## 7.1.2 API 契约来源

**Decision**：MVP 先维护 Markdown 契约，编码时同步生成或补充 OpenAPI。

**Rationale**：当前阶段需求仍在确认，Markdown 契约更适合快速讨论和中文说明；进入编码后可以用 OpenAPI 辅助前后端联调和测试，但不让 Controller 成为唯一事实来源。

**Alternatives considered**：

- OpenAPI First：规范强，但当前需求变化频繁，编辑成本更高。
- 只靠 Controller：开发快，但文档、前端和测试容易滞后。

## 7.2 错误处理与降级策略

**Decision**：采用分级错误 + 可恢复降级。错误等级包括 `USER_FIXABLE`、`RETRYABLE`、`DEGRADED`、`FATAL`、`SAFETY_BLOCKED`。

**Rationale**：Agent 系统依赖模型、向量、Kafka、小红书自动化和 SSE 长连接，任何一个外部能力都可能失败。用户最关心的是已填写内容不丢、能继续创作、能知道下一步怎么处理。分级错误能让后端、前端、工具策略和 Agent 轨迹使用同一套语言。

**Alternatives considered**：

- 只分可重试/不可重试/用户处理：实现更简单，但无法表达安全阻断和降级创作。
- 任意错误都终止 workflow：最简单，但用户体验差，也不符合外部工具易波动的现实。
- 完全由前端自行解释错误：会导致提示不一致，且缺少 Agent 轨迹记录。

## 7.3 测试策略

**Decision**：采用分层测试 + Fake Adapter + Testcontainers。

**Rationale**：本项目同时包含传统后端业务、Agent 编排、模型调用、工具调用、向量检索、Kafka 事件和前端工作台。真实模型和小红书自动化都不可完全稳定依赖，因此自动化测试必须通过 fake adapter 固定外部行为，同时用 Testcontainers 覆盖 PostgreSQL、pgvector 和 Kafka 的真实集成边界。

**Alternatives considered**：

- 以集成测试为主：贴近真实系统，但测试慢，定位问题困难。
- 以 E2E/人工验证为主：前期快，但回归成本高，Agent 行为容易被改坏。
- 自动化测试直接调用真实模型和小红书：看似真实，但成本、稳定性和账号风险都不可控。

## 8. 模型与图片能力

**Decision**：文本模型通过配置接入用户提供的 `gpt-5.5`；图片模型 `gpt-image-2` 仅在用户主动请求生图时调用；Embedding 模型通过阿里云百炼 `text-embedding-v4` 接入。豆袋图片解析作为多模态输入能力，结果全部标记为待确认。

**Rationale**：用户已确认模型来源；生图不是每次记录的必经流程。把图片生成限定为主动触发，可以降低成本并避免把创意图误解成真实品鉴事实。Embedding 独立配置有利于控制向量维度、成本和供应商边界。

**Alternatives considered**：

- 每次自动生成配图：体验热闹但成本高，也可能干扰真实记录。
- 不做图片能力：降低 MVP 范围，但无法覆盖用户明确提出的多模态学习目标。
- 复用文本模型网关做 embedding：实现简单，但不利于后续独立调优检索质量。

## 9. 发布确认策略

**Decision**：发布流程分三段：生成发布包、填写发布页、预览后二次确认并点击发布。任何公开发布、评论、点赞、收藏动作都必须有明确确认记录。

**Rationale**：小红书操作涉及账号和公开内容风险。分段流程能让用户在浏览器中看到真实发布页，失败时也能保留草稿和发布包。

**Alternatives considered**：

- 一键直接发布：效率最高，但误发风险不可接受。
- 只生成发布包完全手动发：最稳，但无法验证用户要的自动化能力。
