# CR: 事实状态链路改造 — 模型驱动语义 × 后端确定性守护

> **变更范围**：将"后端通过关键词/固定词库推断事实"改造为"主模型输出带证据的结构化增量，后端仅执行确定性校验、状态流转和持久化"。
>
> **CR 日期**：2025-07-14
>
> **测试状态**：49 项测试全部通过

---

## 一、改造概述

| 维度 | 改造前 | 改造后 |
|---|---|---|
| 事实识别 | 后端硬编码关键词（`"水洗"`、`"埃塞"`、`"柑橘"`、`"红茶"`、`"92"`） | 主模型通过 `factUpdates` 输出语义判断 |
| 待确认联想 | 不存在 | 模型识别不确定/推断内容，输出 `ADD_PENDING_ASSOCIATION` |
| 确认/拒绝 | 不存在 | 模型输出 `ACCEPT/REJECT_PENDING_ASSOCIATION`，后端校验前置状态 |
| 修正/撤回 | 不存在 | 模型输出 `REVISE/WITHDRAW_CONFIRMED_FACT`，后端校验前置状态 |
| 证据追踪 | 无 | 每项更新必须携带 `sourceMessageId` + `sourceQuote`，后端校验证据连续性 |
| 审计记录 | 无 | 每次状态变更写入不可变 `FactStateChange` |
| 输入上下文 | `ContextEntry` 五字段 | `ContextEntry` 九字段，携带消息 ID、原文引用、理由、状态 |

---

## 二、按业务链路的变动清单

### 链路 1：模型增量契约（输入定义）

#### `FactUpdate.java` — 新增

| 行号 | 变动 | 说明 |
|---:|---|---|
| L3–15 | 新增 | Javadoc：描述模型语义判断契约，明确后端仍需验证 |
| L16–24 | 新增 | `record FactUpdate(action, boundary, value, sourceMessageId, sourceQuote, reason, targetItemId)` |
| L25–32 | 新增 | `enum Action`：`ADD_CONFIRMED_FACT`、`ADD_PENDING_ASSOCIATION`、`ACCEPT_PENDING_ASSOCIATION`、`REJECT_PENDING_ASSOCIATION`、`REVISE_CONFIRMED_FACT`、`WITHDRAW_CONFIRMED_FACT` |
| L34–41 | 新增 | `enum Boundary`：`USER_STATED`、`USER_CONFIRMED`、`USER_UNCERTAIN`、`MODEL_INFERRED`、`PENDING_ASSOCIATION`、`USER_REJECTED` |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/FactUpdate.java`

---

#### `FactStateItem.java` — 新增

| 行号 | 变动 | 说明 |
|---:|---|---|
| L7–19 | 新增 | Javadoc：保存可追踪的事实或联想状态 |
| L20–30 | 新增 | `record FactStateItem(id, status, boundary, value, sourceMessageId, sourceQuote, reason, createdAt, updatedAt)` |
| L31–37 | 新增 | `enum Status`：`CONFIRMED`、`PENDING`、`REJECTED`、`REVISED`、`WITHDRAWN` |
| L47–49 | 新增 | `pending(id, update, now)` — 创建待确认状态项 |
| L59–61 | 新增 | `confirmed(id, update, now)` — 创建确认事实项 |
| L71–73 | 新增 | `transition(nextStatus, update, now)` — 保留 `createdAt`，更新证据和 `updatedAt` |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/domain/FactStateItem.java`

---

#### `FactStateChange.java` — 新增

