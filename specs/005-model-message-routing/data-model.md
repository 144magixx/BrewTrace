# 数据模型：模型驱动消息路由

## 设计原则

- 模型返回必须先表达业务意图，再承载具体内容。
- `talk` 是唯一用于聊天气泡展示的模型话术字段。
- POST 和 CONVERSATION 是两套消息模型，不用大量可空字段混在一个对象里。
- 模型决策可见、可审计、可测试；后端必须校验模型结构后再路由。
- 未确认事实不能因进入 POST 文案而变成用户确认事实。

## 实体

### ModelAgentMessage 模型消息

**含义**：一次真实模型返回后，系统用于驱动工作台下一步行为的顶层消息。

**字段**：`messageType`、`talk`、`post`、`conversation`、`warnings`、`generatedAt`

**枚举**：

- `messageType`：`CONVERSATION`、`POST`

**关系**：

- `messageType=CONVERSATION` 时必须关联 `ConversationModelMessage`，`post` 为空。
- `messageType=POST` 时必须关联 `PostModelMessage`，`conversation` 为空或仅保留非展示型诊断摘要。
- 生成 `ModelOutputSnapshot`，并影响工作台状态和草稿展示。

**验证规则**：

- `messageType` 必须合法。
- `talk` 必须非空，且适合直接展示给用户。
- 不允许同一次成功消息同时进入 CONVERSATION 和 POST 两条业务链路。
- 结构不合格时转为可恢复模型格式错误。

### ConversationModelMessage 继续对话消息

**含义**：模型认为当前上下文不完备或仍需用户确认时返回的消息。

**字段**：`questions`、`pendingConfirmations`、`warnings`

**规则**：

- 必须通过顶层 `talk` 向用户提出 1-3 个清晰问题或边界提醒。
- 不得生成可发布文案草稿。
- `pendingConfirmations` 用于标记模型希望用户确认的信息，不得写成事实。
- 工作台状态保持等待用户补充。

### PostModelMessage 发布草稿消息

**含义**：模型认为当前上下文足够进入文案生成，或用户明确要求不再补充时返回的消息。

**字段**：`variants`、`warnings`

**规则**：

- `variants` 必须刚好包含三版：`RESTRAINED`、`EXAGGERATED`、`SHARP_REVIEW`。
- 三版不得重复或缺失。
- 每版必须保留事实使用、模型推断、待确认项和风险提醒。
- POST 只表示进入草稿和发布前确认，不表示自动公开发布。

### CopyVariant 文案变体

**含义**：POST 消息中的单个小红书文案版本。

**字段**：`style`、`styleLabel`、`title`、`body`、`tags`、`factUsages`、`inferences`、`pendingConfirmations`、`warnings`

**枚举**：

- `style`：`RESTRAINED`、`EXAGGERATED`、`SHARP_REVIEW`

**规则**：

- `title` 和 `body` 必须非空。
- `factUsages` 只能引用用户确认事实或明确用户陈述。
- `inferences` 只能表达模型推断，不能进入用户确认事实。
- `pendingConfirmations` 必须说明仍需用户确认后才能作为事实使用。
- `warnings` 标记信息不足、事实风险或非咖啡输入等问题。

### FactUsage 事实边界记录

**含义**：模型输出中的某个表达与来源边界的对应关系。

**字段**：`expression`、`basisType`、`sourceReference`、`sourceId`、`confidenceLabel`

**枚举**：

- `basisType`：`USER_CONFIRMED`、`CONFIRMED_FACT`、`MODEL_INFERENCE`、`PENDING_ASSOCIATION`、`CANDIDATE_MEMORY`、`UNSUPPORTED`、`CONFLICT`
- `confidenceLabel`：`HIGH`、`MEDIUM`、`LOW`

**规则**：

- `USER_CONFIRMED` 与 `CONFIRMED_FACT` 必须能追溯到当前上下文或已确认事实。
- `MODEL_INFERENCE`、`PENDING_ASSOCIATION` 和 `CANDIDATE_MEMORY` 不得被展示为用户确认事实。
- `UNSUPPORTED` 和 `CONFLICT` 必须进入事实边界检查。

### ModelOutputSnapshot 模型输出快照

**含义**：工作台可见的一次模型输出状态。

**字段**：`outputType`、`messageType`、`talk`、`mode`、`modelName`、`statusLabel`、`sourceBoundary`、`post`、`conversation`、`variants`、`requestPreview`、`responsePreview`、`recoverableError`、`generatedAt`

**规则**：

- `talk` 来源于 `ModelAgentMessage.talk`，前端聊天气泡只展示该字段。
- `messageType=CONVERSATION` 时 `variants` 必须为空，不生成 `DraftTab`。
- `messageType=POST` 时 `variants` 与 `post.variants` 保持一致，并可映射为草稿区展示。
- `recoverableError` 存在时不得清空用户输入和上下文预览。

### DraftTab 草稿页签

**含义**：前端草稿区域展示的一版可编辑或可选择文案。

**字段**：`draftId`、`style`、`title`、`body`、`tags`、`factBoundaryNotes`、`reviewWarnings`

**规则**：

- 仅由 POST 消息生成。
- CONVERSATION 消息不得生成草稿页签。
- 发布相关按钮只能基于草稿页签和发布前确认展示。

### PromptBundle 提示词组合包

**含义**：一次模型调用前由后端组合出的提示词集合。

**字段**：`baseTemplateVersion`、`routingRulesVersion`、`stylePromptVersions`、`fieldDefinitions`、`dynamicConstraints`、`createdAt`

**规则**：

- 基础模板、路由规则和风格提示词必须可追踪版本。
- 当前会话、已确认事实、待确认联想、候选记忆边界和排除项作为动态输入组合。
- 不允许把成段提示词硬编码在运行代码或测试代码中。

## 状态流转

```text
EMPTY
  -> SESSION_CREATED
  -> WAITING_FOR_FACTS
  -> CONVERSATION_RETURNED
  -> WAITING_FOR_FACTS
  -> POST_RETURNED
  -> DRAFTS_READY
  -> PUBLISH_CONFIRMATION_REQUIRED
```

错误流转：

```text
WAITING_FOR_FACTS or DRAFTS_READY
  -> MODEL_FORMAT_INVALID
  -> ERROR_RECOVERABLE
  -> RETRYING
```

## 校验矩阵

| 场景 | 必须校验 | 失败结果 |
|---|---|---|
| 任意模型消息 | `messageType` 合法、`talk` 非空 | 可恢复格式错误 |
| CONVERSATION | 不生成草稿、追问清晰、待确认项不进事实 | 保持等待用户补充并提示风险 |
| POST | 三版风格完整、事实边界字段存在、标题正文非空 | 阻断草稿链路 |
| 非咖啡输入 | 不伪造咖啡事实 | 返回边界提醒或低信息量 POST 风险 |
| 模板缺失 | 不能降级到硬编码提示词 | 可恢复配置错误 |
