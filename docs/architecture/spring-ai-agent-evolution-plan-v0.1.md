# Spring AI Agent 能力演进建议 v0.1

## 1. 文档目标

本文记录当前咖啡品鉴创作 Agent 如何继续使用 Spring AI 的 Agent 能力。重点不是把现有业务编排替换成一个黑盒 Agent，而是在保留当前 `ModelGateway`、`messageType/talk` 路由、事实边界、工作台状态可视化和工具安全策略的前提下，逐步接入 Spring AI 的 Advisor、Tool Calling、ChatMemory、VectorStore、RAG 和 MCP 能力。

## 2. 当前现状

当前已经完成的能力：

- 后端已升级为 Spring Boot 4.1.x 和 Spring AI 2.0.x。
- 文本模型入口已改为 Spring AI `ChatClient`。
- 内部 `ResponsesApiChatModel` 负责把 Spring AI `Prompt` 转为 `/responses` 请求。
- 业务层继续通过 `ModelGateway` 调用模型，不直接依赖 HTTP、鉴权头或底层模型响应。
- 模型输出已经收敛为 `messageType`、`talk`、`conversation`、`post`、`warnings`。
- 前端聊天气泡只展示 `talk`，POST 后生成三版草稿并弹出选择框。
- 右侧 Agent 状态区已展示上下文预览、请求预览、响应预览、模型输出和事实边界检查。

当前还没有真正用上的 Spring AI Agent 能力：

- `Advisor` 链。
- Spring AI `Tool Calling`。
- Spring AI `ChatMemory`。
- Spring AI `VectorStore` 和 RAG Advisor。
- Spring AI MCP client/server。

当前已有但仍偏自研的 Agent 基础：

- `ToolRegistry + ToolAdapter + ToolCallPolicy + ToolCallRecorder` 已有雏形。
- `MemoryRecallService` 已有接口形态，但 embedding 仍是假实现。
- `AgentStateAssembler` 已经可以承载上下文、候选记忆、模型输出和事实边界，但当前 `pendingAssociations` 与 `candidateMemories` 仍为空。

## 3. 核心判断

Spring AI 应作为“模型交互层的 Agent 能力底座”，而不是替代业务主控的黑盒编排器。

当前项目最重要的资产不是“能生成文案”，而是用户能看见：

- 模型用了哪些当前会话内容。
- 哪些是用户确认事实。
- 哪些是模型推断。
- 哪些待用户确认。
- 哪些候选记忆只作为参考。
- 调用了哪些工具。
- 哪些工具需要用户确认。
- 为什么进入 POST，为什么继续 CONVERSATION。

因此，Spring AI 能力必须进入现有工作台状态、工具安全和事实边界体系，不能绕过这些业务约束。

## 4. Spring AI 能力映射

### 4.1 ChatClient

当前已经接入 `ChatClient`，这是模型调用入口。后续建议从 `ChatClient.create(chatModel)` 逐步演进到注入 Spring AI 自动配置的 `ChatClient.Builder` 或经过 `ChatClientBuilderConfigurer` 构造的 builder，以便保留 Spring AI 的观测、Advisor 自定义和后续扩展能力。

当前保留自定义 `ResponsesApiChatModel` 是合理的，因为内部 `gpt-5.5` 网关使用 `/responses`，而业务层仍需要走 Spring AI 抽象。

适用位置：

- `ModelGatewayConfiguration`
- `SpringAiModelGateway`
- `ResponsesApiChatModel`

参考：

- https://docs.spring.io/spring-ai/reference/api/chatclient.html

### 4.2 Advisor

Spring AI `Advisor` 适合承接模型调用前后的横切逻辑。当前项目最适合先接 Advisor，因为它不会立刻改变业务流程，却能把 Agent 能力标准化。

建议拆分的 Advisor：

- `FactBoundaryAdvisor`：注入事实边界规则，确保模型知道哪些内容可作为事实，哪些只能作为待确认项。
- `ContextPreviewAdvisor`：在调用前整理将发送上下文，并把请求预览写回 AgentState。
- `ModelTraceAdvisor`：记录模型调用开始、结束、耗时、模型名和响应摘要。
- `StructuredOutputAdvisor`：统一启用结构化输出约束，和现有 `OpenAiResponsesParser` 的业务校验配合。
- `SafetyAdvisor`：在模型请求前检查敏感信息、外部工具边界和高影响动作约束。