| 行号 | 变动 | 说明 |
|---:|---|---|
| L7–19 | 新增 | Javadoc：记录已应用的事实状态动作审计 |
| L20–31 | 新增 | `record FactStateChange(id, action, targetItemId, previousStatus, nextStatus, sourceMessageId, sourceQuote, reason, createdAt)` |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/domain/FactStateChange.java`

---

### 链路 2：模型输出契约（输出定义）

#### `ModelAgentMessage.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L12 | **新增字段** | `List<FactUpdate> factUpdates` — 模型输出的事实增量 |
| L13 | 保留 | `List<String> warnings` |
| L16–20 | **新增逻辑** | 紧凑构造器：`factUpdates` 和 `warnings` 的 null 规范化为空不可变列表 |
| L22–26 | **修改 Javadoc** | 说明 `validationErrors()` 不执行证据或状态流转校验 |
| L35–37 | **新增校验** | `factUpdates.size() > 20` 时报告错误 |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ModelAgentMessage.java`

---

### 链路 3：输入上下文增强（ModelContextPackage 输入）

#### `ModelContextPackage.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L43–54 | **修改 record** | `ContextEntry` 新增字段：`sourceMessageId`、`sourceQuote`、`reason`、`state` |
| L63–65 | **新增构造器** | 五参数兼容构造器，旧调用方不立即失效 |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/ModelContextPackage.java`

---

### 链路 4：确定性校验

#### `FactUpdateValidator.java` — 新增

| 行号 | 变动 | 说明 |
|---:|---|---|
| L15–17 | 新增 | 类级 Javadoc：不涉及咖啡语义的确定性校验 |
| L19–22 | 新增 | 常量：`MAX_UPDATES=20`、`MAX_VALUE_LENGTH=500`、`MAX_QUOTE_LENGTH=500`、`MAX_REASON_LENGTH=1000` |
| L31–65 | 新增 | `validate(session, updates)` — 整批原子校验入口 |
| L34–37 | 新增 | 批量上限校验（>20 项直接拒绝） |
| L38 | 新增 | 建立用户消息证据索引 |
| L48 | 新增 | `validateRequiredFields` — 结构必填项和长度 |
| L49 | 新增 | `validateEvidence` — 来源消息存在且原文是连续片段 |
| L50 | 新增 | `validateActionBoundary` — 动作与边界合法组合 |
| L51 | 新增 | `validateTarget` — 目标项存在及状态前置条件 |
| L52–62 | 新增 | 重复更新检测和同目标冲突检测 |
| L73–79 | 新增 | `userMessages(session)` — 仅索引 `USER` 角色消息 |
| L88–95 | 新增 | `validateRequiredFields` — 必填字段 + 长度上限 |
| L105–113 | 新增 | `validateEvidence` — 消息存在性 + `sourceQuote` 包含校验 |
| L122–148 | 新增 | `validateActionBoundary` — 关键规则：`MODEL_INFERRED`/`USER_UNCERTAIN` 不得配合 `ADD_CONFIRMED_FACT` |
| L158–186 | 新增 | `validateTarget` — 新增动作不可指定 `targetItemId`；更新动作必须有合法前置状态 |
| L196–199 | 新增 | `requireText` — 文本非空 + 长度校验 |
| L207–209 | 新增 | `normalized` — 重复检测用标准化 |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/application/FactUpdateValidator.java`

---

### 链路 5：确定性状态应用

#### `FactUpdateApplier.java` — 新增

