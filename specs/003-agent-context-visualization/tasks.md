# 任务：Agent 上下文与记忆输入可视化

**输入**：来自 `/specs/003-agent-context-visualization/` 的设计文档

**前置条件**：plan.md（必需）、spec.md（用户故事必需）、research.md、data-model.md、contracts/

**语言要求**：除代码标识符、命令、API 字段和第三方专有名词外，任务说明必须使用简体中文。

**测试要求**：Agent 状态、候选记忆、模型输入预览、模拟输出、事实边界检查、清空当前会话和用户可见流程必须包含自动测试或明确的人工验证任务。真实模型、长期数据库和小红书外部能力不属于本切片验证范围。

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

**目标**：确认 003 基于已有真实 Web 工作台继续实现，不重新初始化工程，不引入真实模型、长期数据库或小红书依赖。

- [X] T001 检查 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java`、`backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/WebWorkbenchService.java`、`frontend/src/services/workbenchTypes.ts` 的现有工作台快照字段并记录需要扩展的 `agentState` 字段
- [X] T002 [P] 检查 `frontend/src/components/layout/WorkbenchLayout.tsx` 和 `frontend/src/app/App.tsx`，确认中间 `main-workspace` 只承载对话流、输入框和必要错误提示
- [X] T003 [P] 检查 `frontend/src/app/styles.css` 中右侧 `right-inspector`、`record-panel`、`agent-trace` 样式，确认状态卡片放置区域在当前记录下方
- [X] T004 [P] 检查 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchApiContractTest.java` 和 `frontend/src/app/App.test.tsx` 的现有覆盖范围，标记 003 需要新增的契约断言

---

## Phase 2：基础能力（阻塞所有用户故事）

**目标**：完成 Agent 状态快照、发送状态、能力边界和前端类型/组件底座。未完成前不得开始用户故事实现。

