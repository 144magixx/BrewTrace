# 任务：咖啡品鉴创作 Agent MVP

**输入**：来自 `/specs/001-coffee-agent-mvp/` 的设计文档，前端文档`docs/architecture/frontend-design-v0.1.md`，后端文档`docs/architecture/backend-design-v0.1.md`

**前置条件**：plan.md、spec.md、research.md、data-model.md、contracts/、quickstart.md、`.specify/memory/constitution.md`

**语言要求**：除代码标识符、命令、API 字段和第三方专有名词外，任务说明必须使用简体中文。

**测试要求**：Agent 行为、工具适配器、记忆召回和用户可见流程必须包含自动测试或明确的人工验证任务。真实模型和小红书能力只能作为显式集成或人工验证，不得成为默认自动化测试依赖。

**组织方式**：任务按用户故事分组，每个用户故事都能独立实现、独立测试、独立演示。

## Phase 1：初始化（共享基础设施）

**目标**：建立前后端工程、基础设施和中文文档约束。

- [X] T001 创建后端 Maven 工程骨架和 `pom.xml`，路径为 `backend/pom.xml`
- [X] T002 创建后端业务边界包结构，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/{agent,tasting,flavor,copywriting,memory,tools,publishing,trace,user,shared}/`
- [X] T003 创建后端测试包结构，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/`
- [X] T004 创建后端资源目录，路径为 `backend/src/main/resources/{prompts,db/migration}/`
- [X] T005 初始化前端 React + Vite + TypeScript 工程，路径为 `frontend/package.json`
- [X] T006 创建前端功能目录，路径为 `frontend/src/features/{conversation,tasting-form,flavor-suggestions,memory,publishing,agent-trace}/`
- [X] T007 创建前端通用目录，路径为 `frontend/src/{app,components,services,stores}/`
- [X] T008 创建 Docker Compose 基础设施定义，路径为 `docker-compose.yml`
- [X] T009 创建仓库环境变量示例文件，路径为 `backend/.env.example`
- [X] T010 创建前端环境变量示例文件，路径为 `frontend/.env.example`
- [X] T011 [P] 配置后端测试和构建命令说明，路径为 `backend/README.md`
- [X] T012 [P] 配置前端测试和开发命令说明，路径为 `frontend/README.md`
- [X] T013 [P] 配置前端 TypeScript、ESLint 和 Vite 基础文件，路径为 `frontend/tsconfig.json`
- [X] T014 [P] 配置前端入口和根组件，路径为 `frontend/src/app/App.tsx`
- [X] T015 更新项目文档索引，路径为 `docs/README.md`

---

## Phase 2：基础能力（阻塞所有用户故事）

**目标**：完成所有用户故事依赖的后端底座、数据结构、工具边界、前端工作台壳和测试基础。