| 行号 | 变动 | 说明 |
|---:|---|---|
| L22–26 | 新增 | `apply(session, updates)` — 按顺序应用整批已校验增量 |
| L35–58 | 新增 | `applyOne` — 核心状态机：新增、接受、拒绝、修正、撤回 |
| L36–38 | 新增 | `ADD_CONFIRMED_FACT` → `FactStateItem.confirmed` |
| L40–42 | 新增 | `ADD_PENDING_ASSOCIATION` → `FactStateItem.pending` |
| L45–51 | 新增 | switch 表达式：`ACCEPT→CONFIRMED`、`REJECT→REJECTED`、`REVISE→REVISED`、`WITHDRAW→WITHDRAWN` |
| L52–53 | 新增 | `transition` + 审计记录写入 |
| L54–57 | 新增 | **修正事实双写**：原项 → `REVISED`，同时创建新 `CONFIRMED` 替代项 |
| L68–70 | 新增 | `add` — 新增项 + 创建审计记录 |
| L82–85 | 新增 | `change` — 生成不可变审计记录 |
| L92–94 | 新增 | `newItemId` — UUID 稳定标识 |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/application/FactUpdateApplier.java`

---

### 链路 6：会话聚合根

#### `TastingSession.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L23 | **新增字段** | `List<FactStateItem> factStateItems` |
| L24 | **新增字段** | `List<FactStateChange> factStateChanges` |
| L73–80 | **重写** | `confirmedFacts()` — 不再从关键词推断，改为从 `confirmedFactItems()` 映射 `value` |
| L82–89 | **新增** | `confirmedFactItems()` — 过滤 `CONFIRMED` 状态 |
| L91–98 | **新增** | `pendingAssociationItems()` — 过滤 `PENDING` 状态 |
| L100–107 | **新增** | `rejectedAssociationItems()` — 过滤 `REJECTED` 状态 |
| L109–117 | **新增** | `factStateItem(itemId)` — 按 ID 查询 |
| L119–129 | **新增** | `addFactState(item, change)` — 新增状态 + 审计记录 |
| L131–148 | **新增** | `replaceFactState(item, change)` — 替换状态 + 审计记录 |
| L150–157 | **新增** | `factStateItems()` — 不可变快照 |
| L159–166 | **新增** | `factStateChanges()` — 不可变快照 |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/domain/TastingSession.java`

---

### 链路 7：应用服务编排

#### `TastingSessionApplicationService.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L3 | **新增 import** | `FactUpdate` |
| L10–11 | **新增 import** | `FactStateChange`、`FactStateItem` |
| L21–22 | **新增字段** | `FactUpdateValidator`、`FactUpdateApplier` |
| L30–32 | **修改构造器** | 默认构造器注入 `new FactUpdateValidator()` 和 `new FactUpdateApplier()` |
| L42–48 | **新增构造器** | 可注入校验器和应用器的构造器 |
| L80–96 | **新增方法** | `applyFactUpdates(sessionId, updates)` — 原子校验 → 应用 → 持久化 |
| L104–108 | **修改方法** | `workspace()` — 快照包含 `confirmedFactItems`、`pendingAssociationItems`、`rejectedAssociationItems`、`factStateChanges` |
| L128–132 | **新增 record** | `FactUpdateApplicationResult(applied, errors)` |
| L134–145 | **修改 record** | `WorkspaceSnapshot` 新增 `confirmedFacts`、`pendingAssociations`、`rejectedAssociations`、`factStateChanges` 字段 |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/application/TastingSessionApplicationService.java`

---

### 链路 8：上下文组装

#### `ModelContextPackageAssembler.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L64–67 | **修改** | `entry(ContextItem)` — 九参数构造，携带 `sourceMessageId`、`content` 作为 `sourceQuote` |
| L69–72 | **修改** | `entry(ConfirmedFact)` — 九参数构造，携带真实 `boundary`、`sourceMessageId`、`sourceQuote`、`reason`、`CONFIRMED` 状态 |
| L74–77 | **修改** | `entry(PendingAssociation)` — 九参数构造，携带真实 `boundary`、`sourceMessageId`、`sourceQuote`、`reason`、`PENDING` 状态 |
| L79–83 | **修改** | `entry(CandidateMemory)` — 九参数构造，携带 `reason`，`sourceMessageId`/`sourceQuote` 为 null |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/ModelContextPackageAssembler.java`

---

### 链路 9：工作台主流程

#### `WebWorkbenchService.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L87–109 | **修改** | `completeAssistantTurn` — 新增事实增量应用阶段 |
| L92–97 | **新增** | 模型无恢复错误时，调用 `tastingSessionService.applyFactUpdates()`；校验失败转换为 `MODEL_FORMAT_INVALID` |
| L260–274 | **新增方法** | `factUpdateRejectedResult` — 将事实增量校验失败降级为可恢复模型错误 |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/WebWorkbenchService.java`

---

### 链路 10：模型请求构造

#### `OpenAiResponsesRequestFactory.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L28 | **修改常量** | Schema 路径从 `model-message-output-schema-v1.json` 升级到 `v2.json` |
| L171–187 | **修改** | `userInput()` — 发送九字段 `ContextEntry`，模型可见 `sourceMessageId`、`sourceQuote` 等 |

> **路径**：`backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesRequestFactory.java`

---

### 链路 11：Prompt 资源

#### `fact-state-updates-v1.md` — 新增

| 行号 | 变动 | 说明 |
|---:|---|---|
| L1 | 新增 | 标题：主模型事实状态职责 |
| L3 | 新增 | 明确后端不再使用关键词或固定咖啡词库 |
| L5–12 | 新增 | 证据规则：`sourceMessageId` 必须精确匹配、`sourceQuote` 必须逐字一致 |
| L14–25 | 新增 | 动作和边界组合定义 |
| L27–32 | 新增 | 安全边界：禁止补写不存在信息、不得触发高影响副作用 |

> **路径**：`backend/src/main/resources/prompts/agent/fact-state-updates-v1.md`

---

#### `model-message-output-schema-v2.json` — 新增

