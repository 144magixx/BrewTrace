# 咖啡品鉴创作 Agent：项目当前上下文

> **文档定位**：本文件是仓库当前技术状态、运行方式、能力边界和文档导航的持续维护入口。
>
> **最近核对日期**：2026-07-11
>
> **维护方式**：本文不使用版本号。每完成一个经过验证的垂直切片，直接更新本文；功能级设计仍保留在对应的 `specs/` 目录中。

## 1. 如何使用本文

进入仓库后，先阅读本文了解当前事实，再根据任务类型进入具体文档：

- 需要了解协作、启动和代码约束：阅读仓库根目录的 [AGENTS.md](../../AGENTS.md)。
- 需要了解产品目标：阅读 [咖啡品鉴创作 Agent PRD](../prd/coffee-note-agent-prd-v0.1.md) 和 [MVP 范围](../prd/mvp-scope-v0.1.md)。
- 需要修改某个已经规格化的功能：再阅读对应的 `specs/{编号}-{功能}/spec.md`、`plan.md` 和 `tasks.md`。
- 需要理解一次已完成改造：阅读 `docs/review/`；`docs/code-review/` 保存早期评审材料。
- 需要启动或验收本地服务：本文提供总规则，IDE 细节见 [IDEA 本地运行与验收指南](./idea-local-run-guide.md)。

本文取代“在 `AGENTS.md` 中永久指向某个功能 plan”的做法。任何单个 `specs/*/plan.md` 都只描述对应功能切片，不代表整个项目的长期现状。

## 2. 当前产品目标与边界

当前产品是一套本地运行的咖啡品鉴创作 Agent 工作台。用户通过对话记录真实咖啡体验，Agent 负责补充追问、调用低风险风味联想工具、维护事实边界，并在信息足够时生成多风格草稿。

必须持续保护以下边界：

- 用户明确表达或确认的内容，才可以进入已确认事实。
- 模型推断和工具返回的风味只能作为待确认联想，不能伪装成真实品鉴记录。
- `POST` 表示生成可审阅草稿，不等于发布到小红书。
- 发布、评论、点赞、收藏和其他外部高影响动作必须经过用户明确确认。
- 模型请求、工具调用、事实状态和错误恢复必须在工作台中可追踪。

## 3. 当前实现状态

### 3.1 已接通并有自动化验证的主链路

| 能力 | 当前实现 | 主要入口 |
|---|---|---|
| 本地 Web 工作台 | React 工作台通过 `/api/workbench` 与 Spring Boot 通信，支持创建会话、提交消息、SSE 提交和清空会话 | `frontend/src/app/App.tsx`、`WorkbenchController` |
| 模型消息路由 | 模型返回结构化 `CONVERSATION` 或 `POST`，聊天区统一展示 `talk`；`POST` 才进入草稿链路 | `ModelAgentMessage`、`OpenAiResponsesParser`、`WebWorkbenchService` |
| 多轮对话上下文 | 请求中携带当前会话消息，不只发送用户本轮回答；模型的历史追问和回答都属于会话上下文 | `ModelContextPackageAssembler`、`OpenAiResponsesRequestFactory` |
| Agent 状态可视化 | 工作台展示当前上下文、事实边界、待确认联想、模型请求、模型响应和能力边界 | `AgentStateAssembler`、`frontend/src/features/agent-trace/` |
| Spring AI Advisor 链 | 事实边界、请求预览和调用轨迹通过 Advisor 横切模型调用 | `FactBoundaryAdvisor`、`ContextPreviewAdvisor`、`AgentTraceAdvisor` |
| 风味联想工具 | `flavor_suggestion` 已注册为 Spring AI `ToolCallback`；模型可自主调用，结果固定为 `PENDING_ASSOCIATION` | `ToolConfiguration`、`FlavorSuggestionToolAdapter` |
| 工具调用循环 | 支持模型发起 `function_call`、后端执行工具、把 `function_call_output` 送回模型，最多三轮 | `SpringAiModelGateway`、`ResponsesApiChatModel` |
| 实际请求预览 | 工作台展示真正交给 HTTP Client 的请求体，只做敏感值脱敏，不重新序列化；工具 `description` 与最终请求保持一致 | `ActualModelRequestCapture`、`ContextPreviewAdvisor` |
| 事实状态增量 | 主模型可返回带证据的 `factUpdates`；后端负责确定性证据校验、状态流转和审计记录 | `FactUpdateValidator`、`FactUpdateApplier`、`TastingSession` |
| Prompt 与 Schema 资源化 | Agent 行为、工具定义、结构化输出和测试 JSON fixture 均由资源文件加载 | `backend/src/main/resources/prompts/` |

