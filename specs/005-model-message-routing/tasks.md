# 任务：模型驱动消息路由

**输入**：来自 `/specs/005-model-message-routing/` 的设计文档

**前置条件**：plan.md、spec.md、research.md、data-model.md、contracts/model-message-routing-contract.md、quickstart.md

**语言要求**：除代码标识符、命令、API 字段和第三方专有名词外，任务说明必须使用简体中文。

**测试要求**：本功能涉及 Agent 行为、prompt 约束、模型结构化输出和用户可见流程，必须包含后端自动测试、前端自动测试和 quickstart 人工验证任务。

**组织方式**：任务按用户故事分组，确保每个故事都能独立实现、独立测试、独立演示。

## 格式：`[ID] [P?] [Story] 描述`

- **[P]**：可并行执行，前提是不同文件且没有依赖关系。
- **[Story]**：任务所属用户故事，例如 US1、US2、US3。
- 描述中必须包含准确文件路径。

## 路径约定

- **后端**：`backend/src/main/java/`、`backend/src/test/java/`、`backend/src/main/resources/`
- **前端**：`frontend/src/`、`frontend/src/features/`、`frontend/src/services/`、`frontend/src/stores/`
- **文档**：`specs/005-model-message-routing/`、`docs/research/`

## Phase 1：初始化（共享基础设施）

**目标**：准备模型消息路由需要的资源目录、测试夹具和风格提示词资源化入口。

- [X] T001 在 `backend/src/main/resources/prompts/style/` 创建模型可加载的风格提示词资源目录，并保留 `RESTRAINED`、`EXAGGERATED`、`SHARP_REVIEW` 三种风格的版本化文件命名约定
- [X] T002 [P] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/fixtures/` 新增模型响应测试夹具目录，用于保存 POST、CONVERSATION 和异常结构样例
- [X] T003 [P] 在 `frontend/src/features/conversation/` 确认或新增模型路由前端测试夹具目录，用于覆盖聊天框只展示 `talk` 的交互
- [X] T004 在 `docs/research/xhs-style-prompts/README.md` 确认风格目录与代码枚举 `RESTRAINED`、`EXAGGERATED`、`SHARP_REVIEW` 的英文命名映射

---

## Phase 2：基础能力（阻塞所有用户故事）

**目标**：建立 POST/CONVERSATION 共享契约、schema、DTO 和提示词底座。未完成前不得开始用户故事实现。

- [X] T005 [P] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ModelMessageType.java` 新增 `POST`、`CONVERSATION` 枚举
- [X] T006 [P] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ConversationModelMessage.java` 新增继续对话消息模型，包含 `questions`、`pendingConfirmations`、`warnings`
- [X] T007 [P] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/PostModelMessage.java` 新增发布草稿消息模型，包含 `variants`、`warnings`
- [X] T008 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ModelAgentMessage.java` 新增顶层模型消息模型，包含 `messageType`、`talk`、`post`、`conversation`、`warnings` 和校验方法
- [X] T009 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ModelGateway.java` 扩展 `ModelResult`，加入 `messageType`、`talk`、`post`、`conversation`，并保留兼容现有 `variants` 映射
- [X] T010 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java` 扩展 `ModelOutputSnapshot`，加入 `messageType`、`talk`、`post`、`conversation`
- [X] T011 在 `frontend/src/services/workbenchTypes.ts` 扩展 `ModelOutputSnapshot`、`PostModelMessage`、`ConversationModelMessage`、`ModelMessageType` 类型定义
- [X] T012 在 `backend/src/main/resources/prompts/agent/openai-responses-copy-v1.md` 重写模型路由 prompt，明确 `messageType`、`talk`、POST、CONVERSATION、事实边界和聊天框展示规则
- [X] T013 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesRequestFactory.java` 将 Responses API JSON schema 改为顶层 `messageType + talk + post + conversation` 结构
- [X] T014 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesRequestFactoryTest.java` 更新请求工厂测试，断言 schema 包含 `messageType`、`talk`、`post`、`conversation` 且不再要求顶层只有 `variants`

**检查点**：共享契约、schema 和类型定义完成，US1/US2 可以分别实现。

---

## Phase 3：用户故事 1 - 信息不足时继续追问（优先级：P1）🎯 MVP

**目标**：当输入信息不足或不是咖啡记录时，模型返回 CONVERSATION，前端聊天框只展示 `talk`，系统不生成草稿。

**独立测试**：输入“今天喝了一杯咖啡”或“我喝了杯可乐”后，工作台保持等待用户补充，`draftTabs` 为空，聊天气泡展示 `modelOutput.talk`。

### 测试与验证

- [X] T015 [P] [US1] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesParserTest.java` 添加 CONVERSATION 解析测试，覆盖 `talk` 非空、`post=null`、`questions` 存在
- [X] T016 [P] [US1] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/application/WorkbenchFactBoundaryTest.java` 添加 CONVERSATION 不生成草稿且不伪造咖啡事实的行为测试
- [X] T017 [P] [US1] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchAgentStateContractTest.java` 添加契约测试，断言 `modelOutput.messageType=CONVERSATION`、`talk` 返回、`draftTabs` 为空
- [X] T018 [P] [US1] 在 `frontend/src/features/conversation/ConversationThread.test.tsx` 添加聊天框测试，断言 CONVERSATION 只展示 `talk` 且不展示结构化 JSON 或草稿正文
- [X] T019 [P] [US1] 在 `frontend/src/features/agent-trace/ModelOutputPanel.test.tsx` 添加模型输出面板测试，断言 CONVERSATION 展示路由状态和风险提醒但不渲染文案变体卡片

