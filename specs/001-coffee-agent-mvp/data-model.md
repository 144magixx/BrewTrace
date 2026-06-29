# 数据模型：咖啡品鉴创作 Agent MVP

## 设计原则

- 所有会影响文案的内容必须标记来源：`USER_CONFIRMED`、`MODEL_SUGGESTED`、`EXTERNAL_REFERENCE`、`IMAGE_EXTRACTED`、`PENDING_CONFIRMATION`。
- 感官评分表达强度，不表达优劣。
- 工具调用、记忆召回和模型决策必须可追踪。
- MVP 单用户运行，固定当前用户为 `local-user`。
- 数据模型保留 `userId`，默认写入 `local-user`，为后续多用户、偏好隔离和记忆隔离预留空间。
- 本系统不建登录态实体；小红书登录态属于外部工具状态。
- 本文描述领域概念模型，不等同于 JPA Entity 设计。
- 实现时 Domain 对象保持无 JPA 注解；Infrastructure 层提供 JPA Entity、Mapper 和 Repository Adapter。
- pgvector 向量字段、HNSW 索引和相似度查询由 JDBC/SQL 与 Flyway 管理。
- Application Service 负责事务边界；核心状态变更与 `DomainEventOutbox` 写入必须同事务提交。
- 聚合产生 Domain Event，Application Service 收集事件并写入 `DomainEventOutbox`。

## 实体

### TastingSession 品鉴会话

**字段**：`id`、`userId`、`status`、`currentIntent`、`orchestrationMode`、`createdAt`、`updatedAt`、`endedAt`、`activeDraftId`

**关系**：拥有多个 `ConversationMessage`、`FlavorSuggestion`、`DraftCopy`、`AgentTrace`。

**规则**：会话结束后可继续查看轨迹；归档成功后关联一个 `CoffeeRecord`。

**编排模式**：`orchestrationMode` 为 `EXPLICIT_WORKFLOW` 或 `MODEL_TOOL_CALLING`。模式切换只影响后续对话轮次，不改写已完成轨迹。

**状态**：`ACTIVE` -> `READY_TO_ARCHIVE` -> `ARCHIVED`；异常时进入 `PAUSED`。

### ConversationMessage 对话消息

**字段**：`id`、`sessionId`、`role`、`content`、`sourceType`、`createdAt`

**规则**：用户原话保持原样保存；模型追问和解释必须标明角色。

### CoffeeBean 咖啡豆信息

**字段**：`id`、`name`、`roaster`、`origin`、`variety`、`process`、`roastLevel`、`batch`、`roastDate`、`sourceType`、`confirmationStatus`

**关系**：可由 `BagImageAsset` 解析生成候选；被 `CoffeeRecord` 引用。

**规则**：豆袋图片解析结果默认 `PENDING_CONFIRMATION`，用户修改后才可成为确认事实。

### BrewRecipe 冲煮参数

**字段**：`id`、`recordId`、`dripper`、`grinder`、`grindSize`、`doseGram`、`waterGram`、`waterTemperatureCelsius`、`ratio`、`pouringPlan`、`brewTimeSeconds`

**规则**：`pouringPlan` 保存分段注水结构；缺失字段可以为空，但文案生成时需要提示信息不足。

### SensoryScore 感官评分

**字段**：`id`、`recordId`、`dimension`、`value`、`note`

**规则**：`value` 范围为 0-10；`dimension` 至少覆盖酸质、甜感、苦感、醇厚度、干净度、余韵，可扩展。

### TemperatureFlavor 温度段风味

**字段**：`id`、`recordId`、`temperatureStage`、`senseType`、`flavorName`、`description`、`polarity`、`sourceType`、`confirmationStatus`

**规则**：`temperatureStage` 为 `HOT`、`WARM`、`COOL`；`senseType` 为 `AROMA` 或 `TASTE`；用户未接受的候选不得进入最终记录。

### FlavorSuggestion 风味候选

**字段**：`id`、`sessionId`、`inputTerm`、`name`、`description`、`temperatureStage`、`senseType`、`polarity`、`sensoryDimensions`、`status`、`reason`

