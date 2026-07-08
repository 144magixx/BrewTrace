# 契约：模型驱动消息路由

## 目标

本文档定义模型返回、工作台快照和前端展示之间的业务契约。契约重点是：模型用结构化消息驱动后端路由，前端聊天框只展示 `talk`，POST 才进入草稿链路，CONVERSATION 只继续追问。

## 模型结构化返回契约

### CONVERSATION

```json
{
  "messageType": "CONVERSATION",
  "talk": "我还需要确认两点：这杯你喝到的主要风味是什么？是否知道产区或处理法？",
  "conversation": {
    "questions": [
      "这杯你喝到的主要风味是什么？",
      "是否知道产区或处理法？"
    ],
    "pendingConfirmations": [
      {
        "expression": "主要风味仍需用户补充",
        "basisType": "PENDING_ASSOCIATION",
        "sourceReference": "model.routing",
        "sourceId": "",
        "confidenceLabel": "LOW"
      }
    ],
    "warnings": [
      "当前信息不足，不能生成真实咖啡品鉴文案。"
    ]
  },
  "post": null,
  "warnings": []
}
```

**契约规则**：

- `messageType` 必须为 `CONVERSATION`。
- `talk` 必须非空，并适合直接展示给用户。
- `post` 必须为空。
- 系统不得从 CONVERSATION 结果生成 `draftTabs`。

### POST

```json
{
  "messageType": "POST",
  "talk": "信息已经够了，我先整理成三版文案，你可以选一版再继续改。",
  "conversation": null,
  "post": {
    "variants": [
      {
        "style": "RESTRAINED",
        "styleLabel": "克制版",
        "title": "橙色茶感的水洗埃塞",
        "body": "这杯水洗埃塞喝起来更偏橙柑和红茶感，整体干净，温度下来后茶感更明显。",
        "tags": ["手冲咖啡", "咖啡品鉴"],
        "factUsages": [
          {
            "expression": "水洗埃塞、橙柑和红茶感",
            "basisType": "USER_CONFIRMED",
            "sourceReference": "currentSession[0].content",
            "sourceId": "context-1",
            "confidenceLabel": "HIGH"
          }
        ],
        "inferences": [],
        "pendingConfirmations": [],
        "warnings": []
      },
      {
        "style": "EXAGGERATED",
        "styleLabel": "夸张版",
        "title": "一杯橙色茶汤炸开了",
        "body": "这支水洗埃塞像把橙柑和红茶一起推到杯口，香气不靠蛮力，干净度撑住了画面感。",
        "tags": ["手冲咖啡", "咖啡豆分享"],
        "factUsages": [],
        "inferences": [],
        "pendingConfirmations": [],
        "warnings": []
      },
      {
        "style": "SHARP_REVIEW",
        "styleLabel": "锐评版",
        "title": "这杯水洗埃塞至少没乱装",
        "body": "先说结论：这杯靠干净度和茶感站住，不需要硬凹热带水果大戏。",
        "tags": ["咖啡锐评", "手冲咖啡"],
        "factUsages": [],
        "inferences": [],
        "pendingConfirmations": [],
        "warnings": []
      }
    ],
    "warnings": []
  },
  "warnings": []
}
```

**契约规则**：

- `messageType` 必须为 `POST`。
- `talk` 必须非空，聊天框只展示 `talk`。
- `post.variants` 必须刚好包含 `RESTRAINED`、`EXAGGERATED`、`SHARP_REVIEW`。
- POST 结果只能进入草稿和发布前确认，不能自动公开发布。

## 工作台快照契约

### CONVERSATION 后的快照要求

```json
{
  "status": "WAITING_FOR_FACTS",
  "conversation": [
    { "role": "USER", "content": "今天喝了一杯咖啡" }
  ],
  "draftTabs": [],
  "agentState": {
    "modelOutput": {
      "messageType": "CONVERSATION",
      "talk": "我还需要确认两点：这杯你喝到的主要风味是什么？是否知道产区或处理法？",
      "conversation": {
        "questions": []
      },
      "post": null,
      "variants": []
    }
  }
}
```

**前端要求**：

- 聊天气泡展示 `agentState.modelOutput.talk`。
- 草稿区为空。
- 不展示一键发送或发布确认入口。

### POST 后的快照要求

```json
{
  "status": "DRAFTS_READY",
  "draftTabs": [
    { "style": "RESTRAINED", "title": "...", "body": "..." },
    { "style": "EXAGGERATED", "title": "...", "body": "..." },
    { "style": "SHARP_REVIEW", "title": "...", "body": "..." }
  ],
  "agentState": {
    "modelOutput": {
      "messageType": "POST",
      "talk": "信息已经够了，我先整理成三版文案，你可以选一版再继续改。",
      "conversation": null,
      "post": {
        "variants": []
      },
      "variants": []
    }
  }
}
```

**前端要求**：

- 聊天气泡只展示 `talk`。
- 草稿区展示三版文案。
- 发布相关入口只在发布前确认流程中出现。

## 错误契约

### 缺少 `talk`

```json
{
  "recoverableError": {
    "code": "MODEL_FORMAT_INVALID",
    "message": "模型返回格式异常，请重试。",
    "recoverable": true,
    "nextActions": ["RETRY"]
  }
}
```

### POST 三版不完整

```json
{
  "recoverableError": {
    "code": "MODEL_FORMAT_INVALID",
    "message": "模型返回格式异常，请重试。",
    "recoverable": true,
    "nextActions": ["RETRY"]
  }
}
```

## 提示词契约

- 系统提示词必须说明输入字段含义、事实边界、路由规则、POST 输出要求、CONVERSATION 输出要求和 `talk` 展示规则。
- 风格提示词内容必须被实际组合进模型输入，不能只提供文件路径。
- 请求预览可以展示脱敏后的业务 JSON 和模板版本信息，不得展示密钥、鉴权 header、Cookie 或 Session Token。