截至 2026-07-11，后端使用 Java 25、按 Java 21 目标编译时，全量测试结果为 51 项通过、0 失败、0 错误。测试数量会随功能增加而变化，不能作为固定门槛；每次交付仍需重新运行测试。

### 3.2 已有代码骨架但尚未成为真实外部能力

| 能力 | 当前状态 | 不得误认为 |
|---|---|---|
| PostgreSQL、pgvector、JPA | 已有领域对象、Repository 形状和迁移脚本，但当前 Maven 运行依赖未接入真实数据库 | 已完成持久化或长期记忆 |
| 长期记忆与 RAG | 已有 memory 包和向量相关骨架，工作台候选记忆仍为空或内存态 | 已接通真实召回、Embedding 或 VectorStore |
| Agent Trace 与前端展示 | 当前 Trace 可在内存中记录模型与工具步骤，但 Repository 仍是应用内实现，前端完整 Trace SSE 与逐步详情尚未接入 | 已具备跨重启审计、完整工具轨迹页面或可观测平台 |
| 小红书工具 | 存在 `XiaohongshuToolAdapter` 和工具定义骨架，但没有注册到当前模型 Tool Calling 列表，也未调用真实小红书能力 | 已登录、已搜索或已发布 |
| 图片生成 | 存在 `ImageGenerationToolAdapter` 和资产领域对象骨架，当前没有真实图像模型调用 | 已生成真实图片文件 |
| 发布工作流 | 前端和领域层有发布审阅、确认相关组件与对象，但当前工作台主链路不执行真实平台发布 | `POST` 已公开发布 |
| Redis、Kafka、Outbox | 存在接口、适配器或迁移设计骨架，当前本地工作台主流程不依赖真实基础设施 | 已完成可靠异步事件链路 |

### 3.3 当前唯一注册给主模型的工具

当前 `ToolConfiguration` 只把 `flavor_suggestion` 注册为主模型可调用的 `ToolCallback`。工具说明和输入 Schema 位于：

- `backend/src/main/resources/prompts/tools/flavor-suggestion/definition-v1.json`

小红书、图片生成、归档、记忆写入和偏好更新均未开放给主模型自主调用。增加新工具时必须同时完成：真实适配器、风险级别、确认策略、输入输出资源、调用记录、自动测试和工作台可见状态。

## 4. 当前技术栈

### 4.1 后端

- Java 编译目标：21。
- 当前本机已验证运行时：Homebrew OpenJDK 25。
- Spring Boot：4.1.0。
- Spring AI：2.0.0。
- 构建工具：Maven Wrapper。
- 模型协议：OpenAI-compatible Responses API，调用 `/responses`。
- 当前真实 HTTP Controller：`/api/workbench` 工作台接口。

### 4.2 前端

- React：18.3.x。
- TypeScript：5.7.x。
- Vite：6.0.x。
- Vitest：2.1.x。
- 本地地址：`http://127.0.0.1:5173`。
- `/api` 由 Vite 代理到 `http://127.0.0.1:8080`。

### 4.3 模型配置事实来源

模型名和代理地址不得从本文、旧 plan、旧 README 或上一次启动参数中推断。每次启动后端前必须读取：

1. `$CODEX_HOME/config.toml`；
2. `CODEX_HOME` 未设置时读取 `~/.codex/config.toml`。

