# API 契约：咖啡品鉴创作 Agent MVP

## 通用约定

- Base path：`/api/v1`
- 请求和响应使用 JSON；文件上传使用 `multipart/form-data`。
- JSON REST 响应统一使用 envelope：`{ "requestId": "...", "data": {}, "error": null }`。
- 成功响应必须 `error=null`；失败响应必须 `data=null`。
- 错误对象格式：`{ "code": "...", "category": "...", "message": "...", "recoverable": true, "nextActions": [], "details": {} }`。
- `requestId` 便于关联前端请求、服务端日志、Agent 轨迹、工具调用和错误记录。
- 本契约描述 MVP 目标接口，具体字段可在实现时拆分 DTO，但不得破坏事实边界、确认和轨迹要求。
- 前端页面布局、视觉风格和关键交互必须遵循 [前端工作台设计 v0.1](../../../docs/architecture/frontend-design-v0.1.md)。接口响应应优先支持该设计中的三栏工作台、当前记录紧凑面板、Agent 轨迹卡片流、文案 Tabs 和发布包检查页。
- API 通信形态为 REST + SSE：REST 用于命令和查询，SSE 用于实时轨迹和进度推送。
- MVP 使用本地单用户模式，接口暂不要求 `Authorization` Header；后端统一将当前用户解析为 `local-user`。
- 响应体默认不需要暴露 `userId`，除非用于学习调试或管理视图；持久化、事件和记忆记录内部必须保留 `userId`。
- 业务 DTO 不重复携带顶层 `requestId`；需要关联 Agent 步骤时使用 `traceId`、`toolCallId` 或领域对象 id。

错误分类：

| category | 含义 | 示例 |
|---|---|---|
| `USER_FIXABLE` | 用户可处理 | 缺少字段、小红书未登录、需要验证码 |
| `RETRYABLE` | 可重试 | 模型超时、网络波动、Kafka 临时失败 |
| `DEGRADED` | 已降级 | 外部参考失败、Embedding 异步失败、SSE 断线 |
| `FATAL` | 致命错误 | 数据库不可用、关键配置缺失 |
| `SAFETY_BLOCKED` | 安全阻断 | 未确认发布、生图成本未确认、疑似泄露 Key |
 
错误响应示例：

```json
{
  "requestId": "uuid",
  "data": null,
  "error": {
    "code": "XHS_LOGIN_REQUIRED",
    "category": "USER_FIXABLE",
    "message": "小红书未登录，已保留发布包。",
    "recoverable": true,
    "nextActions": ["CHECK_LOGIN", "RETRY_FILL_PUBLISH", "SAVE_DRAFT"],
    "details": { "publishingPackageId": "uuid" }
  }
}
```

## 前端工作台契约

### 首页默认状态

打开工作台后默认进入“当前记录”页，而不是营销页或空白仪表盘。

首屏必须能由接口数据支持以下区域：

- 左侧导航：当前记录、历史记录、风味词库、用户偏好、发布记录、设置。
- 中间主工作区：`今天喝了什么咖啡？` 对话创作入口、Agent 模式切换、对话流、底部固定输入框。
- 当前记录紧凑面板：咖啡豆、冲煮、风味、待确认项、迷你感官雷达图入口。
- 右侧 Agent 轨迹栏：按类型着色的卡片流，每张卡片含摘要和年月日时分秒。

### 前端聚合视图

`GET /tasting-sessions/{sessionId}/workspace`

响应用于一次性渲染工作台当前状态：

```json
{
  "requestId": "uuid",
  "data": {
    "sessionId": "uuid",
    "heroQuestion": "今天喝了什么咖啡？",
    "orchestrationMode": "EXPLICIT_WORKFLOW",
    "availableOrchestrationModes": ["EXPLICIT_WORKFLOW", "MODEL_TOOL_CALLING"],
    "conversation": [],
    "recordPanel": {
      "status": "信息收集中",
      "beanFields": [],
      "brewFields": [],
      "flavorFields": [],
      "pendingConfirmations": [],
      "sensoryMiniChart": {}
    },
    "draftTabs": [],
    "agentTraceCards": []
  },
  "error": null
}
```

该接口可由后端聚合，也可由前端通过多个 API 组合实现；无论采用哪种方式，任务拆分时必须覆盖这些 UI 数据需求。

### 订阅工作台事件流

`GET /tasting-sessions/{sessionId}/events`

协议：`text/event-stream`

用途：前端右侧 Agent 轨迹栏实时追加卡片，展示模型步骤、工具调用、记忆召回、审稿判断和异步事件进度。