- [X] T005 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java` 新增 `AgentStateSnapshot`、`AgentStatusCard`、`ContextItem`、`ConfirmedFact`、`PendingAssociation`、`CandidateMemory`、`ContextPreview`、`ContextPreviewSection`、`ContextPreviewItem`、`ModelOutputSnapshot`、`FactBoundaryCheckResult`、`CapabilityBoundary`、`SessionControlAction` DTO
- [X] T006 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/domain/AgentStateModels.java` 定义 `sendStatus`、`riskLevel`、`basisType`、`recommendedAction`、`agentCardType` 等枚举或值对象，覆盖契约中的允许值
- [X] T007 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 创建 Agent 状态组装服务骨架，接收工作台会话快照并输出空 `AgentStateSnapshot`
- [X] T008 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/WebWorkbenchService.java` 注入并调用 `AgentStateAssembler`，保证 `emptySnapshot()`、`snapshot()`、`recoverableSnapshot()` 都返回 `agentState`
- [X] T009 [P] 在 `frontend/src/services/workbenchTypes.ts` 新增与后端契约一致的 `AgentStateSnapshot`、`AgentStatusCard`、`ContextPreview`、`ModelOutputSnapshot`、`FactBoundaryCheckResult`、`CapabilityBoundary`、`SessionControlAction` 类型
- [X] T010 [P] 在 `frontend/src/features/agent-trace/AgentStateCards.tsx` 创建状态卡片组件骨架，支持按 `type`、`sendStatus`、`riskLevel` 展示标签和空状态
- [X] T011 [P] 在 `frontend/src/features/agent-trace/AgentStateCards.test.tsx` 添加组件测试，覆盖空状态、来源标签、发送状态标签和高风险标签渲染
- [X] T012 在 `frontend/src/app/App.tsx` 将 `snapshot.agentState` 传入右侧状态区，并保留现有 `AgentTracePanel` 基础流程状态直到状态卡片接管展示
- [X] T013 在 `frontend/src/app/styles.css` 增加 `agent-state-card`、`send-status`、`risk-level`、`capability-boundary` 样式，确保卡片文字可扫描且不占用中间对话区

**检查点**：工作台快照可返回空 `agentState`，前端可渲染空状态卡片区，所有用户故事可在同一状态模型上继续实现。

---

## Phase 3：用户故事 1 - 分清 Agent 状态来源（优先级：P1）🎯 MVP

**目标**：用户能在当前记录下方看到当前会话上下文、已确认事实、待确认联想和候选记忆，并理解每项来源和是否进入模型输入。

**独立测试**：打开工作台、输入首轮咖啡体验后，右侧状态卡片显示会话上下文、已确认事实、待确认联想和候选记忆；中间主工作区不出现状态卡片或候选记忆卡片。

### 测试与验证

- [X] T014 [P] [US1] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchAgentStateContractTest.java` 添加契约测试，断言首轮消息后 `agentState.contextItems`、`confirmedFacts`、`pendingAssociations`、`candidateMemories` 存在且包含来源和发送状态
- [X] T015 [P] [US1] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssemblerTest.java` 添加单元测试，覆盖“柑橘”扩展为待确认联想且不进入已确认事实
- [X] T016 [P] [US1] 在 `frontend/src/app/App.test.tsx` 增加流程断言，确认右侧出现 Agent 状态卡片且中间 `main-workspace` 不包含候选记忆或上下文预览卡片
- [X] T017 [P] [US1] 在 `frontend/src/features/agent-trace/AgentStateCards.test.tsx` 增加候选记忆卡片测试，断言“本地示例，不是真实长期数据库召回”和 `PAGE_ONLY` 可见

### 实现

- [X] T018 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 实现 `contextItems` 组装，把用户消息和助手回复映射为带 `sourceType`、`confirmationStatus`、`sendStatus` 的上下文项
- [X] T019 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 实现 `confirmedFacts` 映射，只从 `workspace.confirmedFacts()` 生成 `WILL_SEND` 或用户陈述状态
- [X] T020 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 实现 `pendingAssociations`，将“甜橙”“青柠”“葡萄柚”等派生内容标为 `SEND_AFTER_CONFIRMATION` 或 `PAGE_ONLY`
- [X] T021 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 实现稳定 `candidateMemories` 示例和冲突状态占位，来源边界必须写明非真实长期数据库召回
- [X] T022 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 生成 `AgentStatusCard` 列表，覆盖 `SESSION_CONTEXT`、`CONFIRMED_FACT`、`PENDING_ASSOCIATION`、`CANDIDATE_MEMORY`
- [X] T023 [US1] 在 `frontend/src/features/agent-trace/AgentStateCards.tsx` 渲染当前会话上下文、已确认事实、待确认联想和候选记忆分区，所有卡片显示来源、发送状态和风险等级
- [X] T024 [US1] 在 `frontend/src/app/App.tsx` 将 `AgentStateCards` 放入右侧当前记录下方，确保 `ConversationThread` 和 `ConversationComposer` 仍是中间对话区的主要内容
- [X] T025 [US1] 在 `frontend/scripts/test.mjs` 增加静态检查，确认 `AgentStateCards` 只由右侧区域引用且中间对话区不渲染候选记忆卡片

**检查点**：US1 可独立演示：首轮输入后，用户能在 2 分钟内区分当前上下文、确认事实、待确认联想和候选记忆。

---

## Phase 4：用户故事 2 - 预览未来会发给模型的上下文（优先级：P1）

**目标**：用户能看到结构化上下文预览，清楚哪些内容未来会发给大模型，哪些只在页面观察，哪些待确认后才可能发送。

**独立测试**：在不配置真实模型凭证的情况下，首轮输入后右侧出现上下文预览，预览按来源分组并展示 `WILL_SEND`、`PAGE_ONLY`、`SEND_AFTER_CONFIRMATION`、`EXCLUDED`。

### 测试与验证

- [X] T026 [P] [US2] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentContextPreviewTest.java` 添加单元测试，断言上下文预览按已确认事实、待确认联想、候选记忆分组
- [X] T027 [P] [US2] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchAgentStateContractTest.java` 增加契约断言，确认未确认联想不出现在 `CONFIRMED_FACTS` 分组且真实模型边界文案存在
- [X] T028 [P] [US2] 在 `frontend/src/features/agent-trace/ContextPreviewPanel.test.tsx` 添加组件测试，覆盖发送、排除、待确认和仅页面观察四类状态

### 实现

- [X] T029 [US2] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 实现 `ContextPreview` 组装，按 `CURRENT_SESSION`、`CONFIRMED_FACTS`、`PENDING_ASSOCIATIONS`、`CANDIDATE_MEMORIES` 分组
- [X] T030 [US2] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 为上下文预览项计算 `willSendCount`、`excludedCount`、`exclusionReason` 和边界说明
- [X] T031 [US2] 在 `frontend/src/features/agent-trace/ContextPreviewPanel.tsx` 创建上下文预览组件，按分组展示内容、来源标签、发送状态和排除原因
- [X] T032 [US2] 在 `frontend/src/features/agent-trace/AgentStateCards.tsx` 集成 `ContextPreviewPanel`，确保上下文预览在右侧状态区展示且明确“当前未调用真实模型”
- [X] T033 [US2] 在 `frontend/src/app/App.test.tsx` 增加断言，确认上下文预览中待确认联想不会以已确认事实文案出现

**检查点**：US2 可独立演示：用户能看到未来模型输入预览，并区分发送、不发送和待确认后发送的内容。

---

## Phase 5：用户故事 3 - 查看模拟输出与事实边界检查（优先级：P2）

**目标**：用户能查看明确标记的模拟模型输出，并看到事实边界检查指出有用户依据、待确认联想、无依据或冲突表达。

**独立测试**：补充足够事实后，右侧展示模拟输出和事实边界检查；“甜橙爆汁感很明显”一类表达必须被标为待确认或越界，不得作为确认事实。

### 测试与验证

- [X] T034 [P] [US3] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/application/FactBoundaryCheckTest.java` 添加单元测试，覆盖 `USER_CONFIRMED`、`PENDING_ASSOCIATION`、`UNSUPPORTED`、`CONFLICT` 四类检查结果
- [X] T035 [P] [US3] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchAgentStateContractTest.java` 增加契约断言，确认草稿生成后 `modelOutput.outputType=SIMULATED` 且 `factBoundaryChecks` 非空
- [X] T036 [P] [US3] 在 `frontend/src/features/agent-trace/ModelOutputPanel.test.tsx` 添加组件测试，确认模拟输出标签、事实边界风险和建议动作可见

### 实现

- [X] T037 [US3] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/SimulatedModelOutputService.java` 创建模拟输出服务，基于当前已确认事实和待确认联想生成稳定样例
- [X] T038 [US3] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/FactBoundaryChecker.java` 创建事实边界检查服务，对模拟输出片段生成依据类型、风险等级、来源指向和建议动作
- [X] T039 [US3] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 集成 `SimulatedModelOutputService` 和 `FactBoundaryChecker`，仅在草稿或足够事实状态下返回模拟输出和检查结果
- [X] T040 [US3] 在 `frontend/src/features/agent-trace/ModelOutputPanel.tsx` 创建模拟输出与事实边界检查组件，明确展示“模拟输出，未调用真实模型”
- [X] T041 [US3] 在 `frontend/src/features/agent-trace/AgentStateCards.tsx` 集成 `ModelOutputPanel`，高风险事实混淆必须突出显示并提示确认前不进入最终记录
- [X] T042 [US3] 在 `frontend/src/app/App.test.tsx` 增加补充事实后的流程断言，确认模拟输出和事实边界检查位于右侧状态区且中间对话区不展示这些卡片

**检查点**：US3 可独立演示：用户能用稳定模拟输出验证事实边界检查规则，不需要真实模型 API Key。

---

## Phase 6：用户故事 5 - 新建记录并清空当前会话（优先级：P2）

**目标**：用户能主动结束当前会话边界，清空恢复出的旧草稿和旧上下文，并在刷新后保持空记录状态。

**独立测试**：已有会话和草稿时，取消清空不改变状态；确认清空后页面回到空状态，刷新不恢复旧会话。

### 测试与验证

- [X] T043 [P] [US5] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchSessionControlContractTest.java` 添加契约测试，覆盖 `POST /api/workbench/sessions/{sessionId}/clear` 确认清空后返回 `EMPTY` 快照
- [X] T044 [P] [US5] 在 `frontend/src/stores/localResumeStore.test.ts` 添加本地恢复状态测试，覆盖清空 `lastSessionId` 和记录 `clearedSessionIds`
- [X] T045 [P] [US5] 在 `frontend/src/app/App.test.tsx` 增加清空会话流程测试，覆盖取消确认、确认清空和刷新后不恢复旧草稿

