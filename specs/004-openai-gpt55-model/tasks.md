# 任务：OpenAI GPT-5.5 真实模型接入

**输入**：来自 `/specs/004-openai-gpt55-model/` 的设计文档

**前置条件**：plan.md（必需）、spec.md（用户故事必需）、research.md、data-model.md、contracts/

**语言要求**：除代码标识符、命令、API 字段和第三方专有名词外，任务说明必须使用简体中文。

**测试要求**：Agent 行为、模型网关、上下文预览、事实边界、错误恢复、敏感信息过滤和用户可见流程必须包含自动测试或明确的人工验证任务。真实外部模型调用可通过人工 quickstart 验证；自动测试必须使用 fake/stub，不能依赖真实 API Key。

**组织方式**：任务按用户故事分组，确保每个故事都能独立实现、独立测试、独立演示。

## 格式：`[ID] [P?] [Story] 描述`

- **[P]**：可并行执行，前提是不同文件且没有依赖关系。
- **[Story]**：任务所属用户故事，例如 US1、US2、US3。
- 描述中必须包含准确文件路径。

## 路径约定

- **后端**：`backend/src/main/java/`、`backend/src/test/java/`、`backend/src/main/resources/`
- **前端**：`frontend/src/`、`frontend/src/features/`、`frontend/src/components/`、`frontend/src/services/`、`frontend/src/stores/`
- **文档**：`docs/prd/`、`docs/architecture/`、`docs/research/`

## Phase 1：初始化（共享基础设施）

**目标**：确认 004 基于现有工作台和 003 Agent 状态可视化继续实现，不重新初始化工程，不引入真实长期记忆数据库、小红书发布或复杂 Agent 框架。

- [X] T001 检查 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ModelGateway.java`、`backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/SpringAiModelGateway.java` 和 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/WebWorkbenchService.java` 的现有模型边界并记录需要扩展的请求/响应字段
- [X] T002 [P] 检查 `backend/src/main/resources/application.yml` 和 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/config/ModelProperties.java`，确认只能写入非敏感配置且密钥必须来自环境变量
- [X] T003 [P] 检查 `frontend/src/features/agent-trace/ContextPreviewPanel.tsx`、`frontend/src/features/agent-trace/ModelOutputPanel.tsx` 和 `frontend/src/services/workbenchTypes.ts`，确认 004 可复用的右侧状态组件入口
- [X] T004 [P] 检查 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchAgentStateContractTest.java`、`frontend/src/features/agent-trace/ContextPreviewPanel.test.tsx` 和 `frontend/src/features/agent-trace/ModelOutputPanel.test.tsx` 的现有覆盖范围，标记 004 需要新增的测试断言
- [X] T005 [P] 在 `specs/004-openai-gpt55-model/quickstart.md` 确认 `OPENAI_BASE_URL=https://saturday.sankuai.com/v1`、`TEXT_MODEL=gpt-5.5` 和本地 env 加载方式不包含真实密钥值

---

## Phase 2：基础能力（阻塞所有用户故事）

**目标**：完成模型模式、模型上下文包、Responses API 客户端边界、脱敏工具、工作台 DTO 和前端类型底座。未完成前不得开始用户故事实现。

