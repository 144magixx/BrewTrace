# CR：模型请求预览改为展示实际发送体

> **变更范围**：模型请求预览不再通过请求工厂二次序列化，而是复用真正交给 HTTP Client 的请求体，并且只在展示前执行敏感值脱敏。
>
> **CR 日期**：2026-07-11
>
> **验证状态**：后端全量 51 项测试通过；本地运行态已验证 `gpt-5.6-terra` 请求包含 `flavor_suggestion` 及其完整 `description`。

---

## 一、改造概述

| 维度 | 改造前 | 改造后 |
|---|---|---|
| 预览数据来源 | Advisor 或网关再次调用请求工厂，重新生成一份“预计请求体” | 发送层在调用 HTTP Client 前捕获同一份最终 `requestBody` |
| 工具定义可见性 | 预览重建时可能丢失 Spring AI 最终注入的工具，导致 `tools` 为空或缺少 `description` | 展示最终发送体中的完整 `tools`、`tool_choice` 和工具 `description` |
| 工具调用循环 | 预览可能停留在初始 Prompt，无法反映工具返回后的后续请求 | 每次发送覆盖当前线程捕获值，最终预览对应 HTTP Client 收到的最后一份请求体 |
| 展示处理 | 重新序列化后再展示 | 不解析、不重排、不重新序列化，仅对原始字符串执行敏感值脱敏 |
| 异常路径 | 预先生成 fallback 预览，即使发送层未执行也可能展示一份推测请求 | 仅在发送层确实生成请求体后展示；未发送时正文为空 |
| 并发与生命周期 | 无专门的实际请求体生命周期 | 使用 `ThreadLocal` 按同步调用线程隔离，并在每次调用前后清理 |

---

## 二、按业务链路的变动清单

### 链路 1：实际发送体捕获

#### `ActualModelRequestCapture.java` — 新增

| 行号 | 变动 | 说明 |
|---:|---|---|
| L5–9 | 新增 | 定义捕获器边界：只保存当前同步模型调用真正交给 HTTP Client 的请求体，并要求调用结束后清理。 |
| L10–11 | 新增 | 使用 `ThreadLocal<String>` 隔离并发请求，避免不同会话互相覆盖。 |
| L13–20 | 新增 | `record(requestBody)` 保存发送层已经完成序列化的原始请求体。 |
| L22–29 | 新增 | `latest()` 原样返回最近一次捕获值，不执行重新序列化。 |
| L31–36 | 新增 | `clear()` 移除当前线程数据，防止线程池复用导致会话内容残留。 |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/ActualModelRequestCapture.java`

---

### 链路 2：Responses API 发送层

#### `ResponsesApiChatModel.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L29 | 新增字段 | 持有与预览链路共享的 `ActualModelRequestCapture`。 |
| L31–90 | 修改构造器 | 增加可注入共享捕获器的构造器，同时保留旧构造方式的兼容入口。 |
| L92–104 | 修改发送逻辑 | `call()` 只序列化一次，在 `client.createResponse(...)` 前记录同一个 `requestBody`。 |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/ResponsesApiChatModel.java`

---

### 链路 3：Advisor 请求预览

#### `ContextPreviewAdvisor.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L16–18 | 修改依赖 | 由请求工厂改为依赖实际请求体捕获器。 |
| L20–38 | 修改构造器 | 默认顺序调整为 `300`，确保下游模型调用完成后能读取捕获值，并让外层轨迹 Advisor 获取最终预览上下文。 |
| L40–69 | 修改 Advisor 链路 | 先执行下游调用，再读取实际发送体；展示前只调用 `SensitiveValueRedactor.redact`，不重新生成 JSON。 |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/advisor/ContextPreviewAdvisor.java`

---

### 链路 4：依赖装配

#### `ModelGatewayConfiguration.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L49–57 | 新增 Bean | 创建单例 `ActualModelRequestCapture`，作为发送层、Advisor 和业务网关之间的共享对象。 |
| L59–80 | 修改 ChatModel 装配 | 将共享捕获器注入 `ResponsesApiChatModel`。 |
| L87–96 | 修改 Advisor 装配 | `ContextPreviewAdvisor` 不再依赖请求工厂，改为读取共享捕获器。 |
| L111–132 | 修改网关装配 | 将同一捕获器注入 `SpringAiModelGateway`，统一成功与失败路径的数据来源。 |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/ModelGatewayConfiguration.java`

---

### 链路 5：模型网关成功、工具循环与失败路径

#### `SpringAiModelGateway.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L39 | 新增字段 | 保存与真实发送层共享的请求体捕获器。 |
| L41–93 | 修改构造器 | 增加共享捕获器注入入口；旧构造器仍可用于无真实 HTTP 发送层的测试或兼容调用。 |
| L95–169 | 修改主流程 | 调用开始前清理旧值；工具调用循环中由每次真实发送更新捕获体；成功和异常均读取实际捕获值；`finally` 再次清理。 |
| L210–225 | 新增实际预览转换 | `actualRequestPreview()` 只对捕获字符串脱敏；发送层未执行时返回空正文。 |
| L227–236 | 修改预览选择 | 优先读取 Advisor 返回的实际预览；缺失时直接读取捕获器，不调用请求工厂构造 fallback。 |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/SpringAiModelGateway.java`

---

### 链路 6：旧直连网关兼容路径

#### `OpenAiModelGateway.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L39–40 | 修改预览来源 | 对已经生成且随后传给 HTTP Client 的同一个 `requestBody` 直接脱敏，不再调用 `createPreviewBody()` 二次序列化。 |
| L42–50 | 保持展示契约 | 请求预览继续复用既有 DTO，但 `rawJson` 的来源已变为实际发送体。 |
| L51–52 | 发送一致性 | `client.createResponse(...)` 接收的对象与 L40 脱敏前的字符串为同一 `requestBody`。 |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiModelGateway.java`

---

### 链路 7：回归测试与测试资源

#### `SpringAiModelGatewayTest.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L47–49 | 新增资源加载 | 从版本化 JSON fixture 加载模拟发送层捕获体，避免在 Java 测试中硬编码可复用 JSON。 |
| L53–68 | 修改基础场景 | 验证没有真实发送层时请求预览为空，不伪造一份重新序列化的请求。 |
| L94–116 | 新增 Advisor 场景 | 验证请求预览与发送层捕获的原始请求体完全相等，同时保留 Agent 调用轨迹。 |
| L118–158 | 修改工具调用场景 | 验证两轮工具调用后，预览严格等于 HTTP Client 收到的最后一份 body，并包含 `flavor_suggestion.description`。 |
| L184–221 | 修改测试模型 | 测试模型可选地模拟发送层记录实际请求体。 |
| L244–261 | 修改 HTTP Client 桩 | 保存每次收到的请求体，供“最后发送体完全一致”断言使用。 |