配置映射规则：

- 顶层 `model` → `TEXT_MODEL`。
- 当前 `model_provider` 对应 Provider 的 `base_url` → `OPENAI_BASE_URL`。
- `requires_openai_auth` 决定是否需要本地鉴权环境。

任何真实凭证都不得输出到终端记录、请求预览、文档或 Git。`application.yml` 中的模型名和地址只属于框架默认值，不能覆盖启动前的 `config.toml` 校验规则。

## 5. 当前核心运行链路

一次用户消息的主链路如下：

1. 前端调用 `POST /api/workbench/sessions/{sessionId}/messages` 或 SSE 版本。
2. `WebWorkbenchService` 保存本轮用户消息并组装用户可见状态。
3. `ModelContextPackageAssembler` 汇总会话历史、已确认事实、待确认联想、候选记忆和事实边界约束。
4. `SpringAiModelGateway` 创建 Prompt，并绑定 Advisor 和允许调用的工具。
5. `ResponsesApiChatModel` 生成最终 Responses API body，在 HTTP 调用前捕获同一份实际请求体。
6. 模型如返回 `flavor_suggestion` 的 `function_call`，后端执行工具并把结果送回模型继续推理。
7. `OpenAiResponsesParser` 解析最终 `CONVERSATION` 或 `POST`，同时读取 `factUpdates`。
8. 后端对事实增量执行证据、边界和状态流转校验；校验失败时返回可恢复错误，不静默写入事实。
9. 工作台返回完整快照，展示聊天内容、草稿、Agent 状态及实际模型请求/响应预览；工具调用会写入内存 Trace，但完整逐步工具轨迹仍待接入前端。

## 6. 项目结构

| 路径 | 当前职责 |
|---|---|
| `AGENTS.md` | 仓库协作、启动和质量约束 |
| `AI_Coding_行为准则.md` | AI Coding 工作原则 |
| `backend/` | Spring Boot + Spring AI 后端 |
| `backend/src/main/java/.../agent/` | 模型契约、网关、Advisor 和 Prompt 组装 |
| `backend/src/main/java/.../workbench/` | 当前真实 Web 工作台主链路 |
| `backend/src/main/java/.../tools/` | 工具注册、策略、适配和记录 |
| `backend/src/main/java/.../flavor/` | 风味联想领域与模型生成器 |
| `backend/src/main/java/.../tasting/` | 会话、事实状态和咖啡记录领域 |
| `backend/src/main/resources/prompts/` | Prompt、Schema 和工具定义 |
| `frontend/` | React 工作台 |
| `docs/` | 当前架构、评审、研究和学习资料 |
| `specs/` | 按功能切片保存的 Spec Kit 产物 |

详细包结构应以当前源码为准。表格只用于导航，不是生成新模块的授权。

## 7. 构建、启动与验收

### 7.1 后端 Java 预检

当前 Shell 的 `JAVA_HOME` 可能仍指向 Java 8。Maven 命令前必须确认 `./mvnw -v` 中的 Java 版本不低于 21。Homebrew OpenJDK 可使用：

```bash
cd backend
export JAVA_HOME="$(brew --prefix openjdk)/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -v
./mvnw test
```

如果 Maven 使用 Java 8，`record` 和 switch 表达式会产生大量伪装成源码损坏的语法错误。此时先修正 JDK，不得修改业务代码来迎合错误运行时。

### 7.2 后端启动

启动前按 [AGENTS.md](../../AGENTS.md) 读取 `config.toml`，把非敏感模型配置映射到环境变量后运行：

```bash
cd backend
./mvnw spring-boot:run
```

不要在文档或脚本示例中写入真实 API Key。

### 7.3 前端命令

```bash
cd frontend
npm install
npm test
npm run build
npm run dev
```

### 7.4 启动后验收

```bash
curl http://127.0.0.1:8080/api/workbench/snapshot
```

必须核对：