- [X] T016 创建 Spring Boot 应用入口，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/CoffeeAgentApplication.java`
- [X] T017 创建统一 REST envelope 类型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/api/ApiResponse.java`
- [X] T018 创建错误分类和值对象，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/error/ErrorCategory.java`
- [X] T019 创建全局异常处理器，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/api/GlobalExceptionHandler.java`
- [X] T020 创建 `requestId` 过滤器，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/api/RequestIdFilter.java`
- [X] T021 创建本地用户上下文提供器，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/user/application/CurrentUserProvider.java`
- [X] T022 创建类型安全配置类，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/config/CoffeeAgentProperties.java`
- [X] T023 [P] 创建模型配置类，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/config/ModelProperties.java`
- [X] T024 [P] 创建 Embedding 配置类，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/config/EmbeddingProperties.java`
- [X] T025 [P] 创建小红书工具配置类，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/config/XiaohongshuProperties.java`
- [X] T026 [P] 创建 Redis 和文件存储配置类，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/config/StorageProperties.java`
- [X] T027 创建 Flyway 初始化迁移，路径为 `backend/src/main/resources/db/migration/V001__init_core_schema.sql`
- [X] T028 创建 pgvector 与 HNSW 迁移，路径为 `backend/src/main/resources/db/migration/V002__memory_embeddings_pgvector.sql`
- [X] T029 创建 Outbox 表迁移，路径为 `backend/src/main/resources/db/migration/V003__domain_event_outbox.sql`
- [X] T030 创建发布、工具、轨迹表迁移，路径为 `backend/src/main/resources/db/migration/V004__tool_trace_publishing.sql`
- [X] T031 创建基础领域事件接口，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/domain/DomainEvent.java`
- [X] T032 创建聚合根基类，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/domain/AggregateRoot.java`
- [X] T033 创建 Outbox 应用服务，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/application/DomainEventOutboxService.java`
- [X] T034 创建 Outbox Publisher 定时任务，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/infrastructure/kafka/OutboxPublisher.java`
- [X] T035 创建 Kafka topic 配置，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/infrastructure/kafka/KafkaTopicConfig.java`
- [X] T036 创建 `ModelGateway` 接口，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ModelGateway.java`
- [X] T037 创建 Spring AI 模型网关适配器，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/SpringAiModelGateway.java`
- [X] T038 [P] 创建 FakeModelGateway 测试适配器，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/support/FakeModelGateway.java`
- [X] T039 创建工具注册中心接口，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tools/application/ToolRegistry.java`
- [X] T040 创建工具适配器接口，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tools/application/ToolAdapter.java`
- [X] T041 创建工具调用策略，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tools/application/ToolCallPolicy.java`
- [X] T042 创建工具调用记录服务，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tools/application/ToolCallRecorder.java`
- [X] T043 [P] 创建 FakeToolAdapter 测试适配器，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/support/FakeToolAdapter.java`
- [X] T044 创建本地文件存储服务，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/infrastructure/storage/LocalFileStorageService.java`
- [X] T045 创建 Redis 健康检查配置，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/shared/infrastructure/redis/RedisHealthConfig.java`
- [X] T046 创建 API 契约测试基类，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/support/ApiContractTestSupport.java`
- [X] T047 创建 Testcontainers 集成测试基类，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/support/IntegrationTestSupport.java`
- [X] T048 创建前端 API client envelope 解析器，路径为 `frontend/src/services/apiClient.ts`
- [X] T049 创建前端 SSE client，路径为 `frontend/src/services/sseClient.ts`
- [X] T050 创建前端全局工作台 store，路径为 `frontend/src/stores/workbenchStore.ts`
- [X] T051 创建三栏工作台布局组件，路径为 `frontend/src/components/layout/WorkbenchLayout.tsx`
- [X] T052 创建咖啡豆线条 Logo 组件，路径为 `frontend/src/components/branding/CoffeeBeanLogo.tsx`
- [X] T053 创建前端基础样式和配色变量，路径为 `frontend/src/app/styles.css`
- [X] T054 创建架构约束验证清单，路径为 `docs/learn/backend-implementation-checklist-v0.1.md`

**检查点**：基础能力可运行，后端能启动，前端能显示空工作台，数据库迁移能创建 PostgreSQL/pgvector/Kafka/Redis 依赖所需结构。

---

## Phase 3：用户故事 1 - 对话记录并生成文案（优先级：P1）🎯 MVP

**目标**：用户用自然语言描述咖啡体验，系统追问缺失信息，区分事实和联想，并生成克制版、夸张版、锐评版文案。

**独立测试**：输入“今天喝了一支水洗埃塞，有柑橘和红茶感”，系统追问豆子信息和冲煮参数；补充事实后生成至少一个可用草稿，未确认风味不得写成事实。

### 测试与验证