注意：Advisor 不应直接把未确认内容写入 `confirmedFacts`，也不应执行工具副作用。它只做模型交互增强、可视化记录和安全拦截。

适用位置：

- `SpringAiModelGateway`
- `AgentStateAssembler`
- `ModelContextPackageAssembler`
- `AgentTraceRecorder`

参考：

- https://docs.spring.io/spring-ai/reference/api/advisors.html

### 4.3 Tool Calling

Spring AI Tool Calling 可以让模型在允许工具集合中自主选择工具。当前项目已经有 `ToolRegistry`，很适合映射成 Spring AI `ToolCallback` 或 `@Tool`。

建议第一批工具：

- `flavor_suggestion`：风味联想，低风险，可自主调用。
- `memory_recall`：历史记录召回，低到中风险，可自主调用但结果必须标记为候选记忆。
- `draft_similarity_check`：文案相似度检测，低风险，可自主调用。
- `fact_boundary_review`：事实边界审稿，低风险，可自主调用。

暂不建议模型自主调用的工具：

- `xiaohongshu_publish`：高影响动作，必须用户确认。
- `image_generation`：可能产生成本或外部资源，建议用户确认。
- `archive_record`：写入长期记忆，必须用户确认最终文案和事实。
- `preference_update`：更新用户长期偏好，显式偏好可直接写，推断偏好必须确认。

接入原则：

- Spring AI 负责把工具 schema 暴露给模型。
- `ToolRegistry` 仍是唯一工具注册源。
- `ToolCallPolicy` 仍是唯一安全裁决点。
- `ToolCallRecorder` 仍记录入参摘要、结果摘要、确认状态和失败原因。
- 工具调用结果必须进入 `AgentTrace` 和右侧状态卡片。

适用位置：

- `ToolRegistry`
- `ToolAdapter`
- `ToolCallPolicy`
- `ToolCallRecorder`
- 新增 `SpringAiToolCallbackAdapter`

参考：

- https://docs.spring.io/spring-ai/reference/api/tools.html

### 4.4 ChatMemory

Spring AI `ChatMemory` 适合保存最近几轮对话窗口，让模型保留短期上下文。它不应替代完整会话存储，也不应替代长期咖啡记忆。

建议边界：

- `ChatMemory`：最近 N 轮用户和助手消息，用于模型上下文。
- `TastingSession`：当前会话业务状态，包括已确认事实、待确认问题、草稿和用户选择。
- PostgreSQL：完整历史会话、咖啡记录、最终文案和发布状态。
- pgvector：相似咖啡、相似文案和偏好摘要召回。

接入方式：

- 每个 `sessionId` 对应一个 conversation id。
- 只把聊天窗口作为模型上下文补充，不把它当事实源。
- 事实仍必须来自 `confirmedFacts` 或用户明确确认的当前消息。

参考：

- https://docs.spring.io/spring-ai/reference/api/chat-memory.html

### 4.5 VectorStore 与 RAG

当前 `memory-design-v0.1.md` 已确认 PostgreSQL + pgvector。Spring AI 可以承接 `EmbeddingModel`、`VectorStore` 和 RAG Advisor，让长期记忆召回进入模型上下文。

建议召回内容：

- 同一支咖啡或相似咖啡记录。
- 同产区、同处理法、同风味关键词记录。
- 用户近期满意的同风格文案。
- 用户明确不喜欢的表达。
- 已归档文案中的重复表达风险。

必须保留的业务边界：

- 召回结果进入 `candidateMemories`，默认 `PAGE_ONLY` 或 `SEND_AFTER_CONFIRMATION`。
- 长期记忆不得自动变成用户本次确认事实。
- RAG 引用必须带来源、相似原因和冲突状态。
- 召回失败时降级为无记忆创作，不阻断当前文案生成。

适用位置：

- `MemoryRecallService`
- `EmbeddingModelGateway`
- `MemoryEmbeddingJdbcRepository`
- `ModelContextPackageAssembler`
- `AgentStateAssembler`

参考：

- https://docs.spring.io/spring-ai/reference/api/vectordbs.html
- https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html

