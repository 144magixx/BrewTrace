# 事件契约：咖啡品鉴创作 Agent MVP

## 通用约定

- 事件通过 Kafka 传递。
- 核心业务事务先写 PostgreSQL；需要可靠投递的事件先写 `DomainEventOutbox`，再由后端定时 Outbox Publisher 投递 Kafka。
- 领域聚合产生 Domain Event，Application Service 收集并写入 `DomainEventOutbox`。
- 所有事件必须包含 `eventId`、`eventType`、`occurredAt`、`aggregateType`、`aggregateId`、`schemaVersion`、`payload`。
- 消费者必须幂等处理，使用 `eventId` 或业务幂等键去重。
- 高影响动作即使由事件触发，也必须重新校验用户确认状态。
- 事件 payload 不得包含完整 API Key、Authorization Header、Cookie、Session Token。

## Topic 建议

| Topic | 用途 |
|---|---|
| `coffee.record.archived` | 咖啡记录归档后的记忆写入和偏好推断 |
| `draft.set.generated` | 文案生成后的审稿、重复表达检测和候选归档 |
| `tool.call.completed` | 工具调用完成后的状态同步和轨迹补全 |
| `publishing.package.confirmed` | 发布包确认后的发布页填写准备 |
| `agent.trace.completed` | Agent workflow 完成后的学习分析和摘要 |

## CoffeeRecordArchivedEvent

触发时机：用户确认最终文案并归档咖啡记录，核心事务提交后。

```json
{
  "eventId": "uuid",
  "eventType": "CoffeeRecordArchivedEvent",
  "schemaVersion": 1,
  "occurredAt": "2026-06-30T10:00:00Z",
  "aggregateType": "CoffeeRecord",
  "aggregateId": "uuid",
  "payload": {
    "recordId": "uuid",
    "sessionId": "uuid",
    "finalDraftId": "uuid",
    "flavorKeywords": ["甜橙", "红茶"],
    "source": "USER_ARCHIVE"
  }
}
```

消费者：

- Memory consumer：生成 embedding，写入 `memory_embeddings`。
- Preference consumer：基于用户反馈和选择行为生成偏好候选。

## DraftSetGeneratedEvent

触发时机：一次文案草稿集合生成完成。

用途：

- 审稿风险异步复核。
- 历史表达重复检测。
- 文案摘要写入记忆候选。

## ToolCallCompletedEvent

触发时机：工具调用完成，无论成功或失败。

payload 至少包含：

- `toolCallId`
- `sessionId`
- `toolName`
- `outputStatus`
- `riskLevel`
- `requiresConfirmation`

用途：

- 补全 Agent 轨迹。
- 推进发布包状态。
- 记录失败原因和可重试状态。

## PublishingPackageConfirmedEvent

触发时机：用户确认发布包后。

用途：

- 准备标题、正文和图片临时文件。
- 检查小红书登录状态。
- 调用发布页填写工具前的异步准备。

约束：

- 不得直接公开发布。
- 公开发布必须等待发布页预览后的二次确认。

## AgentTraceCompletedEvent

触发时机：一次 Agent workflow 完成。

用途：

- 后续学习分析。
- 生成轨迹摘要。
- 支撑调试和质量评估。

## Outbox 状态

`DomainEventOutbox.status`：

- `PENDING`：已写入，等待投递。
- `PUBLISHED`：已成功投递 Kafka。
- `FAILED_RETRYABLE`：投递失败，可重试。
- `FAILED_DEAD`：超过重试上限，需要人工处理。

重试规则：

- Outbox Publisher 定时轮询 `PENDING` 和到达 `nextRetryAt` 的 `FAILED_RETRYABLE` 事件。
- 每批按 `createdAt` 正序读取。
- 发送成功后写入 `publishedAt` 并标记 `PUBLISHED`。
- 失败后按退避策略更新 `nextRetryAt`。
- 超过重试上限后标记 `FAILED_DEAD`，保留 `lastError`。
- 多实例运行时必须通过 `lockedBy` / `lockedAt` 或数据库锁避免重复投递。
- 消费者必须能处理重复事件。
- 发布、互动、生图等高影响动作不得仅靠事件重试自动重复执行。