- [ ] T055 [P] [US1] 创建对话 API 契约测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/tasting/api/TastingSessionApiContractTest.java`
- [ ] T056 [P] [US1] 创建对话 Agent contract 测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/application/ConversationWorkflowAgentTest.java`
- [ ] T057 [P] [US1] 创建文案事实边界测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/copywriting/domain/DraftFactBoundaryTest.java`
- [ ] T058 [P] [US1] 创建前端对话流程测试，路径为 `frontend/src/features/conversation/ConversationFlow.test.tsx`

### 实现

- [ ] T059 [P] [US1] 创建品鉴会话聚合，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/domain/TastingSession.java`
- [ ] T060 [P] [US1] 创建对话消息值对象，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/domain/ConversationMessage.java`
- [ ] T061 [P] [US1] 创建文案草稿聚合，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/copywriting/domain/DraftCopy.java`
- [ ] T062 [US1] 创建品鉴会话 Repository 接口，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/domain/TastingSessionRepository.java`
- [ ] T063 [US1] 创建品鉴会话 JPA Entity 和 Mapper，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/infrastructure/TastingSessionJpaEntity.java`
- [ ] T064 [US1] 创建品鉴会话 Repository Adapter，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/infrastructure/TastingSessionRepositoryAdapter.java`
- [ ] T065 [US1] 创建对话 Application Service，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/application/TastingSessionApplicationService.java`
- [ ] T066 [US1] 创建上下文组装器，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ContextAssembler.java`
- [ ] T067 [US1] 创建显式工作流 Planner，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ExplicitWorkflowPlanner.java`
- [ ] T068 [US1] 创建 Orchestrator，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/AgentOrchestrator.java`
- [ ] T069 [US1] 创建 InterviewAgent，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/InterviewAgent.java`
- [ ] T070 [US1] 创建 DraftAgent，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/DraftAgent.java`
- [ ] T071 [US1] 创建 ReviewAgent，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ReviewAgent.java`
- [ ] T072 [P] [US1] 创建文案生成 Prompt，路径为 `backend/src/main/resources/prompts/draft/generate-v1.md`
- [ ] T073 [P] [US1] 创建事实边界审稿 Prompt，路径为 `backend/src/main/resources/prompts/review/fact-boundary-v1.md`
- [ ] T074 [US1] 创建品鉴会话 Controller，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/api/TastingSessionController.java`
- [ ] T075 [US1] 创建文案 Controller，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/copywriting/api/DraftController.java`
- [ ] T076 [US1] 创建前端对话输入组件，路径为 `frontend/src/features/conversation/ConversationComposer.tsx`
- [ ] T077 [US1] 创建前端对话流组件，路径为 `frontend/src/features/conversation/ConversationThread.tsx`
- [ ] T078 [US1] 创建前端文案 Tabs 组件，路径为 `frontend/src/features/conversation/DraftTabs.tsx`
- [ ] T079 [US1] 创建当前记录紧凑面板基础组件，路径为 `frontend/src/features/tasting-form/CurrentRecordPanel.tsx`
- [ ] T080 [US1] 将对话、追问和文案草稿接入工作台页面，路径为 `frontend/src/app/App.tsx`

**检查点**：用户故事 1 可独立运行、独立测试、独立演示。

---

## Phase 4：用户故事 2 - 结构化模板完成品鉴记录（优先级：P1）

**目标**：用户通过模板记录咖啡豆、冲煮参数、感官评分和温度段风味，并获得可接受、拒绝、编辑的风味联想。

**独立测试**：用户输入“柑橘”时，系统返回至少 5 个具体候选；保存记录时保留温度段、香气/味道和 0-10 强度评分。

### 测试与验证

