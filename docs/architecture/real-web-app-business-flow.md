# 真实 Web 应用业务链路学习指南

## 已确认事实

- 本切片已经把后端接成 Spring Boot Web 服务。
- 前端已经接成 React + Vite 工作台。
- 当前主流程只保留 GPT-5.5 文本模型链路；暂未接真实数据库、小红书账号或图片生成。
- 当前模型入口已经改为 Spring AI `ChatClient`；`/responses` 仅作为内部兼容 transport。

## 链路总览

```text
浏览器动作
  -> frontend/src/app/App.tsx
  -> frontend/src/services/workbenchApi.ts
  -> backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java
  -> backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/WebWorkbenchService.java
  -> backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/application/TastingSessionApplicationService.java
  -> backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java
  -> SpringAiModelGateway / ChatClient / ResponsesApiChatModel
  -> WebWorkbenchService 映射真实模型输出成页面快照
  -> React 工作台更新消息、摘要、草稿和错误提示
```

## 创建会话

用户点击“开始记录”后：

1. `App.tsx` 调用 `createSession()`。
2. `workbenchApi.ts` 请求 `POST /api/workbench/sessions`。
3. `WorkbenchController` 接收请求并返回 `ApiResponse<WorkbenchSnapshot>`。
4. `WebWorkbenchService` 调用 `TastingSessionApplicationService.createSession()`。
5. 会话保存在内存 `TastingSessionRepositoryAdapter`。
6. 前端保存 `lastSessionId` 到 `localStorage`，用于刷新恢复。

## 首轮提交

用户输入“今天喝了一支水洗埃塞，有柑橘和红茶感”后：

1. `App.tsx` 调用 `submitMessage(sessionId, content)`。
2. 后端把用户消息追加到 `TastingSession`，不再触发本地追问或本地草稿生成。
3. `AgentStateAssembler` 组装当前会话和已确认事实上下文。
4. 后端调用 GPT-5.5 生成克制版、夸张版和锐评版三类草稿。
5. `WebWorkbenchService` 根据真实模型结果返回 `DRAFTS_READY`；模型失败时返回可恢复错误。
6. 页面中间以聊天气泡展示用户消息和真实模型自然语言输出；右侧展示请求/响应预览和事实边界检查。

## AgentState 可视化链路

003 切片新增的页面状态链路如下：

```text
页面动作
  -> 工作台 API 返回 WorkbenchSnapshot
  -> WebWorkbenchService 读取 WorkspaceSnapshot
  -> AgentStateAssembler 组装 AgentStateSnapshot
  -> 右侧 AgentStateCards 展示状态卡片、上下文预览、真实模型输出和能力边界
```

`AgentStateAssembler` 使用当前内存会话和已确认事实组装真实模型上下文，不再生成本地联想、候选记忆或替代输出。上下文预览按当前会话、已确认事实、待确认联想、候选记忆分组，当前 GPT-5.5-only 模式下后两类默认为空。

提交用户输入后，后端调用 GPT-5.5 返回三版文案，并执行事实边界检查。事实边界检查至少区分用户确认、模型推断、待确认表达和无依据表达，避免模型把未确认内容写成用户事实。

## 真实模型生成链路

004 切片已收敛为唯一 `openai-gpt55` 垂直链路，当前默认模型入口为 Spring AI：

```text
工作台提交
  -> WebWorkbenchService 读取 WorkspaceSnapshot
  -> ModelContextPackageAssembler 组装当前会话、已确认事实和排除项
  -> SpringAiModelGateway
  -> OpenAiResponsesRequestFactory 构造 Spring AI Prompt 和脱敏请求预览
  -> ChatClient
  -> ResponsesApiChatModel
  -> OpenAiResponsesLlmClient POST /responses
  -> OpenAiResponsesParser 校验克制版、夸张版、锐评版
  -> AgentStateAssembler 写入 modelMode、modelOutput、requestPreview、responsePreview 和 factBoundaryChecks
  -> React 中间聊天区展示三版文案气泡，右侧 Agent 状态区展示 JSON 预览和事实边界
```

`openai-gpt55` 是唯一文本模型模式；无 `OPENAI_API_KEY` 时返回可恢复错误，不生成本地替代输出。真实请求预览只展示脱敏后的 JSON body，不展示 Authorization header、Cookie、Session Token 或 API Key。真实模型失败会映射为 `MODEL_TIMEOUT`、`MODEL_AUTH_FAILED`、`MODEL_RATE_LIMITED`、`MODEL_FORMAT_INVALID` 或 `MODEL_SERVICE_UNAVAILABLE`，并保留当前输入、会话和上下文预览。后续如果内部网关支持 Spring AI OpenAI Chat Completions，可在 `ModelGatewayConfiguration` 中替换底层 `ChatModel`，业务层不需要改。

## 清空当前会话

用户点击“新建记录 / 清空当前会话”后，前端先显示确认弹窗。取消时不调用后端、不修改快照；确认时调用：

```text
POST /api/workbench/sessions/{sessionId}/clear
```

后端只删除指定内存会话并返回 `EMPTY` 工作台快照。前端同时调用 `clearSessionResume(sessionId)` 清除浏览器中的旧 `lastSessionId` 和草稿，并记录 `clearedSessionIds`，刷新后不恢复已清空会话。

## 错误恢复

当后端不可用时：

1. `workbenchApi.ts` 捕获 `fetch` 失败。
2. 前端生成 `SERVICE_UNAVAILABLE` 可恢复错误。
3. `App.tsx` 保留当前输入到 `localStorage`。
4. `RecoverableErrorBanner` 展示错误类别、恢复建议、重试和重新创建入口。

## 待用户确认假设

- 当前只支持本地单用户会话。
- 当前恢复状态只用于浏览器便利性，不是长期咖啡记录。
- 后续如果接入真实长期数据库、小红书或生图，应分别重新做规格、计划、任务和验收。