### 实现

- [X] T046 [US5] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java` 新增 `ClearSessionRequest` DTO，包含 `confirmed` 字段
- [X] T047 [US5] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/domain/TastingSessionRepository.java` 增加删除或清空当前会话的方法声明，限定只影响指定 `sessionId`
- [X] T048 [US5] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/infrastructure/TastingSessionRepositoryAdapter.java` 实现指定会话删除或清空方法，不影响其他内存会话
- [X] T049 [US5] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/WebWorkbenchService.java` 实现 `clearSession(sessionId, confirmed)`，未确认时返回可恢复错误或拒绝清空，确认后返回空 `WorkbenchSnapshot`
- [X] T050 [US5] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java` 增加 `POST /api/workbench/sessions/{sessionId}/clear` 接口并复用 `ApiResponse`
- [X] T051 [US5] 在 `frontend/src/services/workbenchApi.ts` 增加 `clearSession(sessionId, { confirmed: true })` 方法
- [X] T052 [US5] 在 `frontend/src/stores/localResumeStore.ts` 增加 `clearSessionResume(sessionId)`，清除旧 `lastSessionId`、`draftInput` 并记录 `clearedSessionIds`
- [X] T053 [US5] 在 `frontend/src/components/feedback/ClearSessionConfirmDialog.tsx` 创建确认弹窗，说明将清空当前会话可见状态但不删除长期记忆或外部平台数据
- [X] T054 [US5] 在 `frontend/src/app/App.tsx` 添加“新建记录 / 清空当前会话”入口，取消时保持快照不变，确认后调用清空接口并更新本地恢复状态
- [X] T055 [US5] 在 `frontend/src/features/agent-trace/AgentStateCards.tsx` 为清空后的空 `agentState` 展示“当前没有可发送上下文”空状态

**检查点**：US5 可独立演示：旧会话和旧草稿可被用户主动清掉，刷新后不再恢复。

---

## Phase 7：用户故事 4 - 理解切片边界与不可用状态（优先级：P3）

**目标**：用户能明确知道当前未接真实模型、长期数据库和小红书，所有候选记忆、模型输出和工具状态都只是本地可视化链路。

**独立测试**：打开工作台即可看到能力边界；任何候选记忆、模型输出和工具状态都不会暗示真实外部调用已发生。

### 测试与验证

- [X] T056 [P] [US4] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchCapabilityBoundaryTest.java` 添加契约测试，断言 `capabilityBoundary.realModelConnected=false`、`longTermMemoryConnected=false`、`xiaohongshuConnected=false`
- [X] T057 [P] [US4] 在 `frontend/src/features/agent-trace/CapabilityBoundaryPanel.test.tsx` 添加组件测试，确认未接真实模型、未接长期数据库、未接小红书文案可见
- [X] T058 [P] [US4] 在 `frontend/src/app/App.test.tsx` 添加空状态断言，确认无内容分区不被隐藏且显示可理解边界说明