- [ ] T081 [P] [US2] 创建模板保存 API 契约测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/tasting/api/TastingTemplateApiContractTest.java`
- [ ] T082 [P] [US2] 创建风味联想 Agent 测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/flavor/application/FlavorSuggestionAgentTest.java`
- [ ] T083 [P] [US2] 创建豆袋图片解析 contract 测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/tasting/application/BagImageExtractionTest.java`
- [ ] T084 [P] [US2] 创建前端模板表单测试，路径为 `frontend/src/features/tasting-form/TastingForm.test.tsx`

### 实现

- [ ] T085 [P] [US2] 创建咖啡豆领域模型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/domain/CoffeeBean.java`
- [ ] T086 [P] [US2] 创建冲煮参数领域模型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/domain/BrewRecipe.java`
- [ ] T087 [P] [US2] 创建感官评分领域模型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/domain/SensoryScore.java`
- [ ] T088 [P] [US2] 创建温度段风味领域模型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/domain/TemperatureFlavor.java`
- [ ] T089 [P] [US2] 创建风味候选领域模型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/flavor/domain/FlavorSuggestion.java`
- [ ] T090 [US2] 创建模板 Application Service，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/application/TastingTemplateApplicationService.java`
- [ ] T091 [US2] 创建风味联想服务，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/flavor/application/FlavorSuggestionService.java`
- [ ] T092 [P] [US2] 创建风味联想 Prompt，路径为 `backend/src/main/resources/prompts/flavor/suggest-v1.md`
- [ ] T093 [US2] 创建豆袋图片资源领域模型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/domain/BagImageAsset.java`
- [ ] T094 [US2] 创建豆袋图片解析服务，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/application/BagImageExtractionService.java`
- [ ] T095 [US2] 创建模板 Controller，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/api/TastingTemplateController.java`
- [ ] T096 [US2] 创建风味联想 Controller，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/flavor/api/FlavorSuggestionController.java`
- [ ] T097 [US2] 创建豆袋图片上传 Controller，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/api/BagImageController.java`
- [ ] T098 [US2] 创建前端模板编辑组件，路径为 `frontend/src/features/tasting-form/TastingForm.tsx`
- [ ] T099 [US2] 创建前端风味候选 chips 组件，路径为 `frontend/src/features/flavor-suggestions/FlavorSuggestionChips.tsx`
- [ ] T100 [US2] 创建前端感官雷达图组件，路径为 `frontend/src/features/tasting-form/SensoryRadar.tsx`
- [ ] T101 [US2] 创建前端豆袋图片上传组件，路径为 `frontend/src/features/tasting-form/BagImageUpload.tsx`
- [ ] T102 [US2] 将模板字段同步到当前记录面板，路径为 `frontend/src/features/tasting-form/CurrentRecordPanel.tsx`

**检查点**：用户故事 2 可独立保存结构化记录并验证风味候选事实边界。

---

## Phase 5：用户故事 3 - 记忆归档与相似记录召回（优先级：P2）

**目标**：用户归档咖啡记录后，系统异步生成长期记忆和偏好候选；新记录生成文案前可以召回相似记录并提示可能重复。

**独立测试**：归档第一条“甜橙、红茶”记录后，第二条相似记录能召回历史摘要、相似原因和可能重复提示；偏好候选可查看、编辑、删除。

### 测试与验证

- [ ] T103 [P] [US3] 创建归档 API 契约测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/tasting/api/ArchiveApiContractTest.java`
- [ ] T104 [P] [US3] 创建 pgvector HNSW 集成测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/memory/infrastructure/MemoryEmbeddingJdbcTest.java`
- [ ] T105 [P] [US3] 创建 Outbox 事务测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/shared/application/DomainEventOutboxTransactionTest.java`
- [ ] T106 [P] [US3] 创建记忆召回 Agent 测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/memory/application/MemoryRecallAgentTest.java`
- [ ] T107 [P] [US3] 创建前端记忆面板测试，路径为 `frontend/src/features/memory/MemoryPanel.test.tsx`

### 实现

- [ ] T108 [P] [US3] 创建咖啡记录聚合，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/domain/CoffeeRecord.java`
- [ ] T109 [P] [US3] 创建用户偏好领域模型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/user/domain/UserPreference.java`
- [ ] T110 [P] [US3] 创建记忆向量领域模型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/memory/domain/MemoryEmbedding.java`
- [ ] T111 [P] [US3] 创建记忆召回领域模型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/memory/domain/MemoryRecall.java`
- [ ] T112 [US3] 创建归档 Application Service，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/application/ArchiveCoffeeRecordService.java`
- [ ] T113 [US3] 创建 `CoffeeRecordArchivedEvent`，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/domain/CoffeeRecordArchivedEvent.java`
- [ ] T114 [US3] 创建记忆向量 JDBC Repository，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/memory/infrastructure/MemoryEmbeddingJdbcRepository.java`
- [ ] T115 [US3] 创建 Embedding 网关适配器，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/memory/infrastructure/EmbeddingModelGateway.java`
- [ ] T116 [US3] 创建记忆消费者，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/memory/infrastructure/MemoryEmbeddingConsumer.java`
- [ ] T117 [US3] 创建偏好推断消费者，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/user/infrastructure/PreferenceInferenceConsumer.java`
- [ ] T118 [US3] 创建记忆召回服务，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/memory/application/MemoryRecallService.java`
- [ ] T119 [P] [US3] 创建记忆压缩 Prompt，路径为 `backend/src/main/resources/prompts/memory/compress-v1.md`
- [ ] T120 [US3] 创建归档 Controller，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/api/ArchiveController.java`
- [ ] T121 [US3] 创建记忆召回 Controller，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/memory/api/MemoryRecallController.java`
- [ ] T122 [US3] 创建用户偏好 Controller，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/user/api/UserPreferenceController.java`
- [ ] T123 [US3] 创建前端记忆召回面板，路径为 `frontend/src/features/memory/MemoryRecallPanel.tsx`
- [ ] T124 [US3] 创建前端用户偏好编辑组件，路径为 `frontend/src/features/memory/UserPreferenceEditor.tsx`
- [ ] T125 [US3] 将记忆召回接入生成文案流程，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/copywriting/application/DraftGenerationService.java`