- [X] T006 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ModelGateway.java` 扩展模型网关契约，支持 `mode`、`modelName`、三版文案、请求预览、响应预览和可恢复错误结果
- [X] T007 [P] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ModelMode.java` 新增 `offline-fake` 与 `openai-gpt55` 模式值对象和显示文案
- [X] T008 [P] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ModelContextPackage.java` 新增模型上下文包记录，覆盖当前会话、已确认事实、待确认联想、候选记忆边界、排除项和 prompt 约束
- [X] T009 [P] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/CopyVariant.java` 新增三版文案结构，覆盖 `RESTRAINED`、`EXAGGERATED`、`SHARP_REVIEW`、事实使用、模型推断和待确认表达
- [X] T010 [P] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ModelPreview.java` 新增 `ModelRequestPreview`、`ModelResponsePreview` 和 `SensitiveRedactionResult` 记录
- [X] T011 [P] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/RecoverableModelError.java` 新增 `MODEL_TIMEOUT`、`MODEL_AUTH_FAILED`、`MODEL_RATE_LIMITED`、`MODEL_FORMAT_INVALID`、`MODEL_SERVICE_UNAVAILABLE` 错误模型
- [X] T012 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/config/ModelProperties.java` 扩展非敏感配置字段，包含 `mode`、`textModel`、`baseUrl`、`timeoutSeconds`、`maxRetries`，默认保留 `offline-fake` 和 `gpt-5.5`
- [X] T013 在 `backend/src/main/resources/application.yml` 增加非敏感模型配置占位，使用环境变量读取 `OPENAI_BASE_URL`、`TEXT_MODEL`、模型模式和超时，禁止写入 `OPENAI_API_KEY` 值
- [X] T014 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesLlmClient.java` 创建 OpenAI-compatible Responses API 客户端骨架，封装 `POST {OPENAI_BASE_URL}/responses`、`model: gpt-5.5` 和环境变量密钥读取
- [X] T015 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiModelGateway.java` 创建真实模型网关骨架，将 `ModelContextPackage` 映射为 Responses API 请求并返回统一 `ModelGateway` 结果
- [X] T016 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OfflineFakeModelGateway.java` 创建离线模型网关骨架，复用现有模拟输出语义并返回同一三版文案结构
- [X] T017 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/ModelGatewayConfiguration.java` 创建模型网关注入配置，按模型模式选择 `OfflineFakeModelGateway` 或 `OpenAiModelGateway`
- [X] T018 [P] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/error/SensitiveValueRedactor.java` 新增敏感信息过滤工具，覆盖 API Key、Authorization、Cookie、Session Token 和 Bearer token
- [X] T019 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java` 扩展 `AgentStateSnapshot`、`ModelOutputSnapshot`、`ContextPreview` 和 `SubmitMessageRequest`，增加 `modelMode`、`variants`、`requestPreview`、`responsePreview`、`recoverableError`
- [X] T020 [P] 在 `frontend/src/services/workbenchTypes.ts` 扩展前端类型，覆盖 `ModelMode`、`CopyVariant`、`ModelRequestPreview`、`ModelResponsePreview`、`RecoverableModelError` 和真实模型输出状态
- [X] T021 [P] 在 `frontend/src/services/workbenchApi.ts` 扩展提交消息 payload，允许传入 `modelMode` 并继续对错误详情执行敏感信息过滤
- [X] T022 [P] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/support/FakeModelGateway.java` 更新 fake 模型网关，支持 004 的三版文案、请求预览、响应预览和错误注入
- [X] T023 [P] 在 `frontend/src/test/setup.ts` 增加 004 前端测试所需的模型模式和请求预览测试辅助数据

**检查点**：后端和前端都能编译 004 的共享模型状态；离线和真实模式可通过同一个 DTO/类型契约表达。

---

## Phase 3：用户故事 1 - 无密钥时继续本地创作（优先级：P1）🎯 MVP

**目标**：无 `OPENAI_API_KEY` 时，工作台继续使用 `offline-fake` 完成现有流程，并清楚显示未调用真实模型。

**独立测试**：不设置 `OPENAI_API_KEY` 启动后端，输入咖啡体验后右侧状态区显示离线模式、模拟输出、上下文预览和事实边界检查，且不暗示真实请求已发送。

### 测试与验证

- [X] T024 [P] [US1] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchOfflineModelModeContractTest.java` 添加契约测试，断言无 Key 时 `modelMode.mode=offline-fake` 且模型输出显示“模拟输出，未调用真实模型”
- [X] T025 [P] [US1] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/application/OfflineFakeModelGatewayTest.java` 添加单元测试，断言离线网关返回克制版、夸张版、锐评版结构化样例和离线来源标签
- [X] T026 [P] [US1] 在 `frontend/src/features/agent-trace/ModelOutputPanel.test.tsx` 添加组件测试，断言 `offline-fake` 输出显示“模拟输出，未调用真实模型”且不显示真实请求标签
- [X] T027 [P] [US1] 在 `frontend/src/app/App.test.tsx` 添加无 Key 工作台流程测试，断言输入后仍保留上下文预览、模型输出和事实边界检查

