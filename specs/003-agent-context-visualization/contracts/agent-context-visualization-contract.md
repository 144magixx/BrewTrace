# 契约：Agent 上下文与记忆输入可视化

## 契约目标

本契约描述 003 中工作台页面与本地服务之间的用户可见状态边界。字段名称可在实现时微调，但不得破坏来源标签、发送状态、模拟边界、事实边界检查和清空当前会话语义。

## 工作台快照扩展

用途：打开页面、刷新页面、提交消息、清空会话后恢复完整工作台状态。

接口延续：

```text
GET /api/workbench/snapshot?sessionId={sessionId}
```

响应数据在 002 基础上必须新增或等价表达 `agentState`：

```json
{
  "requestId": "uuid",
  "data": {
    "sessionId": "uuid",
    "status": "WAITING_FOR_FACTS",
    "heroQuestion": "今天喝了什么咖啡？",
    "orchestrationMode": "EXPLICIT_WORKFLOW",
    "conversation": [],
    "recordSummary": {
      "confirmedFacts": [],
      "pendingQuestions": [],
      "suggestedFlavors": [],
      "draftStatus": "HIDDEN",
      "factBoundaryNotes": []
    },
    "draftTabs": [],
    "agentState": {
      "statusCards": [],
      "contextItems": [],
      "confirmedFacts": [],
      "pendingAssociations": [],
      "candidateMemories": [],
      "contextPreview": {
        "sections": [],
        "willSendCount": 0,
        "excludedCount": 0,
        "boundaryNote": "当前未调用真实模型。"
      },
      "modelOutput": null,
      "factBoundaryChecks": [],
      "capabilityBoundary": {
        "realModelConnected": false,
        "longTermMemoryConnected": false,
        "xiaohongshuConnected": false,
        "message": "当前仅展示本地可视化链路。"
      }
    },
    "lastError": null,
    "updatedAt": "2026-06-30T00:00:00Z"
  },
  "error": null
}
```

规则：

- `agentState` 必须随 `conversation`、`recordSummary` 和 `draftTabs` 一起更新。
- 空会话必须返回空状态，而不是隐藏整个状态卡片区。
- `capabilityBoundary` 必须明确真实模型、长期记忆和小红书未接入。
- 响应不得包含真实 API Key、Cookie、Authorization Header 或 Session Token。

## Agent 状态卡片

每张卡片必须至少表达：

```json
{
  "id": "card-1",
  "type": "CONFIRMED_FACT",
  "title": "已确认事实",
  "summary": "用户确认风味：柑橘",
  "sourceLabel": "来自用户消息",
  "sendStatus": "WILL_SEND",
  "riskLevel": "NONE",
  "createdAt": "2026-06-30T00:00:00Z"
}
```

允许的 `type`：

- `SESSION_CONTEXT`
- `CONFIRMED_FACT`
- `PENDING_ASSOCIATION`
- `CANDIDATE_MEMORY`
- `CONTEXT_PREVIEW`
- `MODEL_OUTPUT`
- `FACT_BOUNDARY_CHECK`
- `CAPABILITY_BOUNDARY`
- `SESSION_CONTROL`

允许的 `sendStatus`：

- `WILL_SEND`：未来模型输入会包含该项。
- `PAGE_ONLY`：只用于页面观察。
- `SEND_AFTER_CONFIRMATION`：用户确认后才可能发送。
- `EXCLUDED`：已排除，不会发送。

## 候选记忆

候选记忆必须包含来源边界：

```json
{
  "id": "memory-1",
  "title": "相似风味样例",
  "content": "曾经偏好干净的水洗埃塞表达。",
  "sourceBoundary": "本地示例，不是真实长期数据库召回",
  "reason": "与当前柑橘、红茶风味相似",
  "relationType": "SIMILAR_FLAVOR",
  "similarityLabel": "相似风味关键词",
  "conflictStatus": "NONE",
  "sendStatus": "PAGE_ONLY"
}
```

规则：