**检查点**：用户故事 3 可独立归档、异步写入记忆、召回相似记录并管理偏好。

---

## Phase 6：用户故事 4 - 查看 Agent 真实交互过程（优先级：P2）

**目标**：用户在右侧侧边栏看到真实 prompt、模型输出摘要、工具调用、记忆召回和模型决策，并能通过 SSE 实时追加轨迹。

**独立测试**：完成一次对话或模板文案生成后，侧边栏展示至少 5 类轨迹卡片；点击卡片可查看 JSONB snapshot；不得出现完整 API Key。

### 测试与验证

- [ ] T126 [P] [US4] 创建 Agent 轨迹 API 契约测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/trace/api/AgentTraceApiContractTest.java`
- [ ] T127 [P] [US4] 创建 SSE 事件流集成测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/trace/api/AgentTraceSseTest.java`
- [ ] T128 [P] [US4] 创建敏感信息禁止记录测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/trace/application/TraceSecretRedactionTest.java`
- [ ] T129 [P] [US4] 创建前端轨迹侧边栏测试，路径为 `frontend/src/features/agent-trace/AgentTracePanel.test.tsx`

### 实现

- [ ] T130 [P] [US4] 创建 AgentTrace 领域模型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/trace/domain/AgentTrace.java`
- [ ] T131 [P] [US4] 创建 AgentTraceStep 领域模型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/trace/domain/AgentTraceStep.java`
- [ ] T132 [US4] 创建 AgentTrace Repository Adapter，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/trace/infrastructure/AgentTraceRepositoryAdapter.java`
- [ ] T133 [US4] 创建 AgentTraceService，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/trace/application/AgentTraceService.java`
- [ ] T134 [US4] 创建 AgentTraceRecorder，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/trace/application/AgentTraceRecorder.java`
- [ ] T135 [US4] 创建 AgentTrace SSE Publisher，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/trace/api/AgentTraceSsePublisher.java`
- [ ] T136 [US4] 创建 AgentTrace Controller，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/trace/api/AgentTraceController.java`
- [ ] T137 [US4] 将 ModelGateway 调用写入轨迹，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/SpringAiModelGateway.java`
- [ ] T138 [US4] 将 ToolCallRecorder 输出写入轨迹，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tools/application/ToolCallRecorder.java`
- [ ] T139 [US4] 创建前端轨迹卡片组件，路径为 `frontend/src/features/agent-trace/AgentTraceCard.tsx`
- [ ] T140 [US4] 创建前端轨迹侧边栏组件，路径为 `frontend/src/features/agent-trace/AgentTracePanel.tsx`
- [ ] T141 [US4] 创建前端轨迹详情弹窗，路径为 `frontend/src/features/agent-trace/AgentTraceDetailDialog.tsx`
- [ ] T142 [US4] 将 SSE 增量事件接入工作台 store，路径为 `frontend/src/stores/workbenchStore.ts`

**检查点**：用户故事 4 可独立展示、实时追加、展开查看 Agent 真实交互过程。

---

## Phase 7：用户故事 5 - 外部参考与发布确认（优先级：P3）

**目标**：用户可检索小红书外部参考，生成发布包，确认后填写小红书发布页，发布页预览后二次确认才公开发布；用户主动请求时可生成配图候选。

