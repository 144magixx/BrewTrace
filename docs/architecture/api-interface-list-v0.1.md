# 当前后端接口清单 v0.1

## 1. 文档目标

本文只梳理后端接口，重点回答三个问题：

- 当前哪些接口已经通过 Spring MVC 暴露为 HTTP 接口。
- 每个接口的源码出处在哪里，精确到文件和行号。
- 哪些 `api` 包下的类只是应用内接口外观，还没有 HTTP 路由。

本文基于 2026-07-08 当前源码整理。若后续 Controller 注解、方法签名或 DTO 字段变化，需要同步更新本文中的行号。

## 2. 后端启动入口

| 类型 | 源码出处 | 说明 |
| --- | --- | --- |
| Spring Boot 启动类 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/CoffeeAgentApplication.java:7` | `@SpringBootApplication`。 |
| 进程入口 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/CoffeeAgentApplication.java:10` | `main()` 调用 `SpringApplication.run()`。 |

当前本地后端默认监听端口来自：

```text
backend/src/main/resources/application.yml
```

默认端口：

```text
8080
```

## 3. 当前 HTTP 暴露结论

当前只有 `WorkbenchController` 是真实 HTTP Controller。

判定依据：

| 判定项 | 源码出处 |
| --- | --- |
| `@RestController` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:25` |
| `@RequestMapping("/api/workbench")` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:26` |
| Controller 类定义 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:28` |

也就是说，当前后端真实暴露的 HTTP 路由只有：

```text
GET  /api/workbench/snapshot
POST /api/workbench/sessions
POST /api/workbench/sessions/{sessionId}/messages
POST /api/workbench/sessions/{sessionId}/messages/stream
POST /api/workbench/sessions/{sessionId}/clear
```

其他 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/**/api/*Controller.java` 当前没有 `@RestController`、`@RequestMapping`、`@GetMapping` 或 `@PostMapping` 注解，不是可直接通过 HTTP 调用的后端接口。

## 4. 通用响应与错误结构

### 4.1 REST Envelope

所有当前工作台 REST JSON 接口返回 `ApiResponse<T>`。

| 对象 | 源码出处 | 字段 |
| --- | --- | --- |
| `ApiResponse<T>` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/api/ApiResponse.java:5` | `requestId`、`data`、`error` |
| 成功构造 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/api/ApiResponse.java:6` | `data` 有值，`error` 为 `null`。 |
| 失败构造 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/api/ApiResponse.java:10` | `data` 为 `null`，`error` 有值。 |

成功响应形状：

```json
{
  "requestId": "req-id",
  "data": {},
  "error": null
}
```

失败响应形状：

```json
{
  "requestId": "req-id",
  "data": null,
  "error": {
    "code": "SESSION_NOT_FOUND",
    "category": "USER_FIXABLE",
    "message": "品鉴会话不存在。",
    "recoverable": true,
    "nextActions": ["CREATE_SESSION"],
    "details": {}
  }
}
```

### 4.2 错误对象

| 对象 | 源码出处 | 字段 |
| --- | --- | --- |
| `ApiError` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/error/ApiError.java:6` | `code`、`category`、`message`、`recoverable`、`nextActions`、`details` |
| `ApiError.of()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/error/ApiError.java:14` | 构造无 details 的错误。 |
| `ApiError.withDetails()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/error/ApiError.java:18` | 附加错误 details。 |
| `ErrorCategory` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/error/ErrorCategory.java:3` | 错误分类枚举。 |

`ErrorCategory` 当前枚举：

| 枚举值 | 源码出处 |
| --- | --- |
| `USER_FIXABLE` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/error/ErrorCategory.java:4` |
| `RETRYABLE` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/error/ErrorCategory.java:5` |
| `DEGRADED` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/error/ErrorCategory.java:6` |
| `FATAL` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/error/ErrorCategory.java:7` |
| `SAFETY_BLOCKED` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/error/ErrorCategory.java:8` |

## 5. 工作台 HTTP 接口

### 5.1 获取工作台快照

```text
GET /api/workbench/snapshot
GET /api/workbench/snapshot?sessionId={sessionId}
```

| 项目 | 内容 |
| --- | --- |
| 路由注解 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:35` |
| 处理方法 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:36` |
| 请求头 `X-Request-Id` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:37` |
| Query 参数 `sessionId` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:38` |
| Service 调用 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:40` |
| 返回 DTO | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:32` |

请求参数：