| 行号 | 变动 | 说明 |
|---:|---|---|
| L3 | 新增 | Schema 名称升级为 `coffee_model_message_v2` |
| L8 | 新增 | `required` 数组包含 `factUpdates` |
| L32–49 | 新增 | `factUpdates` 定义：`maxItems=20`，7 个必填字段，`additionalProperties=false` |

> **路径**：`backend/src/main/resources/prompts/agent/model-message-output-schema-v2.json`

---

### 链路 12：测试

#### `FactStateUpdateTest.java` — 新增

| 行号 | 变动 | 说明 |
|---:|---|---|
| L17–27 | 新增 | 测试类和 `setUp`：创建会话并记录用户消息 |
| L30–37 | 新增 | `appliesUserStatedFactWithExactEvidence` — 用户明确陈述事实 + 精确原文证据 |
| L40–48 | 新增 | `keepsUncertainAndModelInferredValuesPending` — 不确定和推断内容保持待确认 |
| L51–65 | 新增 | `acceptsAndRejectsExistingPendingAssociations` — 接受/拒绝待确认联想 |
| L68–81 | 新增 | `revisesAndWithdrawsConfirmedFactsWithHistory` — 修正/撤回 + 4 条审计记录 |
| L84–97 | 新增 | `rejectsMissingMessageInvalidQuoteIllegalTransitionAndInferredConfirmedFact` — 缺失消息、错误原文、非法流转、推断升级均被拒绝 |
| L100–113 | 新增 | `rejectsDuplicateAndConflictingUpdatesAtomically` — 重复和冲突更新整批拒绝 |
| L115–117 | 新增 | `update(...)` 测试辅助方法 |

> **路径**：`backend/src/test/java/com/minyuwei/xhs/coffeeagent/tasting/application/FactStateUpdateTest.java`

---

#### `OpenAiResponsesRequestFactoryTest.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L18–29 | **修改** | 测试输入改为九字段 `ContextEntry`，携带 `sourceMessageId`、`sourceQuote` |
| L46–51 | **修改** | 新增断言：`coffee_model_message_v2`、`factUpdates`、`sourceMessageId`、`message-1`、`sourceQuote`、主模型事实状态职责 |

> **路径**：`backend/src/test/java/com/minyuwei/xhs/coffeeagent/agent/infrastructure/OpenAiResponsesRequestFactoryTest.java`

---

#### `AgentContextPreviewTest.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L17–41 | **修改** | 测试数据改为完整 `FactStateItem` 构造（含证据字段） |
| L43–48 | **新增** | `fact(...)` 测试辅助方法：构造带证据的确认事实项 |

> **路径**：`backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/application/AgentContextPreviewTest.java`

---

#### `WorkbenchAgentStateContractTest.java` — 修改

| 行号 | 变动 | 说明 |
|---:|---|---|
| L39 | **修改断言** | `confirmedFacts` 预期从 `not(empty())` 改为 `empty()` |
| L40 | **修改断言** | `pendingAssociations` 预期 `empty()` |

> **路径**：`backend/src/test/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchAgentStateContractTest.java`

---

## 三、已删除逻辑

| 文件 | 原位置 | 已删除内容 |
|---|---|---|
| `TastingSession.java` | 旧 `confirmedFacts()` | 通过关键词（`"水洗"`、`"埃塞"`、`"柑橘"`、`"红茶"`、`"92"`）从用户消息推断事实的全部逻辑 |
| `FactBoundaryChecker.java` | 旧事实边界判断 | 关键词匹配和固定咖啡词库依赖（此文件保留但职责被模型替代） |

---

## 四、架构决策记录

1. **模型负责语义，后端守护边界**：主模型执行开放世界的语义判断（识别事实、待确认联想、确认/拒绝意图），后端仅执行确定性的结构校验、证据关联、状态流转和持久化。
2. **原子校验**：`FactUpdateValidator` 对整批增量校验，任一项失败则整批拒绝，避免部分状态污染。
3. **修正双写**：`REVISE_CONFIRMED_FACT` 同时将原事实标记为 `REVISED` 并创建新的 `CONFIRMED` 替代项，保证修正历史可追踪。
4. **证据连续性**：`sourceQuote` 必须是用户消息中的连续原文片段，后端通过 `contains()` 校验。
5. **安全降级**：事实增量校验失败时，`WebWorkbenchService` 将其转换为 `MODEL_FORMAT_INVALID` 可恢复错误，阻断草稿和后续高影响动作。