**独立测试**：外部参考最多摘要 5 条且标明来源；未二次确认公开发布执行率为 0；登录受阻时保留发布包并返回 `USER_FIXABLE`。

### 测试与验证

- [ ] T143 [P] [US5] 创建外部参考 API 契约测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/publishing/api/ExternalReferenceApiContractTest.java`
- [ ] T144 [P] [US5] 创建发布包状态机测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/publishing/domain/PublishingPackageStateTest.java`
- [ ] T145 [P] [US5] 创建小红书工具适配器 fake 测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/tools/infrastructure/XiaohongshuToolAdapterTest.java`
- [ ] T146 [P] [US5] 创建图片生成工具测试，路径为 `backend/src/test/java/com/minyuwei/xhs/coffeeagent/tools/infrastructure/ImageGenerationToolAdapterTest.java`
- [ ] T147 [P] [US5] 创建前端发布确认流程测试，路径为 `frontend/src/features/publishing/PublishingFlow.test.tsx`
- [ ] T148 [US5] 创建小红书真实发布人工验证记录模板，路径为 `docs/research/xiaohongshu-manual-verification-v0.1.md`

### 实现

- [ ] T149 [P] [US5] 创建外部参考领域模型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/domain/ExternalReference.java`
- [ ] T150 [P] [US5] 创建发布包领域模型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/domain/PublishingPackage.java`
- [ ] T151 [P] [US5] 创建生成图片资源领域模型，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/domain/GeneratedImageAsset.java`
- [ ] T152 [US5] 创建外部参考检索服务，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/application/ExternalReferenceService.java`
- [ ] T153 [US5] 创建发布包 Application Service，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/application/PublishingPackageService.java`
- [ ] T154 [P] [US5] 创建发布包 Prompt，路径为 `backend/src/main/resources/prompts/publishing/package-v1.md`
- [ ] T155 [US5] 创建小红书工具适配器，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tools/infrastructure/XiaohongshuToolAdapter.java`
- [ ] T156 [US5] 注册小红书 `checkLogin/searchFeeds/getFeedDetail/fillPublish/clickPublish/saveDraft` 工具，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tools/infrastructure/XiaohongshuToolRegistrar.java`
- [ ] T157 [US5] 创建图片生成工具适配器，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/tools/infrastructure/ImageGenerationToolAdapter.java`
- [ ] T158 [US5] 创建外部参考 Controller，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/api/ExternalReferenceController.java`
- [ ] T159 [US5] 创建发布包 Controller，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/api/PublishingPackageController.java`
- [ ] T160 [US5] 创建图片生成 Controller，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/publishing/api/ImageGenerationController.java`
- [ ] T161 [US5] 创建前端外部参考面板，路径为 `frontend/src/features/publishing/ExternalReferencePanel.tsx`
- [ ] T162 [US5] 创建前端发布包检查页，路径为 `frontend/src/features/publishing/PublishingPackageReview.tsx`
- [ ] T163 [US5] 创建前端二次确认弹窗，路径为 `frontend/src/features/publishing/PublishConfirmDialog.tsx`
- [ ] T164 [US5] 创建前端图片生成面板，路径为 `frontend/src/features/publishing/ImageGenerationPanel.tsx`
- [ ] T165 [US5] 将发布状态和工具结果接入 Agent 轨迹栏，路径为 `frontend/src/features/agent-trace/AgentTracePanel.tsx`

**检查点**：用户故事 5 可独立完成外部参考、发布包确认、发布页填写、二次确认和主动生图验证。

---

## Phase 8：收尾与横切事项

**目标**：完成跨故事质量门禁、文档更新、安全检查和 quickstart 验证。