| 名称 | 位置 | 必填 | 说明 |
| --- | --- | --- | --- |
| `X-Request-Id` | header | 否 | 不传时后端生成 UUID。 |
| `sessionId` | query | 否 | 不传时返回空工作台快照。 |

响应：

```text
ApiResponse<WebWorkbenchDtos.WorkbenchSnapshot>
```

### 5.2 创建工作台会话

```text
POST /api/workbench/sessions
Content-Type: application/json
```

| 项目 | 内容 |
| --- | --- |
| 路由注解 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:43` |
| 处理方法 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:44` |
| 请求头 `X-Request-Id` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:45` |
| 请求体 DTO | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:23` |
| 默认编排模式 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:48` |
| Service 调用 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:49` |
| 返回 DTO | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:32` |

请求体：

```json
{
  "mode": "EXPLICIT_WORKFLOW"
}
```

字段：

| 字段 | 类型 | 必填 | 源码出处 | 说明 |
| --- | --- | --- | --- | --- |
| `mode` | `OrchestrationMode` | 否 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:23` | 不传时使用 `EXPLICIT_WORKFLOW`。 |

`OrchestrationMode` 枚举出处：

| 枚举值 | 源码出处 |
| --- | --- |
| `EXPLICIT_WORKFLOW` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/OrchestrationMode.java:4` |
| `MODEL_TOOL_CALLING` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/OrchestrationMode.java:5` |

响应：

```text
ApiResponse<WebWorkbenchDtos.WorkbenchSnapshot>
```

### 5.3 提交用户消息，返回完整快照

```text
POST /api/workbench/sessions/{sessionId}/messages
Content-Type: application/json
```

| 项目 | 内容 |
| --- | --- |
| 路由注解 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:52` |
| 处理方法 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:53` |
| 请求头 `X-Request-Id` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:54` |
| 路径参数 `sessionId` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:55` |
| 请求体 DTO | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:26` |
| 读取 `content` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:59` |
| 读取 `modelMode` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:60` |
| Service 调用 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:61` |
| 异常转 envelope | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:62` |
| 返回 DTO | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:32` |

请求体：

```json
{
  "content": "今天喝了一支水洗埃塞，有柑橘和红茶感。",
  "modelMode": "openai-gpt55"
}
```

字段：

| 字段 | 类型 | 必填 | 源码出处 | 说明 |
| --- | --- | --- | --- | --- |
| `content` | string | 是 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:26` | 用户输入内容。 |
| `modelMode` | string | 否 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:26` | 当前实际解析为 `openai-gpt55`。 |

`modelMode` 当前唯一模式出处：

| 模式 | 源码出处 |
| --- | --- |
| `openai-gpt55` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ModelMode.java:4` |

响应：

```text
ApiResponse<WebWorkbenchDtos.WorkbenchSnapshot>
```

### 5.4 流式提交用户消息

```text
POST /api/workbench/sessions/{sessionId}/messages/stream
Content-Type: application/json
Accept: text/event-stream
```

| 项目 | 内容 |
| --- | --- |
| 路由注解 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:68` |
| 处理方法 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:69` |
| 请求头 `X-Request-Id` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:70` |
| 路径参数 `sessionId` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:71` |
| 请求体 DTO | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:26` |
| SSE 超时设置 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:74` |
| 记录用户消息 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:80` |
| 完成 assistant 回合 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:90` |
| 推送 assistant 文本 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:91` |
| 推送最终快照 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:92` |
| 异常转 SSE error | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:98` |

请求体：

```json
{
  "content": "今天喝了一支水洗埃塞，有柑橘和红茶感。",
  "modelMode": "openai-gpt55"
}
```

SSE 事件：

| 事件名 | 源码出处 | data 说明 |
| --- | --- | --- |
| `user_message` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:81` | `{ "requestId": "...", "message": WebConversationMessage }` |
| `assistant_start` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:86` | `{ "requestId": "...", "id": "assistant-stream-id" }` |
| `assistant_delta` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:91` | 由 `streamAssistantContent()` 逐块发送。 |
| `snapshot` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:92` | `{ "requestId": "...", "snapshot": WorkbenchSnapshot }` |
| `done` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:96` | `{ "requestId": "..." }` |
| `error` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:98` | `{ "requestId": "...", "error": ApiError }` |

正常事件顺序：

```text
user_message
assistant_start
assistant_delta*
snapshot
done
```

### 5.5 清空当前工作台会话

```text
POST /api/workbench/sessions/{sessionId}/clear
Content-Type: application/json
```