### 实现

- [X] T028 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OfflineFakeModelGateway.java` 实现离线三版文案生成，保持稳定样例并标记 `outputType=SIMULATED`
- [X] T029 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/ModelContextPackageAssembler.java` 创建上下文包组装服务，从现有会话、确认事实、待确认联想和候选记忆生成离线可用上下文
- [X] T030 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/WebWorkbenchService.java` 集成模型模式选择，无 Key 或默认配置时调用 `OfflineFakeModelGateway`
- [X] T031 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 将离线模型模式、离线输出和离线边界文案写入 `agentState`
- [X] T032 [US1] 在 `frontend/src/features/agent-trace/ModelOutputPanel.tsx` 渲染离线模式状态、三版离线文案和“未调用真实模型”边界说明
- [X] T033 [US1] 在 `frontend/src/features/agent-trace/ContextPreviewPanel.tsx` 保持离线模式上下文预览可见，并显示“当前未发送给真实模型”
- [X] T034 [US1] 在 `frontend/src/app/App.tsx` 确保无 Key 场景提交消息后不进入全局失败页，保留当前输入、会话和右侧状态卡片

**检查点**：US1 可独立演示：无 Key 时用户能在 2 分钟内完成一次咖啡记录到离线输出的完整流程。

---

## Phase 4：用户故事 2 - 启用真实 GPT-5.5 生成三版文案（优先级：P1）

**目标**：配置本地 Key 并启用 `openai-gpt55` 后，后端通过 `https://saturday.sankuai.com/v1` 的 Responses API 调用 `gpt-5.5`，返回克制版、夸张版和锐评版三版文案。

**独立测试**：使用 fake `OpenAiResponsesLlmClient` 自动验证真实模式成功路径；人工 quickstart 使用本地 env 验证真实服务可达时的端到端生成。

### 测试与验证

- [X] T035 [P] [US2] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesLlmClientTest.java` 添加客户端测试，断言请求路径为 `/responses`、模型为 `gpt-5.5`、base URL 为 `https://saturday.sankuai.com/v1`
- [X] T036 [P] [US2] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiModelGatewayTest.java` 添加模型网关测试，断言 fake 响应被解析为三版 `CopyVariant`
- [X] T037 [P] [US2] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchRealModelContractTest.java` 添加契约测试，断言 `openai-gpt55` 成功时快照包含 `REAL_MODEL`、`gpt-5.5` 和三版文案
- [X] T038 [P] [US2] 在 `frontend/src/features/agent-trace/ModelOutputPanel.test.tsx` 添加真实模型输出组件测试，断言“真实模型输出 / GPT-5.5”和三版文案标签可见
- [X] T039 [P] [US2] 在 `frontend/src/app/App.test.tsx` 添加真实模式工作台流程测试，使用 mock API 返回三版文案并断言来源状态正确

### 实现

