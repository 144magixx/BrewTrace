# 契约：OpenAI GPT-5.5 真实模型接入

## 契约目标

本契约描述 004 中工作台页面、后端工作台服务和模型网关之间的用户可见边界。字段名称可在实现时微调，但不得破坏以下语义：

- `offline-fake` 与 `openai-gpt55` 模式必须可区分。
- 真实模型请求和返回必须使用中文标签分开展示。
- 前端可见请求 JSON 必须脱敏，不包含任何凭证。
- 三版文案必须结构化返回：克制版、夸张版、锐评版。
- 失败必须可恢复，并保留当前会话状态。

## 工作台快照扩展

接口延续：

```text
GET /api/workbench/snapshot?sessionId={sessionId}
POST /api/workbench/sessions/{sessionId}/messages
```

响应中的 `agentState.modelOutput` 从 003 的模拟输出扩展为可同时承载离线输出、真实模型输出和可恢复错误：

```json
{
  "agentState": {
    "contextPreview": {
      "sections": [],
      "willSendCount": 3,
      "excludedCount": 2,
      "boundaryNote": "真实模型模式开启，以下内容将组织为 GPT-5.5 请求。"
    },
    "modelMode": {
      "mode": "openai-gpt55",
      "displayName": "真实模型输出 / GPT-5.5",
      "modelName": "gpt-5.5",
      "available": true,
      "requiresApiKey": true,
      "fallbackAvailable": true
    },
    "modelOutput": {
      "outputType": "REAL_MODEL",
      "mode": "openai-gpt55",
      "modelName": "gpt-5.5",
      "statusLabel": "真实模型输出 / GPT-5.5",
      "sourceBoundary": "由真实模型生成，事实边界仍需检查。",
      "variants": [],
      "requestPreview": null,
      "responsePreview": null,
      "recoverableError": null,
      "generatedAt": "2026-06-30T00:00:00Z"
    },
    "factBoundaryChecks": [],
    "capabilityBoundary": {
      "realModelConnected": true,
      "longTermMemoryConnected": false,
      "xiaohongshuConnected": false,
      "message": "已启用真实文本模型；仍未执行小红书动作，未接真实长期记忆数据库。"
    }
  }
}
```

规则：

- `modelMode.mode=offline-fake` 时，`modelOutput.outputType` 必须为 `SIMULATED` 或空状态，并显示“模拟输出，未调用真实模型”。
- `modelMode.mode=openai-gpt55` 且成功生成时，`modelOutput.outputType=REAL_MODEL`，`modelName=gpt-5.5`。
- `capabilityBoundary.xiaohongshuConnected` 和 `longTermMemoryConnected` 在本切片必须保持 `false`。
- 响应不得包含 API Key、Authorization、Cookie、Session Token。

## 模型模式选择

推荐在提交消息或触发生成时允许传入模式；若实现阶段选择全局本地模式，也必须在快照中返回实际模式。

```json
{
  "content": "今天喝了一支水洗埃塞，酸质像柑橘，尾段有红茶感。",
  "modelMode": "openai-gpt55"
}
```

规则：

- 省略 `modelMode` 时默认使用后端配置或 `offline-fake`。
- 当 `openai-gpt55` 没有可用 Key 时，返回可恢复错误或保持离线模式，不得静默伪装为真实模型输出。

## Responses API 请求体

真实模型客户端向 OpenAI-compatible 服务发送：

```text
POST {OPENAI_BASE_URL}/responses
```

其中本地验收的非敏感配置为：

```text
OPENAI_BASE_URL=https://saturday.sankuai.com/v1
TEXT_MODEL=gpt-5.5
```

脱敏后的请求 body 语义：

```json
{
  "model": "gpt-5.5",
  "instructions": "从 backend/src/main/resources/prompts/agent/openai-responses-copy-v1.md 动态加载，并替换本次动态约束占位符后的完整提示词。",
  "input": [
    {
      "role": "user",
      "content": [
        {
          "type": "input_text",
          "text": "请基于以下咖啡记录生成三版小红书文案..."
        }
      ]
    }
  ],
  "text": {
    "format": {
      "type": "json_schema",
      "name": "coffee_copy_variants",
      "strict": true,
      "schema": {
        "type": "object",
        "required": ["variants"],
        "properties": {
          "variants": {
            "type": "array",
            "minItems": 3,
            "maxItems": 3
          }
        }
      }
    }
  }
}
```