### 实现

- [X] T020 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesParser.java` 将 `parseVariants` 升级为解析 `ModelAgentMessage`，支持 CONVERSATION 结构和 `talk` 校验
- [X] T021 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiModelGateway.java` 将真实模型成功结果映射为包含 `messageType` 和 `talk` 的 `ModelResult`
- [X] T022 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 将 CONVERSATION 模型消息写入 `ModelOutputSnapshot` 并保留请求/响应预览
- [X] T023 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/WebWorkbenchService.java` 确保 `messageType=CONVERSATION` 时 `modelOutputDraftTabs` 返回空列表且状态保持 `WAITING_FOR_FACTS`
- [X] T024 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/FactBoundaryChecker.java` 对 CONVERSATION 的待确认项和 warnings 进行事实边界检查，不把待确认项作为用户事实
- [X] T025 [US1] 在 `frontend/src/features/conversation/ConversationThread.tsx` 改为优先使用 `modelOutput.talk` 生成助手气泡，并在 CONVERSATION 时不附加 `drafts`
- [X] T026 [US1] 在 `frontend/src/features/agent-trace/ModelOutputPanel.tsx` 支持 CONVERSATION 状态展示，显示 `talk`、questions、warnings 和事实边界结果

**检查点**：用户故事 1 可以独立运行、独立测试、独立演示，是本功能 MVP。

---

## Phase 4：用户故事 2 - 信息完备时生成发布草稿（优先级：P1）

**目标**：当上下文足够或用户明确要求生成时，模型返回 POST，聊天框只展示 `talk`，草稿区域展示三版文案。

**独立测试**：输入完整咖啡记录并明确“给我生成文案吧”后，工作台进入 `DRAFTS_READY`，草稿区包含三版，聊天气泡只显示 `talk`。

### 测试与验证

