# 任务：真实 Web 应用接入

**输入**：来自 `/specs/002-real-web-app/` 的设计文档

**前置条件**：`plan.md`、`spec.md`、`research.md`、`data-model.md`、`contracts/web-app-contract.md`、`quickstart.md`

**语言要求**：除代码标识符、命令、API 字段和第三方专有名词外，任务说明使用简体中文。

**测试要求**：本功能规格明确要求自动化验证覆盖主流程、空输入、信息不足、补充事实生成草稿、错误提示和事实边界展示，因此各用户故事均包含测试或人工验收任务。

**组织方式**：任务按用户故事分组，保证每个故事能独立实现、独立测试、独立演示。

## Phase 1：初始化（共享基础设施）

**目标**：把现有离线工程升级为可启动的本地 Spring Boot + React/Vite Web 应用骨架。

- [X] T001 在 `backend/pom.xml` 接入 Spring Boot Web/Test 依赖、Java 21 编译配置和可执行 `spring-boot:run` 插件，并取消默认跳过测试配置
- [X] T002 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/CoffeeAgentApplication.java` 改造为 Spring Boot 启动入口
- [X] T003 [P] 在 `backend/src/main/resources/application.yml` 添加本地服务端口、CORS 允许来源、离线模型模式和敏感配置占位说明
- [X] T004 在 `frontend/package.json` 接入 React、Vite、TypeScript、Vitest、Testing Library 和 Playwright 相关脚本
- [X] T005 [P] 在 `frontend/src/main.tsx` 创建 React 挂载入口并连接 `frontend/index.html`
- [X] T006 [P] 在 `frontend/vite.config.ts` 配置 Vite React 插件、测试环境、开发代理和本地端口

---

## Phase 2：基础能力（阻塞所有用户故事）

**目标**：建立真实 Web 工作台共用的服务契约、状态模型、错误包装和前端 API 层。

- [X] T007 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WebWorkbenchDtos.java` 定义工作台快照、创建会话、提交消息、记录摘要、草稿标签页和可恢复错误 DTO
- [X] T008 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/WebWorkbenchService.java` 封装现有 `TastingSessionApplicationService` 并输出 Web 工作台快照
- [X] T009 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/api/GlobalExceptionHandler.java` 统一可恢复错误响应，确保 `requestId`、`category`、`recoverable`、`nextActions` 和 `preservedInput` 可表达
- [X] T010 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/config/WebWorkbenchConfiguration.java` 装配内存会话仓库、离线 `ModelGateway` 和本切片所需应用服务 Bean
- [X] T011 [P] 在 `frontend/src/services/workbenchTypes.ts` 定义与 `contracts/web-app-contract.md` 对齐的 TypeScript 类型
- [X] T012 [P] 在 `frontend/src/services/workbenchApi.ts` 实现创建会话、读取快照、提交消息和错误响应解析
- [X] T013 [P] 在 `frontend/src/stores/localResumeStore.ts` 实现 `lastSessionId`、`draftInput`、`lastKnownStatus` 和 `savedAt` 的本地恢复读写
- [X] T014 [P] 在 `frontend/src/app/styles.css` 建立稳定三栏工作台布局、消息流、摘要面板、草稿标签页和错误提示的响应式样式

**检查点**：后端能启动空工作台服务，前端能编译到真实 React 入口，所有用户故事可以基于同一契约继续实现。

---

## Phase 3：用户故事 1 - 浏览器完成对话文案切片（优先级：P1）

**目标**：用户在浏览器工作台中完成“创建会话 -> 首轮追问 -> 补充事实 -> 三版草稿”的主流程，并清楚看到确认事实和待确认联想的边界。

**独立测试**：只完成本故事时，启动本地后端和前端后，用户能在浏览器输入“今天喝了一支水洗埃塞，有柑橘和红茶感”，先看到追问且无最终草稿；继续补充豆子、冲煮参数和文案风格后，看到克制版、夸张版和锐评版三类草稿，每类草稿都有事实边界说明。

### 测试与验证

- [X] T015 [P] [US1] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchApiContractTest.java` 添加创建会话、提交首轮消息、补充事实生成草稿的服务契约测试
- [X] T016 [P] [US1] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/application/WorkbenchFactBoundaryTest.java` 覆盖信息不足不得生成草稿、足够事实生成三版草稿、联想词不得写成确认事实
- [X] T017 [P] [US1] 在 `frontend/src/features/conversation/ConversationWorkbenchFlow.test.tsx` 添加首轮追问、摘要更新、三版草稿展示和事实边界标记测试
- [X] T018 [P] [US1] 在 `frontend/e2e/workbench-main-flow.spec.ts` 添加浏览器主流程验收脚本

### 实现

- [X] T019 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java` 实现 `GET /api/workbench/snapshot`、`POST /api/workbench/sessions`、`POST /api/workbench/sessions/{sessionId}/messages`
- [X] T020 [P] [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/domain/WebWorkbenchSession.java` 创建工作台会话状态模型，覆盖 `EMPTY`、`SESSION_CREATED`、`WAITING_FOR_FACTS`、`DRAFTS_READY`、`ERROR_RECOVERABLE`
- [X] T021 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/WebWorkbenchService.java` 映射消息流、当前记录摘要、草稿标签页和事实边界说明
- [X] T022 [US1] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/application/TastingSessionApplicationService.java` 补齐 Web 快照所需的追问、确认事实、待确认联想和草稿状态输出
- [X] T023 [US1] 在 `frontend/src/app/App.tsx` 改造成真实 React 工作台容器，串联启动会话、读取快照、提交消息和加载状态
- [X] T024 [P] [US1] 在 `frontend/src/features/conversation/ConversationThread.tsx` 实现真实消息流组件，区分用户消息、助手追问和来源类型
- [X] T025 [P] [US1] 在 `frontend/src/features/conversation/ConversationComposer.tsx` 实现可提交输入框、提交中状态和空输入拦截
- [X] T026 [P] [US1] 在 `frontend/src/features/tasting-form/CurrentRecordPanel.tsx` 展示已确认事实、待回答问题、待确认联想和草稿状态
- [X] T027 [P] [US1] 在 `frontend/src/features/conversation/DraftTabs.tsx` 实现克制版、夸张版、锐评版标签页以及每个草稿的事实边界说明
- [X] T028 [P] [US1] 在 `frontend/src/features/agent-trace/AgentTracePanel.tsx` 展示基础流程状态和完整 AgentTrace 后续接入占位
- [X] T029 [US1] 在 `specs/002-real-web-app/quickstart.md` 记录 US1 浏览器人工验收步骤、预期页面状态和未接入真实模型的边界
- [X] T030 [US1] 使用 Codex 内置浏览器打开本地前端页面，自测创建会话、首轮追问、补充事实生成三版草稿和事实边界展示，并将检查结果记录到 `specs/002-real-web-app/quickstart.md`

**检查点**：US1 可独立运行、独立测试、独立演示，MVP 到此可验收。

---

## Phase 4：用户故事 2 - 用户可见的错误与恢复（优先级：P2）

**目标**：当本地服务不可用、请求失败、空输入或刷新恢复失败时，工作台给出可理解提示，并尽量保留用户输入和恢复路径。

**独立测试**：关闭后端或模拟请求失败后，前端显示错误类别、可恢复状态和下一步建议；用户刚输入的咖啡描述不会丢失；刷新页面后能恢复最近会话或明确提示重新创建。

### 测试与验证

- [X] T031 [P] [US2] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchErrorContractTest.java` 覆盖空输入、未知会话和可恢复错误响应结构
- [X] T032 [P] [US2] 在 `frontend/src/features/conversation/WorkbenchErrorRecovery.test.tsx` 覆盖服务不可用、提交失败后输入保留、重试和重新创建会话
- [X] T033 [P] [US2] 在 `frontend/e2e/workbench-error-recovery.spec.ts` 添加后端不可用和刷新恢复的浏览器验收脚本

### 实现

- [X] T034 [US2] 在 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java` 为提交消息添加空输入、过短输入、未知会话和请求失败的分级错误映射
- [X] T035 [P] [US2] 在 `frontend/src/components/feedback/RecoverableErrorBanner.tsx` 实现错误类别、下一步建议、重试和重新创建会话入口
- [X] T036 [US2] 在 `frontend/src/app/App.tsx` 接入错误提示、输入保留、重试提交和重新创建会话流程
- [X] T037 [US2] 在 `frontend/src/stores/localResumeStore.ts` 增加刷新恢复失败提示所需的会话状态校验和草稿输入保留逻辑
- [X] T038 [US2] 在 `frontend/src/services/workbenchApi.ts` 屏蔽凭证、Cookie、Authorization Header 和 Session Token 的错误详情展示
- [X] T039 [US2] 使用 Codex 内置浏览器打开本地前端页面，自测服务不可用提示、提交失败输入保留、重试和刷新恢复，并将检查结果记录到 `specs/002-real-web-app/quickstart.md`

**检查点**：US1 和 US2 均可独立运行；服务异常不会静默丢失用户输入或泄露敏感信息。

---

## Phase 5：用户故事 3 - 一键本地验收指引（优先级：P3）

**目标**：用户可以按文档启动后端、启动前端、执行自动化验证，并知道本切片已验证和未验证的能力。

**独立测试**：用户从项目根目录按文档执行命令，应能在 10 分钟内打开工作台并完成 US1 浏览器验收；验证失败时能看到常见原因和恢复方式。

### 测试与验证

- [X] T040 [P] [US3] 在 `frontend/scripts/e2e.mjs` 接入真实 Playwright 命令或等价本地浏览器验收脚本，并让 `npm run test:e2e` 可执行
- [X] T041 [P] [US3] 在 `backend/README.md` 补充 Java 21 检查、`./mvnw test`、`./mvnw spring-boot:run` 和常见启动失败恢复方式
- [X] T042 [P] [US3] 在 `frontend/README.md` 补充 `npm install`、`npm test`、`npm run build`、`npm run test:e2e`、`npm run dev` 和本地页面地址说明

### 实现

- [X] T043 [US3] 在 `specs/002-real-web-app/quickstart.md` 更新最终准确命令、端口、自动化验证结果记录位置和已验证/未验证边界
- [X] T044 [P] [US3] 在 `docs/architecture/real-web-app-business-flow.md` 新增“页面动作 -> 服务命令 -> Agent 编排 -> 用户可见状态”的业务链路学习指南
- [X] T045 [US3] 在 `docs/architecture/idea-local-run-guide.md` 生成面向用户的 IDEA 操作指南，说明 JDK、Maven、Node、Spring Boot 运行配置、前端 npm 配置、环境变量、启动顺序和常见问题
- [X] T046 [US3] 在 `specs/002-real-web-app/contracts/web-app-contract.md` 根据最终实现同步字段名、错误码和本切片明确排除的接口
- [X] T047 [US3] 使用 Codex 内置浏览器打开本地前端页面，按 IDEA 操作指南启动后的地址完成最终浏览器验收，并将检查结果记录到 `specs/002-real-web-app/quickstart.md`

**检查点**：所有目标用户故事均可按文档验证，且用户知道当前切片的真实能力边界。

---

## Phase N：收尾与横切事项

- [X] T048 [P] 在 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/support/FoundationSmokeTest.java` 增加真实 Spring Boot 启动烟测或更新现有烟测以覆盖 Web 工作台 Bean 装配
- [X] T049 [P] 在 `frontend/src/app/App.test.tsx` 增加工作台首屏渲染烟测，确认首页不是 landing page 或空白页
- [X] T050 在 `backend/pom.xml` 运行并修复 `./mvnw test` 发现的问题，确保后端测试不再被默认跳过
- [X] T051 在 `frontend/package.json` 运行并修复 `npm test`、`npm run build`、`npm run test:e2e` 发现的问题
- [X] T052 在 `specs/002-real-web-app/quickstart.md` 记录人工浏览器验收结果、残余风险和真实模型/数据库/小红书/生图未验证边界
- [X] T053 在 `AGENTS.md` 检查 Spec Kit 指针仍指向 `specs/002-real-web-app/plan.md`，并确认新增文档均为简体中文
- [X] T054 在 `specs/002-real-web-app/quickstart.md` 执行并记录密钥扫描命令 `rg -n "sk-[A-Za-z0-9_-]{20,}|AKIA[0-9A-Z]{16}|Authorization: Bearer|Cookie:|Session Token" .` 的结果

## 依赖与执行顺序

- Phase 1 初始化任务先完成，形成真实后端和前端运行时。
- Phase 2 基础能力依赖 Phase 1，并阻塞所有用户故事。
- US1 是 MVP，依赖 Phase 2；完成后即可进行第一次浏览器验收。
- US2 依赖 Phase 2 和 US1 的 API/UI 主链路，但错误恢复场景应能独立测试。
- US3 依赖 US1、US2 的最终命令和行为结果，用于固化验收指引。
- Phase N 在各用户故事完成后执行，用于补齐烟测、全量验证、密钥扫描和文档边界。

## 并行执行示例

### US1 可并行任务

```text
T015 WorkbenchApiContractTest.java
T016 WorkbenchFactBoundaryTest.java
T017 ConversationWorkbenchFlow.test.tsx
T018 workbench-main-flow.spec.ts
T024 ConversationThread.tsx
T025 ConversationComposer.tsx
T026 CurrentRecordPanel.tsx
T027 DraftTabs.tsx
T028 AgentTracePanel.tsx
```

### US2 可并行任务

```text
T031 WorkbenchErrorContractTest.java
T032 WorkbenchErrorRecovery.test.tsx
T033 workbench-error-recovery.spec.ts
T035 RecoverableErrorBanner.tsx
```

### US3 可并行任务

```text
T040 frontend/scripts/e2e.mjs
T041 backend/README.md
T042 frontend/README.md
T044 docs/architecture/real-web-app-business-flow.md
```

## 实施策略

### MVP 优先

先完成 Phase 1、Phase 2 和 US1。此时用户已经可以验收真实 Web 主流程：打开浏览器工作台、创建会话、首轮追问、补充事实、查看三版草稿和事实边界说明。

### 增量交付

1. 完成 US1 后运行后端、前端和浏览器主流程验证。
2. 再完成 US2，验证服务不可用、请求失败、刷新恢复和输入保留。
3. 最后完成 US3，把真实命令、验收结果、失败恢复方式和未验证边界写入文档。

### 验收边界

本任务列表不要求接入真实 PostgreSQL、pgvector、Redis、Kafka、真实模型 API、小红书真实工具、公开发布、点赞、评论、收藏、真实生图或完整 AgentTrace SSE。这些能力必须作为后续独立切片重新规格化、计划和拆任务。