**规则**：`status` 为 `SUGGESTED`、`ACCEPTED`、`REJECTED`、`EDITED`。例如输入“柑橘”时，候选可包含柠檬、青柠、甜橙、血橙、葡萄柚、蜜柑、柚子。

### DraftCopy 文案草稿

**字段**：`id`、`sessionId`、`style`、`title`、`body`、`tags`、`factBoundaryNotes`、`reviewWarnings`、`satisfactionScore`、`status`

**规则**：`style` 至少支持 `RESTRAINED`、`EXAGGERATED`、`SHARP_REVIEW`；正文必须能追溯到事实、联想或外部参考。

### CoffeeRecord 咖啡记录

**字段**：`id`、`userId`、`beanId`、`sessionId`、`finalDraftId`、`flavorKeywords`、`publishedStatus`、`publishedUrl`、`createdAt`

**关系**：拥有 `BrewRecipe`、`SensoryScore`、`TemperatureFlavor`、`MemoryEmbedding`。

**规则**：可能重复记录只标记 `possibleDuplicateOf`，不得自动合并。

### UserPreference 用户偏好

**字段**：`id`、`userId`、`preferenceType`、`value`、`evidence`、`confidence`、`source`、`status`、`updatedAt`

**规则**：系统可自动写入推断偏好，但必须保存依据，并允许用户编辑或删除。

### MemoryEmbedding 记忆向量

**字段**：`id`、`ownerType`、`ownerId`、`embeddingType`、`modelName`、`dimensions`、`contentSummary`、`metadata`、`vector`、`createdAt`

**规则**：`ownerType` 可为咖啡记录、文案草稿、用户偏好或外部参考摘要；默认模型为 `text-embedding-v4`，默认维度为 1024；`vector` 字段维度必须与 `dimensions` 一致。

**索引**：`vector` 使用 pgvector HNSW 索引，默认 cosine 距离和 `vector_cosine_ops`；结构化过滤字段仍需独立普通索引支持混合召回。

### MemoryRecall 记忆召回记录

**字段**：`id`、`sessionId`、`query`、`recallType`、`resultOwnerType`、`resultOwnerId`、`similarityScore`、`matchedReasons`、`summary`、`usedInPrompt`、`createdAt`

**规则**：召回结果必须说明相似原因，例如相同风味关键词、相同处理法、相似感官评分或相似文案表达；进入 prompt 的召回项必须可在 Agent 轨迹侧边栏中看到。

### ExternalReference 外部参考摘要

**字段**：`id`、`sessionId`、`sourcePlatform`、`sourceTitle`、`sourceUrl`、`query`、`summary`、`matchedKeywords`、`createdAt`

**规则**：每次生成上下文最多使用 5 条；必须标明为外部参考。

### PublishingPackage 发布包

**字段**：`id`、`sessionId`、`draftId`、`title`、`body`、`tags`、`imageAssetIds`、`status`、`riskChecklist`、`confirmationId`

**状态**：`DRAFT_PACKAGE` -> `PACKAGE_CONFIRMED` -> `XHS_FILLED` -> `PREVIEW_CONFIRMED` -> `PUBLISHED`，异常时可进入 `FAILED` 或 `CANCELLED`。

**规则**：`PUBLISHED` 只能在发布包确认、发布页填写、浏览器预览后二次确认后由发布工具写入。

### ToolCallRecord 工具调用记录

**字段**：`id`、`sessionId`、`toolName`、`purpose`、`inputSummary`、`rawInputRef`、`outputStatus`、`outputSummary`、`rawOutputRef`、`riskLevel`、`requiresConfirmation`、`confirmationStatus`、`errorCategory`、`recoverable`、`nextActions`、`startedAt`、`endedAt`

**规则**：高影响工具必须 `requiresConfirmation=true`；真实 API Key 不得进入 `rawInputRef` 或 `outputSummary`。

### AgentTrace 与 AgentTraceStep

**AgentTrace 字段**：`id`、`sessionId`、`traceType`、`orchestrationMode`、`startedAt`、`endedAt`、`finalDecision`、`status`