| 项目 | 内容 |
| --- | --- |
| 路由注解 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:108` |
| 处理方法 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:109` |
| 请求头 `X-Request-Id` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:110` |
| 路径参数 `sessionId` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:111` |
| 请求体 DTO | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:29` |
| 读取确认字段 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:115` |
| Service 调用 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:116` |
| 异常转 envelope | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java:117` |
| 返回 DTO | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:32` |

请求体：

```json
{
  "confirmed": true
}
```

字段：

| 字段 | 类型 | 必填 | 源码出处 | 说明 |
| --- | --- | --- | --- | --- |
| `confirmed` | boolean | 是 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:29` | 必须为 `true`，否则业务层返回确认错误。 |

响应：

```text
ApiResponse<WebWorkbenchDtos.WorkbenchSnapshot>
```

## 6. 工作台响应 DTO 出处

### 6.1 顶层快照

| DTO | 源码出处 | 说明 |
| --- | --- | --- |
| `WorkbenchSnapshot` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:32` | 工作台所有普通 REST 接口的主要 `data` 类型。 |
| `WebConversationMessage` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:46` | 会话消息。 |
| `RecordSummary` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:55` | 当前记录摘要。 |
| `DraftTab` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:64` | 三版文案草稿展示对象。 |
| `AgentStateSnapshot` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:75` | Agent 状态快照。 |

### 6.2 Agent 状态

| DTO | 源码出处 | 说明 |
| --- | --- | --- |
| `AgentStatusCard` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:91` | Agent 状态卡片。 |
| `ContextItem` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:103` | 当前会话上下文项。 |
| `ConfirmedFact` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:115` | 已确认事实。 |
| `PendingAssociation` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:125` | 待确认联想。 |
| `CandidateMemory` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:135` | 候选记忆。 |
| `ContextPreview` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:148` | 模型请求上下文预览。 |
| `ContextPreviewSection` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:158` | 上下文预览分区。 |
| `ContextPreviewItem` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:165` | 上下文预览条目。 |
| `ModelOutputSnapshot` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java:173` | 模型输出快照。 |

### 6.3 工作台状态与模型路由枚举

| 枚举 | 值 | 源码出处 |
| --- | --- | --- |
| `WebWorkbenchSession.Status` | `EMPTY` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/domain/WebWorkbenchSession.java:8` |
| `WebWorkbenchSession.Status` | `SESSION_CREATED` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/domain/WebWorkbenchSession.java:9` |
| `WebWorkbenchSession.Status` | `WAITING_FOR_FACTS` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/domain/WebWorkbenchSession.java:10` |
| `WebWorkbenchSession.Status` | `DRAFTS_READY` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/domain/WebWorkbenchSession.java:11` |
| `WebWorkbenchSession.Status` | `ERROR_RECOVERABLE` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/domain/WebWorkbenchSession.java:12` |
| `ModelMessageType` | `POST` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ModelMessageType.java:4` |
| `ModelMessageType` | `CONVERSATION` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ModelMessageType.java:5` |

## 7. 工作台 HTTP 接口依赖的后端 Bean 出处

| Bean 或服务 | 源码出处 | 说明 |
| --- | --- | --- |
| `TastingSessionRepository` Bean | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/config/WebWorkbenchConfiguration.java:12` | 当前实现为内存仓储。 |
| `CurrentUserProvider` Bean | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/config/WebWorkbenchConfiguration.java:17` | 本地单用户上下文。 |
| `TastingSessionApplicationService` Bean | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/config/WebWorkbenchConfiguration.java:22` | 会话应用服务。 |
| `ChatModel` Bean | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/ModelGatewayConfiguration.java:49` | Responses API 兼容模型。 |
| `ChatClient` Bean | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/ModelGatewayConfiguration.java:75` | Spring AI ChatClient。 |
| `ModelGateway` Bean | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/ModelGatewayConfiguration.java:83` | 工作台模型调用网关。 |

## 8. 未 HTTP 暴露的应用内接口形状

本节列出的类虽然命名为 `Controller`，但当前没有 Spring MVC 注解，不是 HTTP 路由。它们只能说明后端业务边界已有接口形状，不能直接当作 URL 使用。

### 8.1 tasting

| 类/方法 | 源码出处 | 业务含义 | HTTP 状态 |
| --- | --- | --- | --- |
| `TastingSessionController` 类 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/api/TastingSessionController.java:10` | 品鉴会话外观。 | 未暴露 |
| `createSession()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/api/TastingSessionController.java:18` | 创建品鉴会话。 | 未暴露 |
| `submitMessage()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/api/TastingSessionController.java:24` | 提交用户消息。 | 未暴露 |
| `workspace()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/api/TastingSessionController.java:28` | 获取工作区快照。 | 未暴露 |
| `TastingTemplateController` 类 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/api/TastingTemplateController.java:13` | 结构化品鉴模板外观。 | 未暴露 |
| `save()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/api/TastingTemplateController.java:21` | 保存结构化豆子、冲煮、评分和风味。 | 未暴露 |
| `ArchiveController` 类 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/api/ArchiveController.java:9` | 归档外观。 | 未暴露 |
| `archive()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/api/ArchiveController.java:17` | 归档咖啡记录。 | 未暴露 |
| `BagImageController` 类 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/api/BagImageController.java:8` | 豆袋图片外观。 | 未暴露 |
| `upload()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/api/BagImageController.java:16` | 上传并解析豆袋图片。 | 未暴露 |

### 8.2 flavor

| 类/方法 | 源码出处 | 业务含义 | HTTP 状态 |
| --- | --- | --- | --- |
| `FlavorSuggestionController` 类 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/flavor/api/FlavorSuggestionController.java:11` | 风味联想外观。 | 未暴露 |
| `suggest()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/flavor/api/FlavorSuggestionController.java:19` | 基于输入词、温度段和感官类型做风味联想。 | 未暴露 |

