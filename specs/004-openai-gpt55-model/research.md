# 调研：OpenAI GPT-5.5 真实模型接入

## 调研来源与边界

- 项目内依据：`specs/004-openai-gpt55-model/spec.md`、`specs/003-agent-context-visualization/*`、`.specify/memory/constitution.md`、现有 `ModelGateway`、`AgentStateAssembler`、`ContextPreviewPanel`、`ModelOutputPanel`。
- 本地环境依据：`~/.config/xhs-coffee-agent/env` 中存在 `OPENAI_BASE_URL=https://saturday.sankuai.com/v1`、`TEXT_MODEL=gpt-5.5` 和已打码的 `OPENAI_API_KEY`。
- 官方文档依据：[OpenAI Responses API create reference](https://platform.openai.com/docs/api-reference/responses/create) 说明创建响应使用 `POST /responses`，请求包含 `model`、`input` 等字段；[OpenAI Structured Outputs guide](https://platform.openai.com/docs/guides/structured-outputs) 说明可用 JSON schema 约束模型输出。
- 安全边界：本调研不记录、不复制、不展示任何真实 API Key、Authorization、Cookie 或 Session Token。

## Decision: 保留 `ModelGateway` 作为业务边界，新增 OpenAI-compatible `LlmClient`

**Rationale**：现有后端已经有 `agent/application/ModelGateway`，且宪法要求外部能力通过清晰适配器调用。继续使用该边界可以让 `WebWorkbenchService` 和 `AgentStateAssembler` 面向业务对象：模型上下文包、三版文案、事实边界结果，而不依赖 OpenAI SDK、HTTP、鉴权头或代理 base URL。底层新增 `OpenAiResponsesLlmClient` 或等价类，负责 Responses API 请求、超时、错误映射、脱敏和格式校验。

**Alternatives considered**：

- 在 `WebWorkbenchService` 中直接发 HTTP：实现最快，但会把凭证、协议、错误分类和业务状态混在一起，不符合可替换模型网关边界。
- 引入完整 Agent 框架：功能过重，超出本切片“真实模型 vertical slice”的范围。
- 直接复用占位 `SpringAiModelGateway` 名称：可作为迁移入口，但当前项目未引入 Spring AI 依赖；计划阶段不要求新增 Spring AI，避免名称和实际能力不一致。

## Decision: 使用 OpenAI-compatible Responses API，base URL 为 `https://saturday.sankuai.com/v1`

**Rationale**：用户明确指定请求地址和模型名，且本地 env 中非敏感配置与之匹配。计划采用 `POST {OPENAI_BASE_URL}/responses`，请求体使用 `model: "gpt-5.5"`，并把用户会话、确认事实、待确认联想和候选记忆边界组织到 `instructions` 与 `input` 中。真实请求预览展示脱敏后的业务 JSON，不展示 HTTP Header。

**Alternatives considered**：

- 使用官方默认 `https://api.openai.com/v1`：与用户指定环境不一致。
- 使用 Chat Completions：不符合用户“使用 Responses API”的明确要求。
- 前端直接调用模型服务：会暴露密钥和 base URL 访问细节，不允许。

## Decision: 三版文案使用结构化输出契约，并对格式异常做可恢复错误

**Rationale**：规格要求一次返回克制版、夸张版、锐评版。真实模型自然语言可能漏版、混版或把事实边界写丢，因此模型请求应要求返回结构化 JSON：`variants[]` 中每项包含 `style`、`title`、`body`、`tags`、`factUsages`、`inferences`、`pendingConfirmations`。后端仍必须校验 style 是否齐全、事实边界字段是否存在；失败时返回 `MODEL_FORMAT_INVALID` 可恢复错误，并保留当前会话。

**Alternatives considered**：

- 让模型返回 Markdown 后前端切分：实现脆弱，漏版难以检测。
- 只展示模型原文：无法满足事实边界检查和三版文案契约。
- 格式异常时降级为离线输出：可能掩盖真实模型失败，计划改为保留状态并提示用户重试或切换离线模式。

## Decision: 密钥只从环境读取，`~/.config/xhs-coffee-agent/env` 只作为本地注入来源

**Rationale**：用户允许访问本地 env 文件，但项目约束要求 API Key 不写入配置文件、日志、测试快照或前端。实现阶段应让运行命令通过 shell 加载该文件，将 `OPENAI_API_KEY`、`OPENAI_BASE_URL`、`TEXT_MODEL` 注入进程环境；应用配置只读取环境变量，不把密钥持久化。文档可记录变量名和非敏感默认值，不记录真实 Key。

**Alternatives considered**：

- 把 Key 写入 `application.yml`：违反宪法和规格。
- 前端保存 Key：直接暴露凭证，不允许。
- 测试使用真实 Key：不可复现且有泄露风险；真实调用可作为人工 quickstart，自动测试使用 fake/stub。

## Decision: 上下文预览分为“将发送 / 不会发送 / 已发送给大模型 / 大模型返回”

**Rationale**：003 已有 `ContextPreview`，但真实模型接入后需要新增真实请求和响应的可见标签。计划保留原有来源分组，再增加脱敏 `requestPreview` 与 `responsePreview`：`requestPreview.rawJson` 展示将发送的业务 JSON；`responsePreview.rawJson` 展示已脱敏的模型返回摘要。标签使用中文，避免用户把待确认联想误读为已确认事实。

**Alternatives considered**：

- 只展示将发送内容，不展示实际发送：无法验证真实请求。
- 展示完整 HTTP 请求：会提高凭证泄露风险；计划只展示脱敏 body，不展示鉴权 header。
- 把发送和返回放在同一文本框：不利于追踪和验收。

## Decision: 错误分为超时、鉴权失败、限流、格式异常和服务不可用

**Rationale**：规格明确要求超时、鉴权失败、限流和返回格式异常可恢复。后端模型客户端应把底层错误映射为稳定错误码：`MODEL_TIMEOUT`、`MODEL_AUTH_FAILED`、`MODEL_RATE_LIMITED`、`MODEL_FORMAT_INVALID`、`MODEL_SERVICE_UNAVAILABLE`。所有错误都必须 `recoverable=true`，带建议动作如 `RETRY`、`CHECK_LOCAL_ENV`、`SWITCH_TO_OFFLINE_FAKE`，并保留当前工作台快照。

**Alternatives considered**：

- 统一成“生成失败”：用户无法判断该重试、改 Key 还是切离线。
- 把底层响应原样展示：可能泄露敏感信息或服务内部细节。
- 失败后清空输出状态：会丢失用户输入和上下文，不符合验收。

## Decision: 本切片不做流式输出、成本统计、真实长期记忆或小红书发布

**Rationale**：用户明确排除真实小红书发布、真实长期记忆数据库、外部工具执行或自动发布动作。为保持可验证垂直切片，真实模型调用先采用一次性响应，右侧状态区展示最终请求/响应和错误状态。

**Alternatives considered**：

- 同步引入流式输出：会增加前后端状态复杂度，且不是验收必要条件。
- 将候选记忆接真实数据库：会混淆本次目标和 003 的候选边界。
- 生成后自动发布小红书：高影响外部动作，必须在后续独立规格中处理确认和工具安全。