### 实现

- [X] T059 [US4] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 实现 `CapabilityBoundary`，统一声明真实模型、长期数据库和小红书未接入
- [X] T060 [US4] 在 `frontend/src/features/agent-trace/CapabilityBoundaryPanel.tsx` 创建能力边界组件，显示未接入状态和不执行外部动作说明
- [X] T061 [US4] 在 `frontend/src/features/agent-trace/AgentStateCards.tsx` 集成 `CapabilityBoundaryPanel`，候选记忆、模拟输出和工具状态分区都必须保留边界说明
- [X] T062 [US4] 在 `frontend/scripts/test.mjs` 增加静态检查，确认前端包含“未调用真实模型”“不是真实长期数据库召回”“未执行小红书动作”等边界文案

**检查点**：US4 可独立演示：用户不会误以为系统已经完成真实召回、真实推理或外部平台动作。

---

## Phase 8：收尾与横切事项

- [X] T063 [P] 更新 `docs/architecture/frontend-design-v0.1.md`，补充 003 状态卡片区、上下文预览、模拟输出和清空当前会话的最终交互说明
- [X] T064 [P] 更新 `docs/architecture/real-web-app-business-flow.md`，补充“页面动作 -> 工作台快照 -> AgentState 组装 -> 右侧状态卡片”的链路说明
- [X] T065 [P] 更新 `frontend/README.md`，补充 003 本地验收入口、清空当前会话和未接真实模型/数据库/小红书边界
- [X] T066 运行 `cd backend && ./mvnw test`，记录后端契约、Agent 状态和事实边界测试结果到 `specs/003-agent-context-visualization/quickstart.md`
- [X] T067 运行 `cd frontend && npm test -- --run && node scripts/test.mjs && npm run build`，记录前端流程、静态检查和构建结果到 `specs/003-agent-context-visualization/quickstart.md`
- [X] T068 使用浏览器打开 `http://127.0.0.1:5173/` 人工验证 quickstart 场景 1 到场景 6，并在 `specs/003-agent-context-visualization/quickstart.md` 记录通过、未验证内容和残余风险
- [X] T069 检查 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/`、`frontend/src/features/agent-trace/`、`frontend/src/services/workbenchApi.ts` 中没有真实 API Key、Cookie、Authorization Header 或 Session Token 输出
- [X] T070 清理 003 中未使用的占位代码和重复状态文案，确保 `frontend/src/app/App.tsx`、`frontend/src/features/agent-trace/AgentStateCards.tsx`、`backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentStateAssembler.java` 保持克制结构

## 依赖与执行顺序

- Phase 1 无依赖，用于确认现有工程和改动边界。
- Phase 2 依赖 Phase 1，且阻塞所有用户故事，因为 `agentState` DTO、组装服务和前端类型是共享底座。
- US1 和 US2 均为 P1：建议先完成 US1，再完成 US2；US2 依赖 US1 的上下文项、事实、联想和候选记忆基础数据。
- US3 依赖 US2 的上下文预览和发送状态，可在 US2 完成后独立实现模拟输出与事实边界检查。
- US5 依赖 Phase 2，可与 US3 并行推进，但最终需要与 US1/US2/US3 的状态清空语义集成验证。
- US4 依赖 Phase 2，可与 US3 或 US5 并行推进，但建议在主要状态分区完成后统一收口文案。
- Phase 8 依赖所有用户故事完成。

## 并行执行示例

### US1 并行

```text
T014 后端契约测试
T015 AgentStateAssembler 单元测试
T016 App 流程测试
T017 AgentStateCards 候选记忆组件测试
```

### US2 并行

```text
T026 后端上下文预览单元测试
T027 后端契约断言
T028 前端 ContextPreviewPanel 组件测试
```

### US3 并行

```text
T034 FactBoundaryCheck 单元测试
T035 后端契约断言
T036 前端 ModelOutputPanel 组件测试
```

### US5 并行

```text
T043 清空接口契约测试
T044 本地恢复状态测试
T045 App 清空流程测试
```

### US4 并行

```text
T056 能力边界契约测试
T057 CapabilityBoundaryPanel 组件测试
T058 App 空状态断言
```

## 实施策略

### MVP 优先

MVP 范围为 Phase 1、Phase 2、US1 和 US2。完成后用户已经可以看到右侧状态来源与未来模型输入预览，能区分当前上下文、已确认事实、待确认联想和候选记忆。

### 增量交付

1. 先扩展 `WorkbenchSnapshot` 和前端类型，让空 `agentState` 可稳定传递。
2. 交付 US1，完成状态来源可视化。
3. 交付 US2，完成上下文预览和发送状态。
4. 交付 US3，增加模拟输出和事实边界检查。
5. 交付 US5，解决刷新恢复旧草稿和旧上下文的问题。
6. 交付 US4，收口未接真实模型、长期数据库、小红书的能力边界说明。
7. 执行 Phase 8，完成文档、自动化和浏览器验收。

## 备注

- `[P]` 表示可并行任务。
- `[Story]` 必须能追溯到 spec.md 中的用户故事。
- 本任务列表不要求接入真实模型、长期数据库、pgvector 或小红书。
- 避免把候选记忆、待确认联想或模拟输出表述成用户已确认事实。