- [X] T027 [P] [US2] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesParserTest.java` 添加 POST 解析测试，覆盖三版风格完整、`talk` 非空、`post.variants` 映射
- [X] T028 [P] [US2] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesParserTest.java` 添加异常测试，断言 POST 缺少 `RESTRAINED`、`EXAGGERATED` 或 `SHARP_REVIEW` 时返回 `MODEL_FORMAT_INVALID`
- [X] T029 [P] [US2] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchApiContractTest.java` 添加 POST 快照契约测试，断言 `status=DRAFTS_READY`、`draftTabs` 三版齐全、`modelOutput.talk` 存在
- [X] T030 [P] [US2] 在 `frontend/src/features/conversation/ConversationThread.test.tsx` 添加 POST 聊天气泡测试，断言聊天框只展示 `talk` 不展示三版正文
- [X] T031 [P] [US2] 在 `frontend/src/features/conversation/DraftTabs.tsx` 对应测试文件 `frontend/src/features/conversation/ConversationWorkbenchFlow.test.tsx` 添加 POST 草稿展示测试，断言草稿区展示三版文案和标签

### 实现

- [X] T032 [US2] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesParser.java` 实现 POST 结构解析、三版风格去重校验和异常映射
- [X] T033 [US2] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/CopyVariant.java` 调整 `validateCompleteSet`，确保能发现缺失、重复和空标题正文
- [X] T034 [US2] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/WebWorkbenchService.java` 仅在 `messageType=POST` 时将 `post.variants` 映射为 `DraftTab`
- [X] T035 [US2] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 将 POST 消息的 `talk`、`post`、`variants`、warnings 写入 `ModelOutputSnapshot`
- [X] T036 [US2] 在 `frontend/src/features/conversation/ConversationThread.tsx` 确保 POST 助手气泡只显示 `talk`，不在气泡内嵌入三版草稿
- [X] T037 [US2] 在 `frontend/src/features/conversation/DraftTabs.tsx` 确保 POST 草稿区按 `RESTRAINED`、`EXAGGERATED`、`SHARP_REVIEW` 展示三版内容
- [X] T038 [US2] 在 `frontend/src/features/agent-trace/ModelOutputPanel.tsx` 展示 POST 的 `messageType`、`talk`、三版变体和事实边界字段

**检查点**：用户故事 1 和 2 均可独立运行；系统能从继续追问进入草稿生成。

---

## Phase 5：用户故事 3 - 使用统一消息模型驱动后续链路（优先级：P2）

**目标**：让后续 Agent 能力沿用“结构化意图 + 后端校验 + 业务路由”的模型驱动模式，并让 prompt 动态组合可追踪。

**独立测试**：模型状态区能展示决策类型和 `talk`，异常结构被阻断，prompt 组合包含风格提示词正文而不是路径。

### 测试与验证

- [X] T039 [P] [US3] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesParserTest.java` 添加通用路由异常测试，覆盖缺少 `talk`、非法 `messageType`、CONVERSATION 携带 post 草稿
- [X] T040 [P] [US3] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesRequestFactoryTest.java` 添加 prompt 动态组合测试，断言请求包含风格提示词正文且不只包含文件路径
- [X] T041 [P] [US3] 在 `frontend/src/features/agent-trace/AgentStateCards.test.tsx` 添加模型路由状态测试，断言状态卡能区分 CONVERSATION 和 POST
- [X] T042 [P] [US3] 在 `frontend/src/app/App.test.tsx` 添加端到端工作台流程测试，覆盖 CONVERSATION 到 POST 的状态切换和 `talk` 展示

### 实现

- [X] T043 [P] [US3] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/prompt/PromptBundle.java` 新增提示词组合包模型，记录基础模板、路由规则、风格模板版本和动态约束摘要
- [X] T044 [US3] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/prompt/PromptComposer.java` 新增 prompt 组合器，加载基础路由模板和三种风格提示词正文
- [X] T045 [US3] 在 `backend/src/main/resources/prompts/style/restrained-style-v1.md` 沉淀 `docs/research/xhs-style-prompts/restrained/restrained-style-prompt.md` 的运行时风格提示词内容
- [X] T046 [US3] 在 `backend/src/main/resources/prompts/style/exaggerated-style-v1.md` 沉淀 `docs/research/xhs-style-prompts/exaggerated/exaggerated-style-prompt.md` 的运行时风格提示词内容
- [X] T047 [US3] 在 `backend/src/main/resources/prompts/style/sharp-review-style-v1.md` 沉淀 `docs/research/xhs-style-prompts/sharp-review/sharp-review-style-prompt.md` 的运行时风格提示词内容
- [X] T048 [US3] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesRequestFactory.java` 改为通过 `PromptComposer` 生成最终 `instructions`，并在请求预览中保留模板版本摘要
- [X] T049 [US3] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/ModelGatewayConfiguration.java` 注册 `PromptComposer` 和风格提示词资源路径配置
- [X] T050 [US3] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 将 `messageType`、`talk`、模板版本摘要写入模型状态卡和上下文预览
- [X] T051 [US3] 在 `frontend/src/features/agent-trace/AgentStateCards.tsx` 展示模型路由状态摘要，让用户能看到本轮是 CONVERSATION 还是 POST