事件类型：

| SSE event | 说明 |
|---|---|
| `trace.step.created` | 新增 Agent 轨迹步骤 |
| `model.call.started` | 模型调用开始 |
| `model.call.completed` | 模型调用完成 |
| `tool.call.started` | 工具调用开始 |
| `tool.call.completed` | 工具调用完成 |
| `memory.recall.completed` | 记忆召回完成 |
| `outbox.event.published` | Outbox 事件已投递 Kafka |
| `async.task.completed` | 异步消费者任务完成 |
| `workflow.completed` | 本轮 Agent workflow 完成 |
| `workflow.failed` | 本轮 Agent workflow 失败 |
| `workflow.degraded` | 本轮流程已降级但可继续 |
| `error.occurred` | 发生分级错误 |

示例：

```text
event: trace.step.created
id: 01HV...
data: {"requestId":"uuid","traceId":"uuid","sequence":2,"stepType":"CONTEXT_BUILD","title":"上下文组装","summary":"已组装用户事实、候选风味和历史偏好"}
```

规则：

- SSE 事件不得包含完整 API Key、Authorization Header、Cookie、Session Token。
- 前端断线重连后必须先重新获取 workspace 快照，再继续订阅事件流。
- SSE 只推送状态，不替代发布、生图、互动等确认接口。
- SSE 不使用 REST envelope，但事件 `data` 应尽量携带 `requestId`、`traceId` 或业务 id。

错误事件示例：

```text
event: error.occurred
id: 01HV...
data: {"requestId":"uuid","code":"EXTERNAL_REFERENCE_UNAVAILABLE","category":"DEGRADED","summary":"外部参考不可用，已切换为无外部参考创作","recoverable":true}
```

### 切换 Agent 编排模式

`PATCH /tasting-sessions/{sessionId}/orchestration-mode`

请求：

```json
{ "mode": "MODEL_TOOL_CALLING" }
```

响应：

```json
{
  "requestId": "uuid",
  "data": {
    "sessionId": "uuid",
    "orchestrationMode": "MODEL_TOOL_CALLING",
    "appliesTo": "NEXT_TURN",
    "warning": "高影响工具仍需用户确认"
  },
  "error": null
}
```

规则：

- `EXPLICIT_WORKFLOW` 为默认模式。
- `MODEL_TOOL_CALLING` 只允许模型在后端注册工具集合中选择工具。
- 模式切换只影响后续对话轮次，不修改历史轨迹。
- 高影响工具确认规则不随模式变化。

## 会话与对话

### 创建品鉴会话

`POST /tasting-sessions`

请求：

```json
{ "mode": "CONVERSATION", "orchestrationMode": "EXPLICIT_WORKFLOW" }
```

响应：

```json
{
  "requestId": "uuid",
  "data": { "sessionId": "uuid", "status": "ACTIVE" },
  "error": null
}
```

### 提交对话消息

`POST /tasting-sessions/{sessionId}/messages`

请求：

```json
{ "content": "今天喝了一支水洗埃塞，有柑橘和红茶感" }
```

响应：

```json
{
  "requestId": "uuid",
  "data": {
    "assistantMessage": "还需要确认豆子信息、冲煮参数和你想要的文案风格。",
    "pendingQuestions": ["豆名或烘焙商是什么？", "水温和粉水比是多少？"],
    "traceId": "uuid"
  },
  "error": null
}
```

## 模板记录

### 保存模板字段

`PUT /tasting-sessions/{sessionId}/template`

请求包含咖啡豆、冲煮参数、感官评分、温度段风味和备注。字段允许部分提交。

响应返回当前已确认字段、待确认字段和校验提示。

### 上传豆袋图片解析

`POST /tasting-sessions/{sessionId}/bag-images`

请求：`multipart/form-data`，字段 `file`。

响应：

```json
{
  "requestId": "uuid",
  "data": {
    "assetId": "uuid",
    "extractedBeanFields": {
      "name": { "value": "候选豆名", "confirmationStatus": "PENDING_CONFIRMATION" }
    },
    "traceId": "uuid"
  },
  "error": null
}
```

## 风味联想

### 获取风味候选

`POST /tasting-sessions/{sessionId}/flavor-suggestions`

请求：

```json
{
  "inputTerm": "柑橘",
  "temperatureStage": "HOT",
  "senseType": "TASTE"
}
```

响应：

