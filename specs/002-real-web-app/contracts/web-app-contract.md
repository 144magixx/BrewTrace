# 契约：真实 Web 应用接入

## 契约目标

本契约描述真实 Web 工作台与本地服务之间的用户可见交互边界。字段名称可在实现时微调，但不得破坏事实边界、错误恢复和三版草稿展示要求。

## 工作台首页快照

用途：打开页面或刷新页面后恢复当前工作台状态。

最终接口：

```text
GET /api/workbench/snapshot?sessionId={sessionId}
```

响应数据必须支持以下内容：

- 当前是否已有会话。
- 首页问题：“今天喝了什么咖啡？”
- 当前对话消息列表。
- 当前记录摘要：已确认事实、待确认联想、待回答问题、草稿状态。
- 草稿标签页。
- 最近错误提示。

成功示例：

```json
{
  "requestId": "uuid",
  "data": {
    "sessionId": "uuid",
    "status": "WAITING_FOR_FACTS",
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
    "lastError": null
  },
  "error": null
}
```

## 创建会话

用户打开当前记录页后，可以创建新的品鉴会话。

最终接口：

```text
POST /api/workbench/sessions
```

请求数据：

```json
{
  "mode": "EXPLICIT_WORKFLOW"
}
```

响应要求：

- 返回新会话标识。
- 状态为可接收用户输入。
- 不要求登录。

## 提交用户消息

用户在输入框提交自然语言咖啡体验。

最终接口：

```text
POST /api/workbench/sessions/{sessionId}/messages
```

请求数据：

```json
{
  "content": "今天喝了一支水洗埃塞，有柑橘和红茶感"
}
```

信息不足时响应必须包含：

- 助手追问。
- 待回答问题列表。
- 当前已确认事实。
- 不返回最终草稿。

补充足够事实后响应必须包含：

- 助手确认说明。
- 三类草稿。
- 每个草稿的事实边界说明。
- 当前记录摘要。

## 错误响应

所有用户可见错误必须包含：

```json
{
  "requestId": "uuid",
  "data": null,
  "error": {
    "code": "SERVICE_UNAVAILABLE",
    "category": "RETRYABLE",
    "message": "本地服务暂时不可用，已保留你的输入。",
    "recoverable": true,
    "nextActions": ["CHECK_LOCAL_SERVICE", "RETRY"],
    "details": {
      "preservedInput": "用户刚刚输入的内容"
    }
  }
}
```

规则：

- 页面必须展示 `message` 和 `nextActions`。
- 页面必须保留 `preservedInput`。
- 错误响应不得包含真实 API Key、Cookie、Authorization Header 或 Session Token。

当前已实现错误码：

- `EMPTY_MESSAGE`：空输入，需要用户继续输入。
- `MESSAGE_TOO_SHORT`：输入过短，需要补充咖啡事实或感受。
- `SESSION_NOT_FOUND`：后端内存会话不存在，需要重新创建会话。
- `SERVICE_UNAVAILABLE`：前端无法连接本地后端服务，保留用户输入并提示检查服务。
- `INTERNAL_ERROR`：未分类本地服务错误，作为可重试错误展示。

## 前端 UI 契约

首页必须包含：

- 左侧导航栏，当前记录处于选中状态。
- 中间对话创作区，包含模式显示、消息流和底部输入框。
- 当前记录摘要面板，展示已确认事实、待确认联想、待回答问题和草稿状态。
- 右侧轨迹栏预留区域，可以先显示基础流程状态，不强制展示完整 AgentTrace。

## 不在本切片内的契约

- 长期记忆召回接口。
- 模板记录完整编辑接口。
- 小红书发布、点赞、评论、收藏接口。
- 图片生成接口。
- 真实模型配置和密钥管理接口。