- [X] T040 [US2] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesLlmClient.java` 实现 Java 21 `HttpClient` 请求、超时配置、JSON 请求体构造和 HTTP 状态读取
- [X] T041 [US2] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesRequestFactory.java` 创建请求体工厂，按契约生成 `instructions`、`input` 和 `text.format` 的 JSON 结构
- [X] T042 [US2] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesParser.java` 创建响应解析器，提取结构化三版文案并校验 `RESTRAINED`、`EXAGGERATED`、`SHARP_REVIEW` 齐全
- [X] T043 [US2] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiModelGateway.java` 实现真实模型调用链路，将 `OpenAiResponsesLlmClient`、请求工厂、响应解析器和脱敏预览串起来
- [X] T044 [US2] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/WebWorkbenchService.java` 接收 `SubmitMessageRequest.modelMode=openai-gpt55` 并把真实结果合并进工作台快照
- [X] T045 [US2] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 将真实模型状态、模型名、生成时间和三版文案写入 `agentState.modelOutput`
- [X] T046 [US2] 在 `frontend/src/app/App.tsx` 增加模型模式选择入口并传递给 `ConversationComposer`，支持 `offline-fake` 与 `openai-gpt55`
- [X] T047 [US2] 在 `frontend/src/features/agent-trace/ModelOutputPanel.tsx` 渲染真实模型三版文案、模型名称和生成状态，避免复用离线“未调用真实模型”文案
- [X] T048 [US2] 在 `frontend/src/services/workbenchApi.ts` 提交用户选择的 `modelMode`，并在切换模式后清理上一轮来源状态展示

**检查点**：US2 可独立演示：配置本地 env 并启用 `openai-gpt55` 后，输入一段咖啡体验能获得三版真实模型文案。

---

## Phase 5：用户故事 3 - 预览真实模型请求边界（优先级：P1）

**目标**：用户能在右侧上下文预览中用中文标签区分“将发送”“不会发送”“待确认后发送”“已发送给大模型”“大模型返回”，并以 JSON 风格代码框查看脱敏请求。

**独立测试**：真实模式生成后，前端显示脱敏 `requestPreview.rawJson` 和 `responsePreview.rawJson`；候选记忆和待确认联想的发送/排除状态可见。

### 测试与验证

- [X] T049 [P] [US3] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/application/ModelContextPackageAssemblerTest.java` 添加单元测试，断言确认事实进入将发送、待确认联想标记待确认、冲突候选记忆排除
- [X] T050 [P] [US3] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchRequestPreviewContractTest.java` 添加契约测试，断言真实模式快照包含“已发送给大模型”和“大模型返回”的脱敏预览
- [X] T051 [P] [US3] 在 `frontend/src/features/agent-trace/ContextPreviewPanel.test.tsx` 添加组件测试，断言中文发送标签、JSON 代码框和发送/返回分区分开展示
- [X] T052 [P] [US3] 在 `frontend/src/app/App.test.tsx` 添加流程测试，断言真实请求 JSON 中可见 `model: gpt-5.5` 且不包含凭证字段

### 实现

- [X] T053 [US3] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/ModelContextPackageAssembler.java` 实现会发送、不会发送、待确认后发送和排除原因的上下文分组
- [X] T054 [US3] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesRequestFactory.java` 生成可展示的脱敏请求 body JSON，不包含任何 HTTP header
- [X] T055 [US3] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiModelGateway.java` 保存 `ModelRequestPreview` 和 `ModelResponsePreview`，并在失败前尽量保留已构造的请求预览
- [X] T056 [US3] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java` 为 `ContextPreview` 增加 `requestPreview`、`responsePreview` 或等价字段
- [X] T057 [US3] 在 `frontend/src/services/workbenchTypes.ts` 增加请求预览和响应预览字段类型，确保 `rawJson` 可作为 JSON 风格代码框渲染
- [X] T058 [US3] 在 `frontend/src/features/agent-trace/ContextPreviewPanel.tsx` 渲染“将发送”“不会发送”“待确认后发送”“已发送给大模型”“大模型返回”中文标签和 JSON 代码框
- [X] T059 [US3] 在 `frontend/src/app/styles.css` 增加 JSON 预览代码框样式，确保长 JSON 可换行、可扫描且不挤压右侧状态区

**检查点**：US3 可独立演示：真实模式生成后，用户能在 1 分钟内指出哪些内容已发送、哪些未发送以及模型返回了什么。

---

## Phase 6：用户故事 4 - 保护事实边界与输出可信度（优先级：P1）

**目标**：真实模型三版文案必须区分用户确认事实、模型推断、待确认联想、候选记忆来源和无依据表达，不把未确认内容写成事实。

**独立测试**：使用只确认“柑橘感”的样例生成文案，事实边界检查将“甜橙”等表达标为模型推断或待确认联想，并指出来源或缺失依据。

### 测试与验证

- [X] T060 [P] [US4] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/application/RealModelFactBoundaryCheckTest.java` 添加单元测试，覆盖 `USER_CONFIRMED`、`MODEL_INFERENCE`、`PENDING_ASSOCIATION`、`UNSUPPORTED`、`CONFLICT`
- [X] T061 [P] [US4] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesParserTest.java` 添加解析测试，断言模型返回的 `factUsages`、`inferences`、`pendingConfirmations` 不会合并为确认事实
- [X] T062 [P] [US4] 在 `frontend/src/features/agent-trace/ModelOutputPanel.test.tsx` 添加事实边界组件测试，断言每版文案显示事实依据、模型推断和待确认联想
- [X] T063 [P] [US4] 在 `frontend/src/app/App.test.tsx` 添加真实模型事实边界流程测试，断言未确认风味不会显示为用户确认事实

### 实现

- [X] T064 [US4] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/FactBoundaryChecker.java` 扩展真实模型检查逻辑，支持三版文案的 `FactUsage`、模型推断和待确认表达
- [X] T065 [US4] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/CopyVariant.java` 增加事实使用、模型推断、待确认表达和警告字段的校验方法
- [X] T066 [US4] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesRequestFactory.java` 强化 prompt 约束，要求模型不要把未确认风味、产区、处理法或候选记忆写成用户确认事实
- [X] T067 [US4] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 将每版文案的事实边界检查结果映射到 `agentState.factBoundaryChecks`
- [X] T068 [US4] 在 `frontend/src/features/agent-trace/ModelOutputPanel.tsx` 分版展示 `factUsages`、`inferences`、`pendingConfirmations` 和风险标签
- [X] T069 [US4] 在 `frontend/src/features/agent-trace/AgentStateCards.tsx` 对真实模型高风险事实混淆显示醒目卡片，并提示确认前不进入最终记录