```json
{
  "requestId": "uuid",
  "data": {
    "suggestions": [
      {
        "id": "uuid",
        "name": "柠檬",
        "description": "明亮、尖锐、清爽、高酸",
        "temperatureStage": "HOT",
        "senseType": "TASTE",
        "polarity": "POSITIVE",
        "sensoryDimensions": ["ACIDITY", "AFTERTASTE"]
      }
    ],
    "traceId": "uuid"
  },
  "error": null
}
```

### 更新候选状态

`PATCH /flavor-suggestions/{suggestionId}`

请求：

```json
{ "status": "ACCEPTED", "editedName": null, "editedDescription": null }
```

## 文案与审稿

### 生成文案草稿

`POST /tasting-sessions/{sessionId}/drafts`

请求：

```json
{
  "styles": ["RESTRAINED", "EXAGGERATED", "SHARP_REVIEW"],
  "useMemory": true,
  "useExternalReferences": false
}
```

响应返回多个草稿、事实边界说明、审稿提示和 `traceId`。

### 记录文案满意度

`POST /drafts/{draftId}/feedback`

请求：

```json
{ "satisfactionScore": 8, "comment": "克制版更像我" }
```

## 记忆与归档

### 召回相似记录

`POST /tasting-sessions/{sessionId}/memory-recalls`

请求：

```json
{ "query": "水洗埃塞 柑橘 红茶", "limit": 3 }
```

响应返回相似记录摘要、相似原因、可能重复提示和 `traceId`。

### 归档品鉴记录

`POST /tasting-sessions/{sessionId}/archive`

请求：

```json
{ "finalDraftId": "uuid", "writeInferredPreferences": true }
```

响应：

```json
{
  "requestId": "uuid",
  "data": { "recordId": "uuid", "createdPreferenceIds": ["uuid"] },
  "error": null
}
```

## 外部参考与发布

### 检索外部参考

`POST /tasting-sessions/{sessionId}/external-references/search`

请求：

```json
{ "query": "烘焙商 豆名 柑橘 红茶", "limit": 5 }
```

响应最多返回 5 条小红书参考摘要，并标明 `sourcePlatform`。

### 创建发布包

`POST /tasting-sessions/{sessionId}/publishing-package`

请求：

```json
{ "draftId": "uuid", "imageAssetIds": ["uuid"] }
```

响应返回标题、正文、标签、图片、风险检查项和确认状态。

### 确认发布包并填写小红书发布页

`POST /publishing-packages/{packageId}/fill-xhs`

请求：

```json
{ "confirmed": true }
```

响应返回工具调用状态和下一步“发布页预览后二次确认”提示。

### 二次确认并公开发布

`POST /publishing-packages/{packageId}/publish-xhs`

请求：

```json
{ "confirmedAfterPreview": true }
```

响应返回发布结果；若登录、验证码、风控或页面失败，则返回失败原因并保留发布包。

## Agent 轨迹侧边栏

### 获取会话轨迹

`GET /tasting-sessions/{sessionId}/agent-traces`

响应：

```json
{
  "requestId": "uuid",
  "data": {
    "traces": [
      {
        "traceId": "uuid",
        "orchestrationMode": "EXPLICIT_WORKFLOW",
        "steps": [
          {
            "sequence": 1,
            "stepType": "MODEL_CALL",
            "title": "生成风味候选",
            "summary": "模型根据用户输入生成柑橘类候选",
            "promptSnapshot": { "messages": [] },
            "modelOutputSnapshot": { "content": "模型原始输出或结构化输出" },
            "decision": "等待用户选择，不写入事实",
            "toolSelectionReason": null
          }
        ]
      }
    ]
  },
  "error": null
}
```

## 图片生成

### 用户主动请求生图

`POST /tasting-sessions/{sessionId}/images/generate`

请求：

```json
{ "prompt": "根据甜橙、红茶和清透酸质画一张插画", "draftId": "uuid" }
```

响应返回图片候选资源和生成轨迹。未调用该接口时系统不得自动生图。

## 实现差异记录（2026-06-30）

- 已实现离线 Controller 形状和统一 `ApiResponse<T>` envelope，但当前不是实际 HTTP 路由。
- 已覆盖创建会话、提交消息、生成文案、模板保存、豆袋解析、风味候选、归档、记忆召回、Agent 轨迹、外部参考、发布包和生图的契约测试。
- SSE 当前通过 `AgentTraceSsePublisher` 的事件对象测试事件名和 data 形状，尚未接入真实 `text/event-stream`。
- 错误 envelope 与高影响动作阻断已在领域和工具测试中覆盖；真实 Spring 全局异常处理待接入 Web 运行时后验证。