- [ ] T166 [P] 更新后端设计实现差异记录，路径为 `docs/architecture/backend-design-v0.1.md`
- [ ] T167 [P] 更新技术选型学习文档的实现观察，路径为 `docs/learn/technology-selection-learning-v0.1.md`
- [ ] T168 [P] 更新 API 契约与实现差异，路径为 `specs/001-coffee-agent-mvp/contracts/api-contract.md`
- [ ] T169 [P] 更新工具契约与实现差异，路径为 `specs/001-coffee-agent-mvp/contracts/tool-contracts.md`
- [ ] T170 [P] 更新事件契约与实现差异，路径为 `specs/001-coffee-agent-mvp/contracts/event-contracts.md`
- [ ] T171 运行后端单元测试和集成测试，路径为 `backend/pom.xml`
- [ ] T172 运行前端单元测试和 Playwright 流程测试，路径为 `frontend/package.json`
- [ ] T173 验证真实 Key 未进入代码、日志、Agent 轨迹和文档，路径为 `docs/learn/backend-implementation-checklist-v0.1.md`
- [ ] T174 执行 quickstart 验证场景 0 到 7 并记录结果，路径为 `specs/001-coffee-agent-mvp/quickstart.md`
- [ ] T175 清理未使用的抽象、样例数据和临时文件，路径为 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/`

---

## 依赖与执行顺序

### 阶段依赖

- Phase 1 初始化无依赖。
- Phase 2 基础能力依赖 Phase 1 完成，并阻塞所有用户故事。
- US1 和 US2 都是 P1，依赖 Phase 2，可按顺序实现，也可在基础模型稳定后并行。
- US3 依赖 US1 或 US2 至少一个能产生可归档记录。
- US4 依赖 Phase 2 的 trace 基础，可与 US1/US2 后半段交错推进，但验收需要至少一个已完成流程。
- US5 依赖工具系统、发布包基础和 Agent 轨迹，可在 US1/US2/US4 后实现。
- Phase 8 依赖所有目标用户故事完成。

### 用户故事完成顺序

1. US1 对话记录并生成文案
2. US2 结构化模板完成品鉴记录
3. US3 记忆归档与相似记录召回
4. US4 查看 Agent 真实交互过程
5. US5 外部参考与发布确认

### 故事依赖图

```text
Phase 1 -> Phase 2 -> US1 -> US3 -> US5 -> Phase 8
                     -> US2 -> US3
                     -> US4
```

## 并行执行示例

### US1 可并行任务

```text
T055 对话 API 契约测试
T056 对话 Agent contract 测试
T057 文案事实边界测试
T058 前端对话流程测试
T059/T060/T061 领域模型
T072/T073 Prompt 文件
```

### US2 可并行任务

```text
T081/T082/T083/T084 测试任务
T085/T086/T087/T088/T089 领域模型
T098/T099/T100/T101 前端组件
```

### US3 可并行任务

```text
T103/T104/T105/T106/T107 测试任务
T108/T109/T110/T111 领域模型
T119 记忆压缩 Prompt
```

### US4 可并行任务

```text
T126/T127/T128/T129 测试任务
T130/T131 轨迹领域模型
T139/T140/T141 前端轨迹组件
```

### US5 可并行任务

```text
T143/T144/T145/T146/T147 测试任务
T149/T150/T151 领域模型
T154 发布包 Prompt
T161/T162/T163/T164 前端组件
```

## 实施策略

### MVP First

先完成 Phase 1、Phase 2、US1 和 US2，形成可对话、可模板记录、可生成文案的最小可用工作台。这个范围覆盖 P1 用户故事，也是后续记忆、轨迹和发布能力的事实来源。

### 增量交付

1. 交付 US1：证明对话创作和事实边界可用。
2. 交付 US2：证明结构化记录和风味联想可用。
3. 交付 US3：证明长期记忆、pgvector、Kafka Outbox 可用。
4. 交付 US4：证明 Agent 学习侧边栏可用。
5. 交付 US5：证明小红书工具封装、发布确认和主动生图可用。

### 质量门禁

- 每个用户故事必须先完成测试或人工验证任务，再完成实现任务。
- 每个检查点必须能独立演示。
- 不得把未确认风味、外部参考或图片创意写成用户事实。
- 高影响工具必须经过后端确认状态校验。
- 真实 API Key 不得写入代码、日志、Agent 轨迹或文档。

## 格式校验

- 所有任务均使用 `- [ ] T###` checklist 格式。
- 所有用户故事任务均包含 `[US#]` 标签。
- 所有任务描述均包含明确文件路径。
- `[P]` 只标记不同文件且无未完成依赖的可并行任务。
