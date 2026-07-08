# 数据模型：OpenAI GPT-5.5 真实模型接入

## 设计原则

- 模型模式、请求预览、模型返回和事实边界必须成为工作台可见状态，而不是隐藏在日志里。
- 用户确认事实、模型推断、待确认联想、候选记忆和无依据表达必须分开建模。
- 凭证只存在于后端模型客户端运行时；所有可见模型请求、响应、错误和测试输出都必须脱敏。
- `offline-fake` 与 `openai-gpt55` 共用同一业务输出契约，便于降级、测试和对比。

## 实体

### ModelMode 模型模式

**含义**：当前生成链路的来源和能力状态。

**字段**：`mode`、`displayName`、`modelName`、`baseUrlLabel`、`available`、`requiresApiKey`、`statusLabel`、`fallbackAvailable`

**枚举**：

- `mode`：`offline-fake`、`openai-gpt55`
- `statusLabel`：`模拟输出，未调用真实模型`、`真实模型输出 / GPT-5.5`、`真实模型不可用，可切换离线模式`

**规则**：

- `offline-fake` 不要求 API Key。
- `openai-gpt55` 必须显示 `modelName=gpt-5.5`。
- `baseUrlLabel` 可以显示非敏感 base URL；不得显示 API Key 或鉴权 header。

### ModelContextPackage 模型上下文包

**含义**：一次生成前组织出的业务上下文，是真实请求 body 的来源。

**字段**：`sessionId`、`mode`、`currentSession`、`confirmedFacts`、`pendingAssociations`、`candidateMemoryBoundaries`、`excludedItems`、`promptConstraints`、`createdAt`

**关系**：

- 引用现有 `ContextItem`、`ConfirmedFact`、`PendingAssociation` 和 `CandidateMemory`。
- 生成一个 `ModelRequestPreview`。

**规则**：

- `confirmedFacts` 只能来自用户明确输入或确认。
- `pendingAssociations` 默认不得以事实身份发送；如果发送，只能作为“待确认联想/创作候选”。
- `candidateMemoryBoundaries` 必须说明候选来源和是否真实召回；本切片仍不接真实长期记忆数据库。
- `excludedItems` 必须保留排除原因，方便前端显示“不会发送”。

### ModelRequestPreview 真实请求预览

**含义**：前端可见的已脱敏请求结构，用于展示“已发送给大模型”。

**字段**：`label`、`modelName`、`mode`、`endpointPath`、`rawJson`、`redactionStatus`、`sentAt`

**规则**：

- `label` 使用中文，例如“已发送给大模型”。
- `endpointPath` 只允许展示 `/responses` 或非敏感路径，不展示鉴权信息。
- `rawJson` 是请求 body 的 JSON 风格字符串或对象，包含 `model: "gpt-5.5"` 和业务上下文。
- `rawJson` 不得包含 API Key、Authorization、Cookie、Session Token 或其他可复用凭证。

### ModelResponsePreview 模型返回预览

**含义**：前端可见的已脱敏模型返回摘要，用于展示“大模型返回”。

**字段**：`label`、`modelName`、`mode`、`rawJson`、`receivedAt`、`redactionStatus`

**规则**：

- `label` 使用中文，例如“大模型返回”。
- `rawJson` 可以包含模型输出的结构化业务字段和非敏感响应状态。
- 不展示 provider 内部鉴权信息、请求 header 或原始敏感错误体。

### ModelOutputSnapshot 模型输出快照

**含义**：工作台右侧模型输出区域展示的一次生成结果或失败状态。

**字段**：`outputType`、`mode`、`modelName`、`statusLabel`、`sourceBoundary`、`variants`、`requestPreview`、`responsePreview`、`recoverableError`、`generatedAt`

**枚举**：

- `outputType`：`SIMULATED`、`REAL_MODEL`、`ERROR`

**规则**：

- `SIMULATED` 必须显示“模拟输出，未调用真实模型”。
- `REAL_MODEL` 必须显示“真实模型输出 / GPT-5.5”。
- `ERROR` 必须保留 `recoverableError`，并不得清空用户输入和上下文预览。

### CopyVariant 文案版本

**含义**：模型生成的一版小红书文案。

**字段**：`style`、`styleLabel`、`title`、`body`、`tags`、`factUsages`、`inferences`、`pendingConfirmations`、`warnings`

**枚举**：

- `style`：`RESTRAINED`、`EXAGGERATED`、`SHARP_REVIEW`
- `styleLabel`：`克制版`、`夸张版`、`锐评版`

**规则**：

- 一次成功真实生成必须包含三种 `style`。
- `factUsages` 只能引用用户确认事实或明确用户陈述。
- `inferences` 必须标明是模型推断。
- `pendingConfirmations` 必须标明需用户确认后才能写成事实。

### FactUsage 事实使用记录

**含义**：文案中某个表达与事实来源的对应关系。

**字段**：`expression`、`basisType`、`sourceReference`、`sourceId`、`confidenceLabel`

**枚举**：

- `basisType`：`USER_CONFIRMED`、`MODEL_INFERENCE`、`PENDING_ASSOCIATION`、`CANDIDATE_MEMORY`、`UNSUPPORTED`、`CONFLICT`

**规则**：

- `USER_CONFIRMED` 必须能指向上下文或事实来源。
- `MODEL_INFERENCE` 和 `PENDING_ASSOCIATION` 不得被展示为用户确认事实。
- `UNSUPPORTED` 和 `CONFLICT` 必须进入事实边界检查结果。

### RecoverableModelError 可恢复模型错误

**含义**：真实模型失败后返回给前端的稳定错误状态。

**字段**：`code`、`category`、`message`、`recoverable`、`nextActions`、`preservedSessionId`、`retryableMode`、`createdAt`

**枚举**：

- `code`：`MODEL_TIMEOUT`、`MODEL_AUTH_FAILED`、`MODEL_RATE_LIMITED`、`MODEL_FORMAT_INVALID`、`MODEL_SERVICE_UNAVAILABLE`
- `nextActions`：`RETRY`、`CHECK_LOCAL_ENV`、`SWITCH_TO_OFFLINE_FAKE`、`TRY_LATER`

**规则**：

- `recoverable` 必须为 `true`。
- `message` 不得包含 API Key、Authorization、Cookie、Session Token 或 provider 原始敏感错误体。
- 失败后必须保留用户输入、当前会话、上下文预览和状态卡片。

### SensitiveRedactionResult 敏感信息过滤结果

**含义**：系统对可见请求、响应、错误和日志进行脱敏后的检查状态。

**字段**：`target`、`checkedPatterns`、`redacted`、`safeToDisplay`、`checkedAt`

**规则**：

- 检查范围至少覆盖 API Key、Authorization、Cookie、Session Token。
- `safeToDisplay=false` 时不得向前端返回对应原文。

### ModelGenerationState 模型生成状态

**含义**：一次工作台生成流程中的状态流转。

**状态流转**：

```text
IDLE -> CONTEXT_READY -> SENDING -> SUCCEEDED
IDLE -> CONTEXT_READY -> SENDING -> FAILED_RECOVERABLE -> RETRYING -> SUCCEEDED
IDLE -> CONTEXT_READY -> SENDING -> FAILED_RECOVERABLE -> OFFLINE_FALLBACK -> SUCCEEDED
```

**规则**：

- `SENDING` 期间用户输入和上下文预览不能丢失。
- `FAILED_RECOVERABLE` 必须保留重试或切换离线模式入口。
- `OFFLINE_FALLBACK` 输出必须重新标记为离线模拟，不得沿用真实模型状态。
