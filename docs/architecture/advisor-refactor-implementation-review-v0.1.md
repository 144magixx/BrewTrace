# Advisor 化模型链路改造说明 v0.1

## 1. 背景

本次改造对应《Spring AI Agent 能力演进建议 v0.1》的第一阶段：Advisor 化当前模型链路。目标不是改变用户流程，而是把模型调用前后的上下文预览、事实边界摘要、调用轨迹和响应元数据收进 Spring AI Advisor 链。

## 2. 改造前现状

改造前链路为：

```text
WorkbenchService
  -> AgentStateAssembler.completeModel
  -> ModelContextPackageAssembler
  -> SpringAiModelGateway
  -> ChatClient
  -> ResponsesApiChatModel
  -> OpenAiResponsesParser
  -> AgentStateAssembler / WebWorkbenchService
```

已有能力：

- `ModelGateway` 已经隔离业务层和模型层。
- `ChatClient` 已经接入 Spring AI。
- `ResponsesApiChatModel` 已经适配内部 Responses API。
- 模型输出已经收敛为 `messageType`、`talk`、`conversation`、`post`、`warnings`。
- `WebWorkbenchService` 已经根据 `POST` 生成三版文案卡片。
- `AgentStateAssembler` 已经展示上下文预览、请求预览、响应预览、模型输出和事实边界检查。

主要问题：

- `SpringAiModelGateway` 同时承担 prompt 创建、请求预览、模型调用、响应摘要和错误恢复，职责偏重。
- Spring AI Advisor 尚未使用，模型调用前后缺少标准化横切链路。
- 未来接入 Tool Calling、ChatMemory、RAG 时容易继续堆到 gateway。

## 3. 改造后架构

改造后链路为：

```text
WorkbenchService
  -> AgentStateAssembler.completeModel
  -> ModelContextPackageAssembler
  -> SpringAiModelGateway
  -> ChatClient + Advisor 链
       -> FactBoundaryAdvisor
       -> ContextPreviewAdvisor
       -> AgentTraceAdvisor
  -> ResponsesApiChatModel
  -> OpenAiResponsesParser
  -> AgentStateAssembler / WebWorkbenchService
```

新增 Advisor：

- `FactBoundaryAdvisor`：模型调用前记录事实边界摘要，包括当前会话、已确认事实、待确认联想、候选记忆、排除项和动态约束数量。
- `ContextPreviewAdvisor`：基于最终 `Prompt` 生成脱敏请求预览，并通过 `ChatClientResponse.context()` 回传。
- `AgentTraceAdvisor`：记录模型调用结果、耗时、模型名、传输链路、请求预览摘要、响应摘要和失败信息。

新增支撑：

- `ModelAdvisorContextKeys`：统一管理 Advisor context key。
- `AgentTraceConfiguration`：补齐 trace repository/service/recorder 的 Spring bean。
- `ChatClient` 从 `ChatClient.create(chatModel)` 改为 `ChatClient.builder(chatModel).defaultAdvisors(...)`。

## 4. 产品行为改造

同时调整了模型路由 prompt：

- 与咖啡无关的话题必须返回 `CONVERSATION`，温和引导用户回到咖啡记录。
- 信息不足时必须一次只问一个最自然的下一步问题。
- `conversation.questions` 在 schema 层收紧为 `minItems=1`、`maxItems=1`。
- 后端校验新增：`CONVERSATION` 必须恰好包含 1 个追问。
- 聊天气泡不再自动追加“我想确认：1...”列表，只展示 `talk`。

## 5. 复用架构

继续复用：

- `ModelGateway`
- `SpringAiModelGateway`
- `ResponsesApiChatModel`
- `OpenAiResponsesRequestFactory`
- `OpenAiResponsesParser`
- `ModelContextPackageAssembler`
- `AgentStateAssembler`
- `FactBoundaryChecker`
- `AgentTraceRecorder`
- `SensitiveValueRedactor`
- `backend/src/main/resources/prompts/`

没有引入：

- Spring AI Tool Calling
- ChatMemory
- VectorStore / RAG
- MCP
- 新数据库表
- 自动发布或其他高影响工具动作

## 6. 验证

自动化验证：

- `cd backend && ./mvnw test`
- `cd frontend && npm test -- --run`

真实模型验证：

- 后端使用 `OPENAI_API_KEY` 环境变量启动。
- 真实 API 验证了跑题拉回、逐步追问、信息补全后 POST 三版草稿。
- Chrome 页面验收截图见：
  - [advisor-offtopic-chrome.png](./validation/advisor-offtopic-chrome.png)
  - [advisor-stepwise-post-chrome.png](./validation/advisor-stepwise-post-chrome.png)

## 7. 当前限制

- `AgentTraceRecorder` 仍是单步 trace，后续接工具链路时需要升级为完整工作流 trace。
- `FactBoundaryChecker` 中仍存在示例型无依据检查项，后续应改为基于模型输出和事实引用动态生成。
- Advisor 目前只覆盖模型调用链路，不负责 `POST -> DraftTab` 这类产品动作路由；该职责仍归 `WebWorkbenchService`。
