# 技术选型学习文档 v0.1

## 1. 学习目标

这份文档解释咖啡品鉴创作 Agent MVP 为什么选择当前技术方案，以及这些选择带来的好处。目标不是堆技术，而是帮助你理解一个 Agent 系统从业务、后端、模型、记忆、工具到可观测性的完整链路。

## 2. Spring Boot 4.x + Spring AI 2.x

选择原因：

- Spring Boot 是 Java 后端主流工程化底座，适合你已有后端经验。
- Spring AI 能把 Chat、Embedding、Tool Calling、Image、Advisor 等 Agent 能力放进 Spring 生态。
- 4.x / 2.x 能学习较新的 Spring 能力线。

好处：

- 可以把 Agent 能力当成普通后端能力来设计、测试和演进。
- 配置、依赖注入、事务、测试、Web API、Kafka 都能复用 Spring 体系。
- 后续可以逐步理解 Spring AI，而不是一开始被独立 Agent 框架包住。

## 3. 模块化单体 + DDD 分层

选择原因：

- MVP 不需要一开始拆微服务。
- 业务边界已经比较清楚：品鉴、风味、文案、记忆、工具、发布、轨迹。
- 用户希望学习更真实的后端设计。

好处：

- 代码结构能表达业务，而不是只有 `controller/service/repository`。
- Domain 对象不被数据库注解污染，更适合写领域规则和单元测试。
- 后续如果某个边界变复杂，可以再拆服务。

## 4. Application Service 事务边界

选择原因：

- 用户命令通常要同时修改多个对象，例如归档记录、写 Agent 轨迹、写 Outbox。
- Domain 不应该依赖 Spring 事务。
- Repository 自己开事务会破坏跨聚合一致性。

好处：

- “一次用户动作”的一致性边界清楚。
- 核心状态和 Outbox 能同提交、同回滚。
- 领域模型更纯，测试更容易。

## 5. PostgreSQL + pgvector + HNSW

选择原因：

- 咖啡记录既有结构化字段，也有语义相似度需求。
- PostgreSQL 能保存核心事实，pgvector 能做长期记忆召回。
- HNSW 更适合提前学习可扩展向量召回，不需要 IVFFlat 的训练步骤。

好处：

- 可以同时学习字段检索和向量检索，不会把一切都塞进 prompt。
- 记忆召回能解释“为什么相似”，例如同风味、同处理法、文案表达相近。
- HNSW 让后续记录变多时仍有扩展空间。

## 6. Spring Data JPA + JDBC/SQL

选择原因：

- 普通业务表适合 JPA，提高开发效率。
- pgvector 的向量类型、距离操作符和 HNSW 参数更适合直接 SQL。

好处：

- 普通 CRUD 不必写大量样板 SQL。
- 向量查询保持可控，可看执行计划，可调参数。
- 学到的是实际工程里常见的“ORM + 专项 SQL”组合。

## 7. Kafka + PostgreSQL Outbox

选择原因：

- 归档后生成 embedding、偏好推断、发布流程推进都不该阻塞主流程。
- 直接在事务后发 Kafka 会有双写不一致风险。
- Outbox 模式能清楚展示最终一致性。

好处：

- 用户保存记录后不用等所有后置任务完成。
- 数据库提交和事件投递之间有可靠桥梁。
- 可以学习事件、重试、幂等、死信状态和异步副作用。

## 8. REST + SSE + 统一 Envelope

选择原因：

- REST 适合命令和查询。
- SSE 适合把 Agent 轨迹持续推给前端。
- 统一 `{ requestId, data, error }` 便于前端处理和调试。

好处：

- 工作台能实时看到模型调用、工具调用、记忆召回和错误。
- `requestId` 能串起前端请求、后端日志、AgentTrace 和 ToolCall。
- 错误分类稳定，前端不会到处写特殊判断。

## 9. ModelGateway 包装 Spring AI

选择原因：

- 业务不应该直接依赖具体模型 SDK。
- 文本模型、图片模型、Embedding 的失败、重试、日志和轨迹规则需要统一。

好处：

- 将来换模型或换网关时影响更小。
- 可以统一做 2 次短重试、超时、错误分类和敏感信息遮蔽。
- Agent contract test 可以用 `FakeModelGateway` 固定输出。

## 10. Prompt 文件化和版本化

选择原因：

- Prompt 是 Agent 系统的重要资产，不应该散落在 Java 字符串里。
- 你需要看到真实 prompt，并学习它如何影响模型输出。

好处：

- prompt 可以按场景管理，例如风味联想、文案生成、审稿。
- 轨迹里能记录 prompt 版本，方便复盘。
- 修改 prompt 不必翻业务代码。

## 11. ToolRegistry + ToolAdapter

选择原因：

- Agent 不能随意执行脚本或外部动作。
- 小红书发布、生图等动作有风险和成本。

好处：

- 工具是否可调用、是否需要确认、风险等级都可控。
- 显式工作流和模型自主工具调用共用同一套工具边界。
- 工具调用结果能进入 Agent 轨迹侧边栏。

## 12. Fake Adapter + Testcontainers

选择原因：

- 真实模型、小红书账号和页面自动化都不稳定。
- PostgreSQL、pgvector、Kafka 又需要接近真实环境验证。

好处：

- Agent 行为用 Fake 保持可重复。
- 数据库、向量和 Kafka 用 Testcontainers 验证真实边界。
- 测试既稳定，又不会把关键基础设施完全 mock 掉。

## 13. 本地文件系统和 Redis 预留

选择原因：

- MVP 是本地个人使用，不需要一开始引入对象存储。
- Redis 暂时不是核心依赖，但后续可用于 SSE 状态、轻量锁或限流。

好处：

- 文件上传、发布临时文件、生图结果可以快速落地。
- 核心事实仍在 PostgreSQL，系统更容易理解。
- Redis 不抢主线，等确实需要时再进入核心链路。

## 14. 你应该怎么学

建议学习顺序：

1. 先看 `docs/architecture/backend-design-v0.1.md`，理解后端整体分层。
2. 再看 `specs/001-coffee-agent-mvp/data-model.md`，理解领域对象。
3. 接着看 `specs/001-coffee-agent-mvp/contracts/event-contracts.md`，理解 Outbox 和 Kafka。
4. 然后看 `specs/001-coffee-agent-mvp/contracts/tool-contracts.md`，理解 Agent 工具边界。
5. 最后看 `docs/architecture/frontend-design-v0.1.md`，理解为什么要把 Agent 过程可视化。

学习时重点问三个问题：

- 这一步的事实来源是什么？
- 这一步失败后用户数据会不会丢？
- 这一步能不能在 Agent 轨迹里被看见？