**AgentTraceStep 字段**：`id`、`traceId`、`sequence`、`stepType`、`title`、`summary`、`promptSnapshot`、`modelOutputSnapshot`、`toolInputSnapshot`、`toolOutputSnapshot`、`memorySnapshot`、`decision`、`toolSelectionReason`、`toolCallId`、`memoryRecallIds`、`createdAt`

**规则**：`stepType` 至少支持 `USER_INPUT`、`CONTEXT_BUILD`、`MODEL_CALL`、`TOOL_CALL`、`MEMORY_RECALL`、`REVIEW`、`PUBLISH_CONFIRMATION`。列表卡片使用 `summary`；详情展示各类 JSONB snapshot。MVP 默认展示未脱敏用户内容，但 API Key、Authorization Header、Cookie、Session Token 不得被记录。

**模式规则**：模型自主工具调用模式下，`toolSelectionReason` 必须记录模型为什么选择该工具；显式工作流模式下，该字段可记录 Planner 步骤原因。

### DomainEventOutbox 领域事件发件箱

**字段**：`id`、`eventId`、`eventType`、`aggregateType`、`aggregateId`、`payload`、`status`、`topic`、`partitionKey`、`retryCount`、`nextRetryAt`、`lockedBy`、`lockedAt`、`lastError`、`createdAt`、`publishedAt`

**规则**：Application Service 的业务事务内写入 outbox 记录；后端定时 publisher 在事务提交后轮询并投递到 Kafka；投递成功后标记为 `PUBLISHED`。消费者必须基于 `eventId` 或业务幂等键去重。多实例 publisher 必须通过 `lockedBy` / `lockedAt` 或数据库锁避免重复投递。

**状态**：`PENDING` -> `PUBLISHED`；失败可进入 `FAILED_RETRYABLE`，超过重试上限后进入 `FAILED_DEAD`。

**候选事件**：`CoffeeRecordArchivedEvent`、`DraftSetGeneratedEvent`、`PublishingPackageConfirmedEvent`、`ToolCallCompletedEvent`、`AgentTraceCompletedEvent`。

### ErrorRecord 错误记录

**字段**：`id`、`sessionId`、`traceId`、`code`、`category`、`message`、`recoverable`、`nextActions`、`sourceType`、`sourceId`、`createdAt`

**规则**：用于记录模型、工具、Kafka、SSE、数据库和安全策略错误。`category` 必须为 `USER_FIXABLE`、`RETRYABLE`、`DEGRADED`、`FATAL`、`SAFETY_BLOCKED` 之一。

### BagImageAsset 豆袋图片资源

**字段**：`id`、`sessionId`、`filePath`、`mimeType`、`ocrText`、`extractedBeanFields`、`confirmationStatus`、`createdAt`

**规则**：文件存储在本地文件系统，路径入库；图片解析只产生候选信息，默认 `PENDING_CONFIRMATION`；用户可修改后写入 `CoffeeBean`。

### GeneratedImageAsset 生成图片资源

**字段**：`id`、`sessionId`、`draftId`、`filePath`、`prompt`、`modelName`、`sourceType`、`createdAt`

**规则**：仅在用户主动请求生图时创建；文件存储在本地文件系统，路径入库；生成图不得作为真实咖啡事实来源。

## 关键关系

- 一个 `TastingSession` 可产生一个最终 `CoffeeRecord`。
- 一个 `CoffeeRecord` 关联一个 `CoffeeBean`、一组冲煮参数、多项感官评分、多条温度段风味和一个最终文案。
- 一个 `DraftCopy` 可关联多个 `ExternalReference` 和 `MemoryRecall`。
- 一个 `AgentTrace` 由多个有序 `AgentTraceStep` 组成，并引用工具调用和记忆召回。
- `PublishingPackage` 引用最终 `DraftCopy` 和图片资源，并通过确认记录进入发布流程。
- `DomainEventOutbox` 记录核心业务事务产生的待发布事件，并由后台 publisher 投递到 Kafka。