**检查点**：US4 可独立演示：真实或 fake 模型输出中的未确认表达不会被展示为用户确认事实。

---

## Phase 7：用户故事 5 - 模型失败后可恢复重试（优先级：P2）

**目标**：真实模型超时、鉴权失败、限流或格式异常时，页面展示可恢复错误，保留用户输入、当前会话和上下文预览，并支持重试或切换离线模式。

**独立测试**：通过 fake/stub 分别模拟四类失败，后端返回稳定错误码，前端显示对应建议动作且当前输入不丢失。

### 测试与验证

- [X] T070 [P] [US5] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesLlmClientErrorTest.java` 添加客户端错误映射测试，覆盖超时、401/403、429、5xx 和非 JSON 响应
- [X] T071 [P] [US5] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchModelErrorContractTest.java` 添加契约测试，断言错误包含 `recoverable=true`、建议动作和 `preservedSessionId`
- [X] T072 [P] [US5] 在 `frontend/src/features/conversation/WorkbenchErrorRecovery.test.tsx` 添加错误恢复测试，覆盖重试、切换离线模式和输入保留
- [X] T073 [P] [US5] 在 `frontend/src/components/feedback/RecoverableErrorBanner.test.tsx` 添加模型错误展示断言，覆盖 `MODEL_TIMEOUT`、`MODEL_AUTH_FAILED`、`MODEL_RATE_LIMITED`、`MODEL_FORMAT_INVALID`

### 实现

- [X] T074 [US5] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesLlmClient.java` 实现超时、鉴权失败、限流、格式异常和服务不可用的异常分类
- [X] T075 [US5] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiModelGateway.java` 将底层异常转换为 `RecoverableModelError`，并通过脱敏工具清理错误详情
- [X] T076 [US5] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/WebWorkbenchService.java` 捕获模型错误并返回保留当前会话的 recoverable snapshot
- [X] T077 [US5] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java` 确保模型错误沿用 `ApiResponse.failure` 或等价快照响应，且不丢失 `requestId`
- [X] T078 [US5] 在 `frontend/src/services/workbenchApi.ts` 映射模型错误 nextActions，保留用户输入并暴露重试/切换离线模式动作给上层
- [X] T079 [US5] 在 `frontend/src/components/feedback/RecoverableErrorBanner.tsx` 增加模型错误文案、重试按钮和切换 `offline-fake` 操作入口
- [X] T080 [US5] 在 `frontend/src/app/App.tsx` 实现真实模型失败后的重试和离线降级流程，保持当前会话、上下文预览和输入框内容

