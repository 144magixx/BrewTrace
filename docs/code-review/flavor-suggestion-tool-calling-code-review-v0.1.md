# flavor_suggestion Tool Calling 代码审核 v0.1

## 1. 审核结论

本次已完成 `flavor_suggestion` 的受控 Tool Calling 闭环：模型请求可以看到 `flavor_suggestion` 工具 schema；当 `/responses` 返回 `function_call` 时，后端会通过 Spring AI `ToolCallingManager` 调用项目内 `ToolAdapter`；工具调用仍经过 `ToolCallPolicy`、`ToolCallRecorder`，并把结果标记为 `PENDING_ASSOCIATION`，不会写入已确认事实。

验证结果：使用 Java 25 执行 `./mvnw test`，后端 31 个测试通过。

## 2. 已确认事实

- 当前项目使用自定义 `ResponsesApiChatModel` 适配 `/responses`，不能只依赖 Spring AI 默认 OpenAI ChatModel 的工具序列化。
- `flavor_suggestion` 是低风险、无副作用工具，结果只适合作为待确认风味联想。
- 本次没有开放小红书、图片生成、归档或偏好写入工具。
- 工作区中 `frontend/src/features/conversation/ConversationComposer.tsx` 和 `frontend/src/features/conversation/QuestionAnswerDialog.tsx` 已存在改动，但不属于本次 Tool Calling 后端实现。

## 3. 修改文件

### 工具注册与安全边界

- `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tools/application/ToolRegistry.java`
  - 扩展 `ToolDefinition`，新增 `inputSchema`、`outputSchema`、`resultBoundary`、`sideEffectType`、`autonomousAllowed`。
  - 保留旧构造器，兼容已有小红书工具测试。

- `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tools/infrastructure/FlavorSuggestionToolAdapter.java`
  - 新增 `flavor_suggestion` 工具适配器。
  - 输入 `inputTerm`、`temperatureStage`、`senseType`、`limit`。
  - 输出候选风味，并统一标记 `basisType=PENDING_ASSOCIATION`、`confirmationStatus=PENDING_CONFIRMATION`、`sendStatus=SEND_AFTER_CONFIRMATION`。

- `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tools/infrastructure/FlavorSuggestionToolRegistrar.java`
  - 注册 `flavor_suggestion` 工具定义和 JSON schema。
  - 工具风险级别为 `LOW`，副作用为 `NONE`，允许模型自主调用。

- `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tools/infrastructure/SpringAiToolCallbackAdapter.java`
  - 新增 Spring AI `ToolCallback` 适配器。
  - 将 Spring AI 工具 JSON 入参转成项目内 `ToolAdapter.ToolRequest`。
  - 执行前调用 `ToolCallPolicy.verify()`，执行后调用 `ToolCallRecorder.record()`。

- `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tools/infrastructure/ToolConfiguration.java`
  - 注册 `FlavorSuggestionService`、`ToolRegistry`、`ToolCallPolicy`、`ToolCallRecorder`、`flavorSuggestionToolCallback`。
  - 注册带静态 callback resolver 的 `ToolCallingAdvisor`，使工具名解析只来自项目受控 callbacks。

### 模型链路

- `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/ModelGatewayConfiguration.java`
  - 将 `List<ToolCallback>` 注入 `ResponsesApiChatModel` 和 `SpringAiModelGateway`。

- `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesRequestFactory.java`
  - 当存在工具 callbacks 时，在 `/responses` 请求体中加入 `tools` 和 `tool_choice=auto`。
  - 支持第二轮工具调用输入：`function_call` 和 `function_call_output`。

- `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/ResponsesApiChatModel.java`
  - 解析 `/responses` 返回的 `function_call`，转换为 Spring AI `AssistantMessage.ToolCall`。
  - 保留普通模型文本返回逻辑，继续交给 `OpenAiResponsesParser` 解析 `messageType/talk`。

