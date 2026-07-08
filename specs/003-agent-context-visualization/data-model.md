# 数据模型：Agent 上下文与记忆输入可视化

## 设计原则

- 本切片的数据模型服务于可视化和验收，不代表真实长期记忆、真实模型调用或小红书外部动作已经接入。
- 所有会影响模型输入预览或模型输出审查的内容都必须带来源、确认状态和发送状态。
- 清空当前会话只重置当前工作台会话和浏览器恢复状态，不删除长期记录、历史归档或外部平台数据。

## 实体

### WorkbenchSnapshot 工作台快照

**含义**：浏览器打开、刷新、提交消息或清空会话后获得的完整页面状态。

**字段**：`sessionId`、`status`、`heroQuestion`、`orchestrationMode`、`conversation`、`recordSummary`、`draftTabs`、`agentState`、`lastError`、`updatedAt`

**关系**：

- 包含一个 `RecordSummary`。
- 包含零到多条 `WebConversationMessage`。
- 包含零到多条 `DraftTab`。
- 包含一个 `AgentStateSnapshot`。

**规则**：

- `agentState` 必须与当前 `sessionId` 和页面可见会话一致。
- `status=EMPTY` 时，对话、草稿、模型输出和上下文预览必须为空或显示空状态。
- 任何错误状态都不得删除用户正在输入的草稿文本。

### AgentStateSnapshot Agent 状态卡片区快照

**含义**：当前记录下方状态卡片区展示的完整 Agent 状态。

**字段**：`statusCards`、`contextItems`、`confirmedFacts`、`pendingAssociations`、`candidateMemories`、`contextPreview`、`modelOutput`、`factBoundaryChecks`、`capabilityBoundary`、`updatedAt`

**关系**：

- 聚合多个状态分区和卡片。
- 引用会话上下文项、候选记忆和事实边界检查结果。

**规则**：

- 每个状态分区都必须有空状态文案。
- 所有候选记忆和模拟输出必须显示来源边界。
- 状态卡片不得出现在中间对话区。

### AgentStatusCard Agent 状态卡片

**含义**：右侧状态区中一张可扫描的彩色卡片。

**字段**：`id`、`type`、`title`、`summary`、`sourceLabel`、`sendStatus`、`riskLevel`、`createdAt`

**枚举**：

- `type`：`SESSION_CONTEXT`、`CONFIRMED_FACT`、`PENDING_ASSOCIATION`、`CANDIDATE_MEMORY`、`CONTEXT_PREVIEW`、`MODEL_OUTPUT`、`FACT_BOUNDARY_CHECK`、`CAPABILITY_BOUNDARY`、`SESSION_CONTROL`
- `sendStatus`：`WILL_SEND`、`PAGE_ONLY`、`SEND_AFTER_CONFIRMATION`、`EXCLUDED`
- `riskLevel`：`NONE`、`INFO`、`WARNING`、`HIGH`

**规则**：

- 卡片必须显示来源和发送状态。
- 高风险事实混淆必须在卡片上可见。
- 卡片颜色只辅助识别，不能替代文字标签。

### ContextItem 会话上下文项

**含义**：当前会话中可能影响模型输入的用户输入、助手回复、选择或状态变化。

**字段**：`id`、`role`、`content`、`sourceType`、`confirmationStatus`、`sendStatus`、`sourceMessageId`、`createdAt`

**规则**：

- 用户明确表达可以作为用户陈述事实，但仍需区分是否完整确认。
- 助手追问和系统联想不得覆盖用户原话。
- 未确认联想默认不得以确认事实身份进入模型输入预览。

### ConfirmedFact 已确认事实

**含义**：用户明确提供或确认的咖啡事实。

**字段**：`id`、`factType`、`value`、`sourceContextItemId`、`confirmationStatus`、`sendStatus`

**规则**：

- `confirmationStatus` 必须为用户确认或用户陈述。
- 可以进入模型输入预览，但仍必须保留来源指向。

### PendingAssociation 待确认联想

**含义**：系统基于当前事实扩展出的风味、表达或创作方向。

**字段**：`id`、`value`、`triggerFactId`、`reason`、`confirmationStatus`、`sendStatus`

**规则**：

- 默认 `sendStatus=SEND_AFTER_CONFIRMATION` 或 `PAGE_ONLY`。
- 不得列入已确认事实。
- 如果进入模拟输出，事实边界检查必须标为待确认表达。

### CandidateMemory 候选记忆

**含义**：可能与当前会话相关的历史偏好、样例记录或测试数据。

**字段**：`id`、`title`、`content`、`sourceBoundary`、`reason`、`relationType`、`similarityLabel`、`conflictStatus`、`sendStatus`

**规则**：

- 第一版必须标明不是长期数据库真实召回。
- 与当前确认事实冲突时默认 `sendStatus=EXCLUDED`。
- 不得被展示为用户当前会话事实。

### ContextPreview 上下文预览

**含义**：未来会发送给大模型的结构化内容预览。

**字段**：`sections`、`willSendCount`、`excludedCount`、`boundaryNote`

**关系**：

- `sections` 由多个 `ContextPreviewSection` 组成。

**规则**：

- 必须按来源分组。
- 必须明确当前不会调用真实模型。
- 待确认联想不得以确认事实文案出现。

### ContextPreviewSection 上下文预览分组

**字段**：`sectionType`、`title`、`items`

**规则**：

- `items` 必须包含发送状态和排除原因。
- 分组至少覆盖当前会话、已确认事实、待确认联想和候选记忆。

### ModelOutputSnapshot 模型输出快照

**含义**：当前切片展示的模拟输出或固定样例。

**字段**：`outputType`、`content`、`sourceBoundary`、`generatedAt`

**规则**：

- 必须明确标记为模拟输出或固定样例。
- 不得暗示真实模型已经被调用。

### FactBoundaryCheckResult 事实边界检查结果

**含义**：对模拟输出或候选表达的逐项检查。

**字段**：`id`、`expression`、`basisType`、`riskLevel`、`sourceReference`、`message`、`recommendedAction`

**枚举**：

- `basisType`：`USER_CONFIRMED`、`CANDIDATE_MEMORY`、`PENDING_ASSOCIATION`、`UNSUPPORTED`、`CONFLICT`
- `recommendedAction`：`KEEP`、`ASK_USER_CONFIRMATION`、`EXCLUDE_FROM_FINAL_RECORD`、`REWRITE`

**规则**：

- 无依据和冲突表达必须有可理解原因。
- 高风险表达在确认前不得进入最终记录。

### SessionControlAction 会话控制动作

**含义**：用户主动新建记录、清空当前会话或取消清空的操作。

**字段**：`actionType`、`confirmationRequired`、`impactSummary`、`confirmed`、`resultStatus`

**状态流转**：

```text
IDLE -> CONFIRMING_CLEAR -> CLEARED
IDLE -> CONFIRMING_CLEAR -> CANCELLED
CLEARED -> EMPTY
```

**规则**：

- 当前会话有可见内容时，清空前必须要求确认。
- 取消后不得改变当前会话状态。
- 清空后刷新不得恢复旧会话。

### LocalResumeState 本地恢复状态

**含义**：浏览器侧用于刷新恢复的轻量状态。

**字段**：`lastSessionId`、`draftInput`、`lastKnownStatus`、`clearedSessionIds`、`savedAt`

**规则**：

- 清空当前会话后，必须移除或排除被清空的 `lastSessionId`。
- 只用于本地单用户便利性，不能被视为长期咖啡记录。