### 8.3 copywriting

| 类/方法 | 源码出处 | 业务含义 | HTTP 状态 |
| --- | --- | --- | --- |
| `DraftController` 类 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/copywriting/api/DraftController.java:10` | 文案草稿外观。 | 未暴露 |
| `generateDrafts()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/copywriting/api/DraftController.java:18` | 生成文案草稿。 | 未暴露 |

### 8.4 memory

| 类/方法 | 源码出处 | 业务含义 | HTTP 状态 |
| --- | --- | --- | --- |
| `MemoryRecallController` 类 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/memory/api/MemoryRecallController.java:10` | 记忆召回外观。 | 未暴露 |
| `recall()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/memory/api/MemoryRecallController.java:18` | 按 query 召回历史记忆。 | 未暴露 |

### 8.5 publishing

| 类/方法 | 源码出处 | 业务含义 | HTTP 状态 |
| --- | --- | --- | --- |
| `PublishingPackageController` 类 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/api/PublishingPackageController.java:10` | 发布包外观。 | 未暴露 |
| `create()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/api/PublishingPackageController.java:18` | 创建发布包。 | 未暴露 |
| `fillXhs()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/api/PublishingPackageController.java:22` | 填写小红书发布页。 | 未暴露 |
| `publishXhs()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/api/PublishingPackageController.java:26` | 发布小红书。 | 未暴露 |
| `ImageGenerationController` 类 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/api/ImageGenerationController.java:7` | 图片生成外观。 | 未暴露 |
| `generate()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/api/ImageGenerationController.java:10` | 生成图片资产。 | 未暴露 |
| `ExternalReferenceController` 类 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/api/ExternalReferenceController.java:10` | 外部参考外观。 | 未暴露 |
| `search()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/api/ExternalReferenceController.java:18` | 搜索外部参考。 | 未暴露 |

### 8.6 trace

| 类/方法 | 源码出处 | 业务含义 | HTTP 状态 |
| --- | --- | --- | --- |
| `AgentTraceController` 类 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/trace/api/AgentTraceController.java:10` | Agent 轨迹外观。 | 未暴露 |
| `traces()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/trace/api/AgentTraceController.java:18` | 查询指定会话的 Agent 轨迹。 | 未暴露 |

### 8.7 user

| 类/方法 | 源码出处 | 业务含义 | HTTP 状态 |
| --- | --- | --- | --- |
| `UserPreferenceController` 类 | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/user/api/UserPreferenceController.java:9` | 用户偏好外观。 | 未暴露 |
| `listCandidates()` | `backend/src/main/java/com/minyuwei/xhs/coffeeagent/user/api/UserPreferenceController.java:12` | 返回用户偏好候选。 | 未暴露 |

## 9. 后续维护规则

新增或调整后端接口时，请同步更新：

- HTTP 方法和路径。
- Controller 注解出处。
- 处理方法出处。
- 请求 DTO 出处。
- 响应 DTO 出处。
- 是否真实 HTTP 暴露。

如果只是新增应用内 `api` 外观类，但没有 Spring MVC 注解，应继续放入“未 HTTP 暴露的应用内接口形状”章节，避免误认为已经存在可调用 URL。