### 4.6 多角色 Agent

当前文档中的采访 Agent、感官 Agent、冲煮 Agent、文案 Agent、审稿 Agent、记忆 Agent 和发布 Agent，建议先实现为“角色化 ChatClient + Prompt + Advisor 组合”，不要过早拆成独立服务或复杂多 Agent 框架。

建议角色：

- `InterviewAgent`：判断缺失字段，生成追问。
- `SensoryAgent`：抽取风味、温度段、口感和香气。
- `BrewAgent`：分析冲煮参数和萃取复盘。
- `CopyAgent`：生成三版文案。
- `ReviewAgent`：检查虚构、事实边界、风格偏差和重复表达。
- `MemoryAgent`：决定是否召回、召回什么、如何标记候选记忆。
- `PublishAgent`：生成发布包，等待用户确认后调用发布工具。

这些角色可以共用底层 `ChatModel`，但使用不同的 system prompt、advisor 链、工具集合和输出 schema。

### 4.7 MCP

Spring AI MCP 适合后续把外部工具、资源和 prompt 暴露成标准协议。当前不建议立即引入。

适合未来 MCP 化的能力：

- 小红书搜索、详情、发布、互动技能。
- 图片生成工具。
- 外部知识库或文档工具。
- 动态工具发现。

引入前置条件：

- 本地 `ToolRegistry` 到 Spring AI Tool Calling 已跑通。
- 工具确认、工具轨迹和错误恢复已经稳定。
- 高影响动作确认机制已有自动化测试。

参考：

- https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html

## 5. 推荐演进路线

### 阶段一：Advisor 化当前模型链路

目标：不改变用户流程，把模型调用前后的上下文、事实边界和轨迹整理成 Spring AI Advisor 链。

任务：

- 新增 `FactBoundaryAdvisor`。
- 新增 `AgentTraceAdvisor`。
- 新增 `ContextPreviewAdvisor`。
- 调整 `ChatClient` 构造方式，尽量保留 Spring AI builder 的 observability 和 customizer 能力。
- 保持 `ModelGateway` 对业务层不变。

验收：

- POST 和 CONVERSATION 行为不变。
- 请求预览和响应预览仍脱敏。
- 事实边界检查仍能指出模型推断和待确认项。
- 前端工作台契约不变。

### 阶段二：ToolRegistry 映射 Spring AI Tool Calling

目标：让模型可以在低风险工具集合中自主调用工具，但所有工具仍经过本项目安全边界。

任务：

- 新增 `SpringAiToolCallbackAdapter`。
- 把 `ToolRegistry.ToolDefinition` 转为 Spring AI tool schema。
- 把 `ToolAdapter.execute()` 包装为 tool callback。
- `ToolCallPolicy` 在工具执行前强制校验确认状态。
- `ToolCallRecorder` 写入 AgentTrace。
- 先接入风味联想、文案相似度检查和事实边界审稿。

验收：

- 低风险工具可由模型自主调用。
- 高风险工具未确认时必须被阻断。
- 工具调用轨迹在右侧状态区可见。
- 工具失败时可降级，不丢失会话。

### 阶段三：Spring AI Embedding + pgvector RAG

目标：把真实长期记忆召回接入模型上下文。

任务：

- 替换假 `EmbeddingModelGateway` 为 Spring AI `EmbeddingModel`。
- 接入 Spring AI pgvector `VectorStore` 或保留 JDBC 查询但使用 Spring AI embedding。
- 实现 `MemoryRecallTool`。
- 将召回结果写入 `candidateMemories`。
- 在 `ContextPreview` 中展示召回来源、相似原因和发送状态。

验收：

- 有历史记录时能召回相似咖啡和相似文案。
- 召回内容不会自动变成已确认事实。
- 模型文案中使用记忆时必须标记来源或风险。
- 召回失败时仍能生成当前文案。

### 阶段四：ChatMemory 管短期对话窗口

目标：减少手工拼接短期对话上下文，让模型更自然地理解多轮对话。

任务：

- 为每个 `sessionId` 建立 Spring AI conversation id。
- 接入 `ChatMemory` 保存最近 N 轮消息。
- 保留 `TastingSession` 作为业务事实源。
- 明确 ChatMemory 内容只作为上下文，不直接作为事实。