**检查点**：US5 可独立演示：四类模型失败都能恢复，用户无需重新输入咖啡记录。

---

## Phase 8：用户故事 6 - 密钥与敏感信息不泄露（优先级：P2）

**目标**：API Key、Authorization、Cookie、Session Token 和 Bearer token 不进入配置文件、日志、测试快照、前端页面、请求预览或错误消息。

**独立测试**：运行后端、前端和静态扫描测试，确认真实或伪造凭证不会出现在用户可见输出、日志样例和测试快照中。

### 测试与验证

- [X] T081 [P] [US6] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/shared/error/SensitiveValueRedactorTest.java` 添加脱敏单元测试，覆盖 API Key、Authorization、Cookie、Session Token 和 Bearer token
- [X] T082 [P] [US6] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/trace/application/TraceSecretRedactionTest.java` 扩展追踪脱敏测试，断言模型请求、响应和错误不会记录凭证
- [X] T083 [P] [US6] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchSecretLeakContractTest.java` 添加契约测试，断言工作台快照、错误详情、请求预览和响应预览不包含敏感模式
- [X] T084 [P] [US6] 在 `frontend/src/services/workbenchApi.test.ts` 添加前端错误脱敏测试，断言 API 错误详情中的敏感字符串被替换为 `[REDACTED]`
- [X] T085 [P] [US6] 在 `frontend/scripts/test.mjs` 增加静态检查，扫描 `frontend/src/` 中不得出现真实 Key、Authorization header、Cookie 或 Session Token 示例值

### 实现

- [X] T086 [US6] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/error/SensitiveValueRedactor.java` 实现统一脱敏函数，并提供面向 Map、String 和 JSON 字符串的入口
- [X] T087 [US6] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesLlmClient.java` 确保 Authorization header 只在发送请求时构造，不进入请求预览、异常 message 或 trace metadata
- [X] T088 [US6] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiModelGateway.java` 对 `requestPreview.rawJson`、`responsePreview.rawJson` 和错误详情执行 `SensitiveValueRedactor`
- [X] T089 [US6] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/trace/application/AgentTraceRecorder.java` 记录模型 trace 时只写脱敏摘要，不写原始 header 和密钥
- [X] T090 [US6] 在 `frontend/src/services/workbenchApi.ts` 扩展 `SENSITIVE_PATTERNS`，覆盖 API Key、Authorization、Cookie、Session Token、Bearer token 和本地 env 常见格式
- [X] T091 [US6] 在 `frontend/src/features/agent-trace/ContextPreviewPanel.tsx` 对 `rawJson` 渲染前执行前端兜底脱敏，并在发现敏感内容时显示“内容已脱敏”
- [X] T092 [US6] 在 `backend/src/main/resources/application.yml` 确认仅保留非敏感变量占位和安全说明，不新增 `OPENAI_API_KEY` 明文或默认值

**检查点**：US6 可独立演示：所有用户可见状态、错误、trace 和测试输出均不泄露可复用凭证。

---

## Phase 9：收尾与横切事项

- [X] T093 [P] 更新 `docs/architecture/real-web-app-business-flow.md`，补充“工作台提交 -> ModelContextPackage -> ModelGateway -> Responses API -> AgentState”链路说明
- [X] T094 [P] 更新 `docs/architecture/frontend-design-v0.1.md`，补充真实模型状态、JSON 请求预览、三版文案和错误恢复的右侧面板交互说明
- [X] T095 [P] 更新 `frontend/README.md`，补充 `offline-fake`、`openai-gpt55`、本地 env 加载方式和无 Key 降级验收说明
- [X] T096 在 `specs/004-openai-gpt55-model/quickstart.md` 记录后端 `cd backend && ./mvnw test` 的执行结果、未验证内容和残余风险
- [X] T097 在 `specs/004-openai-gpt55-model/quickstart.md` 记录前端 `cd frontend && npm test -- --run && node scripts/test.mjs && npm run build` 的执行结果、未验证内容和残余风险
- [X] T098 在 `specs/004-openai-gpt55-model/quickstart.md` 记录真实模型人工验收结果，若服务不可用则按未验证内容格式说明 fake/stub 已覆盖范围
- [X] T099 运行 `rg -n "sk-[A-Za-z0-9_-]{20,}|Authorization:\\s*Bearer|Cookie:|Session Token|OPENAI_API_KEY=" backend frontend specs docs` 并在 `specs/004-openai-gpt55-model/quickstart.md` 记录敏感信息扫描结果
- [X] T100 清理 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/`、`backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/`、`frontend/src/features/agent-trace/` 中未使用的 004 占位代码和重复文案
- [X] T101 检查 `specs/004-openai-gpt55-model/tasks.md` 中全部任务是否保持 checkbox、ID、可选 `[P]`、用户故事标签和准确文件路径格式