- 冲突候选默认 `sendStatus=EXCLUDED`。
- 不得把候选记忆写入 `recordSummary.confirmedFacts`。

## 上下文预览

上下文预览必须按来源分组：

```json
{
  "sections": [
    {
      "sectionType": "CONFIRMED_FACTS",
      "title": "已确认事实",
      "items": [
        {
          "content": "处理法：水洗",
          "sourceLabel": "来自用户消息",
          "sendStatus": "WILL_SEND",
          "exclusionReason": null
        }
      ]
    }
  ],
  "willSendCount": 1,
  "excludedCount": 0,
  "boundaryNote": "这是未来模型输入预览，当前未调用真实模型。"
}
```

规则：

- 待确认联想不得作为已确认事实出现在 `CONFIRMED_FACTS` 分组。
- 未发送项必须说明排除原因或待确认状态。

## 模拟模型输出与事实边界检查

模型输出必须明确标记为模拟：

```json
{
  "outputType": "SIMULATED",
  "content": "这杯水洗埃塞有柑橘和红茶感，可以联想到甜橙。",
  "sourceBoundary": "模拟输出，未调用真实模型",
  "generatedAt": "2026-06-30T00:00:00Z"
}
```

事实边界检查示例：

```json
{
  "id": "check-1",
  "expression": "甜橙爆汁感很明显",
  "basisType": "PENDING_ASSOCIATION",
  "riskLevel": "WARNING",
  "sourceReference": "由柑橘感扩展",
  "message": "用户只确认了柑橘感，甜橙爆汁感需要进一步确认。",
  "recommendedAction": "ASK_USER_CONFIRMATION"
}
```

规则：

- `UNSUPPORTED` 和 `CONFLICT` 必须给出原因。
- 高风险表达在用户确认前不得进入最终记录。

## 清空当前会话

用途：用户主动结束当前会话边界，避免刷新恢复旧草稿和旧上下文。

推荐接口：

```text
POST /api/workbench/sessions/{sessionId}/clear
```

请求：

```json
{
  "confirmed": true
}
```

响应：

```json
{
  "requestId": "uuid",
  "data": {
    "sessionId": null,
    "status": "EMPTY",
    "heroQuestion": "今天喝了什么咖啡？",
    "conversation": [],
    "recordSummary": {
      "confirmedFacts": [],
      "pendingQuestions": [],
      "suggestedFlavors": [],
      "draftStatus": "HIDDEN",
      "factBoundaryNotes": []
    },
    "draftTabs": [],
    "agentState": {
      "statusCards": [],
      "contextItems": [],
      "confirmedFacts": [],
      "pendingAssociations": [],
      "candidateMemories": [],
      "contextPreview": {
        "sections": [],
        "willSendCount": 0,
        "excludedCount": 0,
        "boundaryNote": "当前没有可发送上下文。"
      },
      "modelOutput": null,
      "factBoundaryChecks": []
    },
    "lastError": null
  },
  "error": null
}
```

前端规则：

- 当前会话有可见内容时，清空前必须显示确认提示。
- 用户取消后不得调用清空接口，也不得修改当前快照。
- 用户确认后必须清除浏览器本地恢复状态中的旧 `lastSessionId`。
- 清空后刷新不得恢复旧会话。

## 前端 UI 契约

页面必须包含：

- 左侧导航栏。
- 中间对话工作区，只包含标题、消息流、输入框和必要错误提示。
- 右侧当前记录与 Agent 状态区。
- 当前记录下方的 Agent 状态卡片区。
- “新建记录 / 清空当前会话”入口。

页面不得：

- 在中间对话区展示候选记忆卡片、上下文预览卡片或事实边界检查卡片。
- 暗示模拟输出来自真实模型。
- 暗示候选记忆来自真实长期数据库。
- 暗示清空当前会话会删除长期记忆、历史归档或外部平台数据。

## 不在本切片内的契约

- 真实模型调用接口。
- 长期记忆数据库召回接口。
- 小红书搜索、发布、点赞、评论、收藏接口。
- 图片生成接口。
- 多用户权限和远程部署接口。