> **路径**：`backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/SpringAiModelGatewayTest.java`

#### `captured-request-v1.json` — 新增

| 行号 | 变动 | 说明 |
|---:|---|---|
| L1–3 | 新增 | Advisor 捕获测试使用的版本化请求体 fixture。 |

> **路径**：`backend/src/test/resources/prompts/fixtures/model-requests/captured-request-v1.json`

---

## 三、已删除逻辑

| 旧逻辑 | 原位置 | 删除后的行为 |
|---|---|---|
| Advisor 在模型调用前调用 `OpenAiResponsesRequestFactory.createPreviewBody(modelName, prompt)` 重新生成预览 | `ContextPreviewAdvisor.java` 原 `adviseCall()` | Advisor 在模型调用完成后读取发送层捕获的同一字符串，只执行脱敏。 |
| Spring AI 网关预先生成 `fallbackRequestPreview` | `SpringAiModelGateway.java` 原 `complete()` | 不再构造推测请求；成功、失败都读取实际捕获体，未发送则为空。 |
| Spring AI 网关的 `fallbackRequestPreview(prompt)` | `SpringAiModelGateway.java` 原私有方法 | 由 `actualRequestPreview()` 替代，完全不依赖请求工厂序列化。 |
| 旧直连网关调用 `createPreviewBody(modelName, contextPackage)` | `OpenAiModelGateway.java` 原 L40 | 直接复用 L39 已生成的 `requestBody` 并脱敏。 |
| 请求预览仅反映初始 Prompt | 原工具调用循环外的预览生成逻辑 | 捕获器在每次真实发送前覆盖值，最终展示最后一次实际请求。 |

---

## 四、架构决策记录

### ADR-1：实际发送体是请求预览的唯一事实来源

- **决策**：工作台 `requestPreview.rawJson` 必须来自真正传给 HTTP Client 的字符串。
- **理由**：只有发送边界掌握最终模型名、完整消息历史、工具定义、工具 `description`、工具选择和结构化输出契约。上游重建无法证明与网络请求一致。
- **边界**：展示层可以做敏感值脱敏，但不得解析后重排字段、格式化或重新序列化。

### ADR-2：在 HTTP Client 调用前捕获，而不是在请求工厂中旁路复制

- **决策**：`ResponsesApiChatModel.call()` 在生成最终 `requestBody` 后、调用 `createResponse()` 前记录该字符串。
- **理由**：这个位置是应用内最接近网络发送的稳定边界，可以保证捕获值与 Client 入参是同一个对象引用对应的内容，同时覆盖工具调用后的后续请求。
- **边界**：如果未来 HTTP Client 内部还会修改 body，应把捕获点继续下沉到最终 HTTP request body publisher。

### ADR-3：同步调用使用 `ThreadLocal` 隔离，并显式清理

- **决策**：当前同步 `ChatClient.call()` 链路使用 `ThreadLocal` 保存最近一次请求体。
- **理由**：Advisor、ChatModel 和 Gateway 位于同一同步调用线程，使用线程隔离可以避免为预览修改业务 DTO 或 Spring AI Prompt 契约。
- **边界**：调用开始前和 `finally` 都清理；未来切换到异步、响应式或跨线程执行时，必须改用显式调用上下文传播，不能继续假设 `ThreadLocal` 有效。

### ADR-4：预览缺失时保持为空，不制造“看起来真实”的内容

- **决策**：没有经过真实发送层的通用 `ChatModel` 或发送前失败场景，`rawJson` 返回空字符串。
- **理由**：一份重新生成的预览虽然便于观察，但会混淆“计划发送”与“实际发送”，也可能再次掩盖工具定义缺失。
- **边界**：如果以后需要展示计划请求，应增加独立字段并明确标记为“计划请求”，不得复用“实际发送”预览。

### ADR-5：工具调用循环展示最后一次实际发送体

- **决策**：多轮工具调用时，`requestPreview` 展示 HTTP Client 收到的最后一份请求体。
- **理由**：最后一份请求包含工具调用结果和完整对话历史，最能解释模型最终回答；测试同时保留所有 Client body，用于证明预览与最后一次发送严格相等。
- **边界**：如果后续需要逐轮审计，应把每次捕获扩展为有序列表，而不是重新构造任一轮请求。
