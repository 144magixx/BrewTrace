# 数据模型：真实 Web 应用接入

## 设计原则

- 本切片的数据模型是页面状态与现有业务内核之间的桥梁，不新增长期事实存储。
- 所有会进入文案或页面记录摘要的内容必须带来源边界：用户确认事实、系统联想、待确认候选或错误状态。
- 页面刷新恢复只用于本地单用户体验，不等同于长期持久化。

## 实体

### WebWorkbenchSession 工作台会话

**含义**：用户在浏览器中进行的一次当前记录流程。

**字段**：`sessionId`、`status`、`heroQuestion`、`orchestrationMode`、`conversation`、`recordSummary`、`draftTabs`、`lastError`、`updatedAt`

**状态**：

```text
EMPTY -> SESSION_CREATED -> WAITING_FOR_FACTS -> DRAFTS_READY
EMPTY -> ERROR_RECOVERABLE
SESSION_CREATED -> ERROR_RECOVERABLE
WAITING_FOR_FACTS -> ERROR_RECOVERABLE
```

**规则**：

- 首页默认应能从 `EMPTY` 创建新会话。
- `WAITING_FOR_FACTS` 时不得展示最终草稿。
- `DRAFTS_READY` 时必须至少包含三类草稿。
- `ERROR_RECOVERABLE` 时必须保留用户输入或恢复提示。

### WebConversationMessage 对话消息

**含义**：页面消息流中的用户消息或助手消息。

**字段**：`id`、`role`、`content`、`sourceType`、`createdAt`

**规则**：

- 用户输入保存为用户确认表达，但其中缺失字段仍需要追问。
- 助手追问和解释属于系统建议，不得覆盖用户原话。

### RecordSummary 当前记录摘要

**含义**：页面右侧当前记录面板展示的结构化摘要。

**字段**：`confirmedFacts`、`pendingQuestions`、`suggestedFlavors`、`draftStatus`、`factBoundaryNotes`

**规则**：

- `confirmedFacts` 只展示用户明确提供或确认的内容。
- `suggestedFlavors` 必须标记为待确认联想。
- `factBoundaryNotes` 必须在生成草稿后可见。

### DraftTab 文案草稿标签页

**含义**：三类文案草稿的页面展示模型。

**字段**：`draftId`、`style`、`title`、`body`、`tags`、`factBoundaryNotes`、`reviewWarnings`

**状态**：

```text
HIDDEN -> GENERATED -> VISIBLE
```

**规则**：

- `style` 必须覆盖克制版、夸张版、锐评版。
- 每个草稿必须展示事实边界说明。
- 草稿正文不得把待确认候选写成用户事实。

### RecoverableError 用户可恢复错误

**含义**：页面展示给用户的错误状态。

**字段**：`code`、`category`、`message`、`recoverable`、`nextActions`、`preservedInput`

**规则**：

- 服务不可用或请求失败时，必须保留 `preservedInput`。
- 错误提示必须说明用户下一步可做什么。
- 不得展示凭证、Cookie 或完整敏感配置。

### LocalResumeState 本地恢复状态

**含义**：浏览器侧用于刷新恢复的轻量状态。

**字段**：`lastSessionId`、`draftInput`、`lastKnownStatus`、`savedAt`

**规则**：

- 只用于本地单用户便利性。
- 不能被视为长期咖啡记录。
- 如果恢复失败，页面必须提示重新创建会话。