**检查点**：所有目标用户故事均可独立运行，后续模型驱动能力可复用同一契约。

---

## Phase N：收尾与横切事项

- [X] T052 [P] 更新 `specs/005-model-message-routing/quickstart.md`，补充实现后的真实验证结果、未验证项和残余风险
- [X] T053 [P] 更新 `docs/architecture/frontend-design-v0.1.md`，把旧的“聊天气泡展示三版正文”调整为“聊天气泡只展示 talk，草稿区展示正文”
- [X] T054 [P] 更新 `docs/architecture/backend-design-v0.1.md`，记录模型驱动消息路由、POST/CONVERSATION 两套消息和 prompt 动态组合边界
- [X] T055 在 `backend/src/main/resources/prompts/agent/openai-responses-copy-v1.md` 执行硬编码和路径残留检查，确认不再引用 docs 相对路径作为模型可读内容
- [X] T056 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/support/TestRunner.java` 纳入新增后端行为测试入口
- [X] T057 在 `backend/` 运行 `./mvnw test`，验证后端模型解析、工作台契约和事实边界测试全部通过
- [X] T058 在 `frontend/` 运行 `npm test`，验证聊天框、草稿区和 Agent 状态前端测试全部通过
- [X] T059 按 `specs/005-model-message-routing/quickstart.md` 执行人工验证，覆盖信息不足、非咖啡输入、信息完备、异常模型返回和 prompt 模板检查
- [X] T060 在 `backend/src/main/resources/prompts/`、`backend/src/main/java/`、`frontend/src/` 执行敏感信息与成段 prompt 硬编码扫描，确认无 API Key、Authorization、Cookie、Session Token 或散落系统提示词

## 依赖与执行顺序

- Phase 1 无依赖。
- Phase 2 依赖 Phase 1，且阻塞所有用户故事。
- US1 和 US2 都依赖 Phase 2；推荐先交付 US1 作为 MVP，再交付 US2。
- US3 依赖 US1/US2 的稳定消息契约，但其中风格提示词资源化任务 T045-T047 可在 US2 实现期间并行准备。
- 收尾阶段依赖所有用户故事完成。

## 并行执行示例

### US1 并行

```text
T015、T016、T017、T018、T019 可并行编写测试。
T025 与 T026 可在 T020-T024 后并行实现前端展示。
```

### US2 并行

```text
T027、T028、T029、T030、T031 可并行编写测试。
T036、T037、T038 可在后端 POST 映射稳定后并行实现。
```

### US3 并行

```text
T039、T040、T041、T042 可并行编写验证。
T045、T046、T047 可并行沉淀三种风格提示词资源。
```

## 实施策略

### MVP 优先

先完成 Phase 1、Phase 2 和 US1。达到 MVP 后，系统能在信息不足时返回 CONVERSATION，聊天框只展示 `talk`，并且不会生成伪造草稿。

### 增量交付

1. 完成 US1：信息不足继续追问，不生成草稿。
2. 完成 US2：信息完备进入 POST，生成三版草稿。
3. 完成 US3：统一模型驱动模式，动态组合 prompt，并为后续工具、记忆和发布链路打基础。
4. 完成收尾：运行自动测试、quickstart 和敏感信息扫描。

### 验证要求

- 每个用户故事必须先完成对应测试或验证任务，再实现核心代码。
- 每个检查点都必须能独立演示，不得把“整体能跑”当作单故事通过。
- 任一模型格式异常必须返回可恢复错误，不得进入草稿或发布链路。