## 依赖与执行顺序

- Phase 1 无依赖，用于确认现有工程、配置和测试边界。
- Phase 2 依赖 Phase 1，且阻塞所有用户故事，因为模型模式、模型网关、DTO、前端类型和脱敏工具是共享底座。
- US1 是 MVP：依赖 Phase 2，可单独证明无 Key 降级和现有流程不回退。
- US2 依赖 Phase 2，并可在 US1 后接入真实模型；US2 与 US3/US4 共享真实输出结构。
- US3 依赖 Phase 2，建议在 US2 成功路径可用后完成真实请求/响应预览。
- US4 依赖 Phase 2 和三版文案结构，建议在 US2 后完成真实输出事实边界。
- US5 依赖 Phase 2，可与 US3/US4 并行推进，但需要与 US2 的真实客户端错误类型对齐。
- US6 依赖 Phase 2，可与 US2-US5 并行推进，但最终必须覆盖所有真实请求、响应、错误和 trace 路径。
- Phase 9 依赖所有用户故事完成。

## 并行执行示例

### US1 并行

```text
T024 后端离线契约测试
T025 离线模型网关测试
T026 前端模型输出组件测试
T027 App 无 Key 流程测试
```

### US2 并行

```text
T035 Responses API 客户端测试
T036 模型网关解析测试
T037 工作台真实模型契约测试
T038 前端真实输出组件测试
T039 App 真实模式流程测试
```

### US3 并行

```text
T049 上下文包组装测试
T050 请求预览契约测试
T051 ContextPreviewPanel 组件测试
T052 App 请求 JSON 流程测试
```

### US4 并行

```text
T060 真实模型事实边界测试
T061 响应解析事实字段测试
T062 模型输出事实边界组件测试
T063 App 事实边界流程测试
```

### US5 并行

```text
T070 客户端错误映射测试
T071 工作台错误契约测试
T072 错误恢复流程测试
T073 RecoverableErrorBanner 组件测试
```

### US6 并行

```text
T081 后端脱敏单元测试
T082 trace 脱敏测试
T083 工作台防泄露契约测试
T084 前端错误脱敏测试
T085 前端静态敏感信息检查
```

## 实施策略

### MVP 优先

先完成 Phase 1、Phase 2 和 US1，确保无 Key 和本地开发场景不回退。这个 MVP 不依赖真实模型服务，但必须使用 004 的统一模型模式、文案结构和状态展示。

### 真实模型垂直切片

在 MVP 后按 US2 -> US3 -> US4 推进：先打通 `openai-gpt55` 成功路径，再展示真实请求/响应 JSON，最后收紧事实边界检查。每一步都能通过 fake/stub 自动测试独立验证。

### 稳定性与安全收口

US5 和 US6 可以在 US2 开始后并行推进，但必须在进入人工真实模型验收前完成。最终 Phase 9 统一运行后端测试、前端测试、构建、quickstart 和敏感信息扫描。

## 备注

- `[P]` 表示可并行任务。
- `[Story]` 必须能追溯到 spec.md 中的用户故事。
- 自动测试不得依赖真实 `OPENAI_API_KEY`；真实服务调用只作为 quickstart 人工验收。
- 任何文档或日志只能出现变量名、脱敏值或非敏感配置，不得出现真实 API Key、Authorization、Cookie 或 Session Token。