- `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/SpringAiModelGateway.java`
  - 在每次模型调用中注入工具上下文：`sessionId`、调用目的、确认状态。
  - 使用 Spring AI `DefaultToolCallingManager` 执行最多 3 轮工具调用循环。
  - 工具调用完成后再解析最终模型消息，保持现有 `ModelGateway` 契约不变。

### 测试

- `backend/src/test/java/com/minyuwei/xhs/coffeeagent/tools/infrastructure/FlavorSuggestionToolAdapterTest.java`
  - 覆盖风味联想工具输出、待确认边界和发送状态。

- `backend/src/test/java/com/minyuwei/xhs/coffeeagent/tools/infrastructure/SpringAiToolCallbackAdapterTest.java`
  - 覆盖 Spring AI callback 调用、schema 暴露和 `ToolCallRecorder` 记录。

- `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesToolRequestFactoryTest.java`
  - 覆盖 `/responses` 请求体中工具 schema 的序列化。

- `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/SpringAiModelGatewayTest.java`
  - 新增工具调用循环测试：模拟模型先返回 `flavor_suggestion` 的 `function_call`，后端执行工具，再发送带 `function_call_output` 的第二轮请求并解析最终 `CONVERSATION`。

- `backend/src/test/java/com/minyuwei/xhs/coffeeagent/support/TestRunner.java`
  - 将新增工具测试纳入现有轻量行为测试入口。

## 4. 关键业务链路

```text
用户消息进入工作台
  -> AgentStateAssembler 组装 ModelContextPackage
  -> SpringAiModelGateway 创建 Prompt，并绑定 flavor_suggestion ToolCallback
  -> OpenAiResponsesRequestFactory 生成 /responses 请求体，包含 tools schema
  -> 模型可选择返回 function_call: flavor_suggestion
  -> ResponsesApiChatModel 将 function_call 转成 Spring AI ToolCall
  -> SpringAiModelGateway 使用 DefaultToolCallingManager 执行工具
  -> SpringAiToolCallbackAdapter 调用 ToolCallPolicy
  -> FlavorSuggestionToolAdapter 调用 FlavorSuggestionService
  -> ToolCallRecorder 记录工具入参摘要和结果摘要
  -> 第二轮 /responses 请求带 function_call_output
  -> 模型返回最终 CONVERSATION 或 POST
  -> OpenAiResponsesParser 继续解析 messageType / talk / conversation / post
```

## 5. 业务边界

- `flavor_suggestion` 输出不是事实，只是候选联想。
- 工具结果默认不能进入 `confirmedFacts`。
- 工具结果可以被模型用于继续追问，例如让用户在“柠檬 / 青柠 / 甜橙”中确认更贴近哪一个。
- 公开发布、图片生成、归档记录、偏好更新仍未开放给模型自主调用。

## 6. 验证记录

执行命令：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home ./mvnw test
```

结果：

- 后端测试通过：31 tests, 0 failures, 0 errors。
- 覆盖工具 schema 序列化、Spring AI callback 执行、工具调用记录、模型工具调用双轮请求。

环境说明：

- 当前 shell 默认 Java 是 1.8，直接执行 `./mvnw test` 会因 Java 版本过低无法编译 Java 21 语法。
- 使用 Homebrew OpenJDK 25 并由 Maven `--release 21` 编译后验证通过。
- Maven 输出中仍提示本地 `~/.m2/repository/org/junit/junit-bom/5.11.1/junit-bom-5.11.1.pom` 文件内容异常，但本次使用 Java 25 验证未被阻断。

## 7. 待确认与后续建议

- 待确认：是否要用真实 `gpt-5.5` 链路做一次端到端联调，确认模型在真实提示词下会选择 `flavor_suggestion`，并能基于工具结果自然追问。
- 待确认：右侧 Agent 状态卡是否需要单独展示“工具调用卡片”。当前工具调用已经进入 `ToolCallRecorder` / `AgentTrace`，但工作台状态卡的展示颗粒度还可以继续增强。
- 后续建议：下一个低风险工具可以做 `fact_boundary_review`，但需要先把当前 `FactBoundaryChecker` 中演示性质的固定检查拆成纯审稿规则。