验收：

- 多轮追问能自然接续。
- 刷新页面后工作台仍以后端快照为准。
- 事实抽取仍来自用户确认消息和结构化事实。

### 阶段五：多角色 Agent 编排

目标：把当前单次模型调用拆成可观察的逻辑角色链。

建议链路：

```text
用户消息
  -> InterviewAgent 判断是否缺字段
  -> MemoryAgent 召回候选记忆
  -> CopyAgent 生成 CONVERSATION 或 POST
  -> ReviewAgent 检查事实边界和风格偏差
  -> WorkbenchSnapshot 输出 talk、草稿、状态卡片和确认动作
```

验收：

- 每个角色都有 AgentTrace 步骤。
- 每个角色有独立 prompt 和输出 schema。
- ReviewAgent 可以阻断明显越界的 POST。
- 用户仍只看到清晰的 `talk`、草稿和确认入口。

### 阶段六：MCP 与动态工具发现

目标：在本地工具体系稳定后，引入 MCP 扩展外部工具能力。

任务：

- 评估小红书技能是否作为 MCP tool 暴露。
- 评估图片生成、文件资源和外部搜索是否 MCP 化。
- MCP 工具仍映射回 `ToolRegistry`，不绕过确认和审计。

验收：

- 动态工具可发现。
- 工具风险级别、确认要求和输出摘要可见。
- 高影响动作仍必须用户确认。

## 6. 不建议做的事

- 不建议把业务主流程完全交给模型自由规划。
- 不建议让模型直接写长期记忆。
- 不建议让模型直接执行小红书发布、评论、点赞、收藏。
- 不建议把 RAG 召回内容直接当作事实写入文案。
- 不建议为了使用 Spring AI 而移除 `ModelGateway`。
- 不建议一次性同时接入 Tool Calling、VectorStore、MCP 和多角色编排。

## 7. 对当前设计的评价

当前设计最值得保留的是：

- `ModelGateway` 作为业务模型边界。
- `messageType/talk` 作为前端展示契约。
- Prompt 资源化和版本化。
- 请求预览、响应预览和事实边界可视化。
- `ToolRegistry + ToolAdapter + ToolCallPolicy + ToolCallRecorder` 的安全工具边界。
- POST 不等于自动发布。
- 长期记忆和候选记忆不等于用户确认事实。

当前最应该补强的是：

- 把模型调用横切逻辑 Advisor 化。
- 把现有工具注册表映射到 Spring AI Tool Calling。
- 替换假 embedding，接入真实 EmbeddingModel 和 pgvector。
- 把候选记忆真正接入 `AgentStateAssembler`。
- 为每次模型调用和工具调用补完整 AgentTrace。
- 把多角色 Agent 做成可观察的逻辑角色，而不是提前拆成复杂服务。

## 8. 推荐下一切片

建议下一切片优先做：

**Spring AI Tool Calling 受控接入切片**

范围：

- 保留当前 `ModelGateway` 和 `ChatClient`。
- 新增 `SpringAiToolCallbackAdapter`。
- 将 `ToolRegistry` 中的低风险工具暴露给 Spring AI。
- 先实现一个真实可见的低风险工具，例如风味联想或文案相似度检测。
- 工具调用结果进入 AgentTrace 和右侧状态卡片。
- 高风险工具未确认时必须阻断。

为什么优先：

- 最符合“用上 Spring AI 强大 Agent 能力”的目标。
- 复用当前已有工具边界，不会推翻架构。
- 用户在工作台能明显看到 Agent 从“只生成”升级为“会选择工具、会记录过程、会受控行动”。
- 为后续 RAG、MCP、多角色 Agent 打基础。

## 9. 参考文档

- Spring AI ChatClient：https://docs.spring.io/spring-ai/reference/api/chatclient.html
- Spring AI Advisors：https://docs.spring.io/spring-ai/reference/api/advisors.html
- Spring AI Tool Calling：https://docs.spring.io/spring-ai/reference/api/tools.html
- Spring AI Chat Memory：https://docs.spring.io/spring-ai/reference/api/chat-memory.html
- Spring AI Vector Databases：https://docs.spring.io/spring-ai/reference/api/vectordbs.html
- Spring AI RAG：https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html
- Spring AI MCP：https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html