规则：

- 真实 HTTP 请求必须包含鉴权 header，但该 header 永远不得进入 `requestPreview`、日志或错误详情。
- `requestPreview.rawJson` 可以展示以上 body 的业务结构，但不得展示 `OPENAI_API_KEY` 的值。
- 如果目标服务不完全支持 `text.format`，实现阶段可以在模型提示和后端校验中保留同等结构化约束，但格式异常必须返回 `MODEL_FORMAT_INVALID`。

## 前端请求预览

上下文预览新增两个中文标签区域：

```json
{
  "requestPreview": {
    "label": "已发送给大模型",
    "modelName": "gpt-5.5",
    "mode": "openai-gpt55",
    "endpointPath": "/responses",
    "rawJson": "{\n  \"model\": \"gpt-5.5\"\n}",
    "redactionStatus": "SAFE_TO_DISPLAY",
    "sentAt": "2026-06-30T00:00:00Z"
  },
  "responsePreview": {
    "label": "大模型返回",
    "modelName": "gpt-5.5",
    "mode": "openai-gpt55",
    "rawJson": "{\n  \"variants\": []\n}",
    "redactionStatus": "SAFE_TO_DISPLAY",
    "receivedAt": "2026-06-30T00:00:10Z"
  }
}
```

前端展示规则：

- `rawJson` 使用类似 Markdown 的 JSON 代码框展示。
- “将发送”“不会发送”“待确认后发送”“已发送给大模型”“大模型返回”必须是中文标签。
- 发送内容和返回内容不得混在同一个文本区域。

## 三版文案契约

成功结果必须包含：

```json
{
  "variants": [
    {
      "style": "RESTRAINED",
      "styleLabel": "克制版",
      "title": "清亮水洗埃塞的一天",
      "body": "正文...",
      "tags": ["咖啡", "手冲"],
      "factUsages": [
        {
          "expression": "水洗埃塞",
          "basisType": "USER_CONFIRMED",
          "sourceReference": "用户输入"
        }
      ],
      "inferences": [
        {
          "expression": "清爽明亮",
          "basisType": "MODEL_INFERENCE",
          "sourceReference": "由柑橘感推断"
        }
      ],
      "pendingConfirmations": [
        {
          "expression": "甜橙",
          "basisType": "PENDING_ASSOCIATION",
          "sourceReference": "由柑橘联想，需确认"
        }
      ],
      "warnings": []
    }
  ]
}
```

规则：

- 必须恰好覆盖 `RESTRAINED`、`EXAGGERATED`、`SHARP_REVIEW`。
- `styleLabel` 必须分别展示为 `克制版`、`夸张版`、`锐评版`。
- `factUsages` 不得引用未确认联想作为用户事实。
- `pendingConfirmations` 必须在事实边界检查中可见。

## 可恢复错误契约

当真实模型失败时，工作台响应保持 `ApiResponse` 结构，错误稳定分类：

```json
{
  "code": "MODEL_AUTH_FAILED",
  "category": "RETRYABLE",
  "message": "真实模型鉴权失败，请检查本地 OPENAI_API_KEY，或切换 offline-fake 模式继续。",
  "recoverable": true,
  "nextActions": ["CHECK_LOCAL_ENV", "SWITCH_TO_OFFLINE_FAKE", "RETRY"],
  "details": {
    "modelMode": "openai-gpt55",
    "modelName": "gpt-5.5",
    "preservedSessionId": "session-id"
  }
}
```

错误码：

- `MODEL_TIMEOUT`
- `MODEL_AUTH_FAILED`
- `MODEL_RATE_LIMITED`
- `MODEL_FORMAT_INVALID`
- `MODEL_SERVICE_UNAVAILABLE`

规则：

- `details` 不得包含 API Key、Authorization、Cookie、Session Token 或 provider 原始敏感错误体。
- 错误必须可恢复，并保留当前会话和用户输入。
- 前端必须提供重试或切回 `offline-fake` 的路径。

## 敏感信息过滤契约

后端和前端都必须过滤以下模式：

```text
API Key
Authorization
Cookie
Session Token
Bearer token
```

验收要求：

- `requestPreview.rawJson` 中出现真实或伪造密钥的次数为 0。
- 前端错误消息和测试快照中出现可复用凭证的次数为 0。
- 后端日志不得记录请求 header 或密钥值。