- `modelMode.modelName` 与 `config.toml` 顶层 `model` 一致。
- `modelMode.baseUrlLabel` 与当前 Provider 的 `base_url` 一致。
- 页面提交一轮真实消息后，请求预览可以解析，并包含实际发送的会话历史和已注册工具定义。
- 提到模糊风味词时，模型具备调用 `flavor_suggestion` 的能力；工具结果仍显示为待确认联想。
- 不把后端已经记录工具步骤误写为前端已经具备完整 Trace 展示；当前需要结合请求预览、模型结果和后端记录进行验证。
- 未发生小红书发布、图片生成或长期记忆写入等未接通动作。

## 8. 文档地图

| 需要了解的内容 | 首选文档 | 说明 |
|---|---|---|
| 当前项目全貌 | 本文 | 持续更新，不绑定单个功能编号 |
| 协作与工程约束 | [AGENTS.md](../../AGENTS.md) | 启动、分支、注释、Prompt、测试、CR 规则 |
| 项目治理原则 | [constitution.md](../../.specify/memory/constitution.md) | 真实体验、状态追踪、工具安全、垂直切片 |
| 产品目标和 MVP | [coffee-note-agent-prd-v0.1.md](../prd/coffee-note-agent-prd-v0.1.md) | 产品范围与用户场景 |
| 总体 Agent 架构 | [agent-architecture-v0.1.md](./agent-architecture-v0.1.md) | 初始总体设计，其中部分未来能力尚未落地 |
| 当前 HTTP 契约 | [api-interface-list-v0.1.md](./api-interface-list-v0.1.md) | 接口形状；修改 Controller 后需同步核对 |
| 本地运行 | [idea-local-run-guide.md](./idea-local-run-guide.md) | IDEA 和浏览器验收细节 |
| 功能规格 | `specs/*/` | 只在处理对应功能时阅读，不作为永久总入口 |
| 已完成改造评审 | `docs/review/` | 新 CR 的标准目录 |
| 早期代码评审 | `docs/code-review/` | 历史目录，只保留追溯价值 |
| 外部调研 | `docs/research/` | 风味、小红书和样本研究 |

## 9. 当前优先级

基于现有主链路，下一阶段更有价值的工作顺序是：

1. 完成事实状态链路的产品验收，确保确认、拒绝、修正、撤回都能在工作台中解释和追踪。
2. 扩充工具前先盘点假实现，明确每个工具的真实适配器、风险和确认策略；优先低风险、无副作用工具。
3. 把工具调用轨迹完整展示到工作台，支持逐轮查看模型请求、工具入参、工具结果和后续模型请求。
4. 接入真实持久化，再实现长期记忆、Embedding 和 pgvector 召回；不能把内存态包装成长记忆。
5. 在发布确认状态机和真实平台适配器完成前，不开放小红书高影响工具。

这些优先级是当前工程判断，不是不可变路线图。进入具体实现前，仍应创建或更新对应功能规格。

## 10. 文档维护规则

- 任何影响技术栈、目录结构、构建或启动命令、配置事实来源、实现状态、假实现边界、核心链路、外部集成、测试基线或当前优先级的变更，必须在同一任务中同步更新本文；不得延后为无明确负责人的文档待办。
- 跨模块改造完成并验证后，必须更新本文的“当前实现状态”“当前核心运行链路”和“当前优先级”。
- 新增功能级 spec 时，不修改 `AGENTS.md` 的总入口；只在本文档地图或相关章节增加链接。
- 功能 plan 与当前代码冲突时，先核对代码、测试和运行态，再更新本文；不得为了匹配旧 plan 回退已验证实现。
- 模型名、代理地址和鉴权要求始终以启动时的 `config.toml` 为准，本文只记录读取规则。
- 自动测试数量、当前模型名称和本机 JDK 路径属于易变化信息，更新时必须标注核对日期。
- 已确认事实、工程判断和待确认假设必须明确区分；不能把规划中的数据库、记忆、发布或工具能力写成已完成事实。
