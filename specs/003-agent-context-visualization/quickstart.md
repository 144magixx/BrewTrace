# Quickstart：Agent 上下文与记忆输入可视化

## 目标

验证 003 是否能在真实 Web 工作台中展示 Agent 上下文、候选记忆、模型输入预览、模拟输出、事实边界检查和清空当前会话能力，同时保持中间主工作区只用于对话。

## 前置条件

- 本机可使用 Java 21 运行后端。
- 本机可使用 Node.js 运行前端。
- 不需要真实模型 API Key。
- 不需要 PostgreSQL、pgvector、小红书账号或图片生成服务。

## 启动

从项目根目录分别启动后端和前端：

```bash
cd backend
./mvnw spring-boot:run
```

```bash
cd frontend
npm install
npm run dev
```

打开：

```text
http://127.0.0.1:5173/
```

## 自动化验证

后端：

```bash
cd backend
./mvnw test
```

前端：

```bash
cd frontend
npm test -- --run
node scripts/test.mjs
npm run build
```

期望结果：

- 所有测试通过。
- 构建成功。
- 测试覆盖状态卡片区、上下文预览、事实边界和清空当前会话。

## 场景 1：空状态

步骤：

1. 打开工作台。
2. 不创建会话或清空当前会话后刷新页面。

期望：

- 中间主工作区显示“今天喝了什么咖啡？”。
- 中间主工作区没有候选记忆卡片、上下文预览卡片或事实边界检查卡片。
- 右侧当前记录为空。
- 当前记录下方状态卡片区说明当前没有可发送上下文。
- 模型输出区域不得暗示真实模型已调用。

## 场景 2：首轮输入后查看状态来源

步骤：

1. 点击“开始记录”。
2. 输入：“今天喝了一支水洗埃塞，有柑橘和红茶感”。
3. 提交。

期望：

- 中间对话区显示用户消息和助手追问。
- 右侧已确认事实显示“水洗”“埃塞”“柑橘”“红茶感”等用户陈述内容。
- 待确认联想显示“甜橙”“青柠”“葡萄柚”等候选。
- 候选记忆标明来源边界，例如“本地示例，不是真实长期数据库召回”。
- 上下文预览标明哪些内容会发送、哪些仅页面观察、哪些待确认后发送。

## 场景 3：补充事实后查看模拟输出与事实边界

步骤：

1. 在场景 2 基础上继续输入：“豆子是某烘焙商的埃塞水洗豆，水温 92 度，粉水比 1:15，想看克制、夸张和锐评。”
2. 提交。

期望：

- 右侧展示草稿和模拟输出。
- 模拟输出明确标记为模拟或固定样例。
- 事实边界检查能指出：
  - 用户已确认事实。
  - 待确认联想。
  - 无依据或冲突表达。
- 待确认联想不得被写成已确认事实。

## 场景 4：候选记忆冲突

步骤：

1. 使用包含候选记忆冲突的验收样例或测试数据。
2. 查看候选记忆和上下文预览。

期望：

- 冲突候选记忆显示冲突原因。
- 冲突候选默认不进入将发送给模型的上下文。
- 上下文预览中该项显示为排除或仅页面观察。

## 场景 5：新建记录 / 清空当前会话

步骤：

1. 完成至少一轮会话并生成草稿。
2. 点击“新建记录 / 清空当前会话”。
3. 在确认提示中取消。
4. 再次点击并确认。
5. 刷新页面。

期望：

- 取消后当前会话、草稿、上下文预览和状态卡片保持不变。
- 确认后中间对话区回到初始提问。
- 当前记录为空。
- 状态卡片区显示没有可发送上下文。
- 草稿、模拟输出和事实边界检查不再显示。
- 刷新后不恢复已清空的旧会话或旧草稿。
- 页面不得暗示清空动作删除长期记忆、历史归档或外部平台数据。

## 场景 6：未接入外部能力边界

步骤：

1. 查看候选记忆、模型输出和工具状态分区。

期望：

- 候选记忆说明不是长期数据库真实召回。
- 模型输出说明未调用真实模型。
- 小红书相关状态说明未执行搜索、发布、评论、点赞或收藏。
- 页面不要求配置真实 API Key。

## 验收完成标准

- 用户能在 2 分钟内区分当前会话上下文、已确认事实、待确认联想和候选记忆。
- 所有状态项都有来源、状态和发送状态说明。
- 中间主工作区保持为对话区。
- 清空当前会话后刷新不会恢复旧草稿。
- 没有真实模型、长期数据库或小红书外部动作被执行。

## 2026-06-30 实施验证记录

### 自动化验证

后端：

```bash
cd backend
./mvnw test
```

结果：通过。共 12 个测试，覆盖工作台契约、AgentState 契约、上下文预览、事实边界检查、能力边界和清空当前会话。

前端：

```bash
cd frontend
npm test -- --run
node scripts/test.mjs
npm run build
```

结果：通过。Vitest 共 8 个测试文件、13 个测试；静态检查通过；生产构建成功。

### 本地服务验证

本次验证使用：

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

```bash
cd frontend
VITE_API_PROXY=http://127.0.0.1:8081 npm run dev -- --host 127.0.0.1
```

说明：本机 8080 和 5173 已有旧服务占用，因此本次 003 人工验收使用后端 8081、前端 5174。

### 场景验证结论

- 场景 1 空状态：通过。浏览器中可见右侧 Agent 状态卡片区、当前没有可发送上下文、未调用真实模型、未接真实长期数据库、未执行小红书动作；中间主工作区没有候选记忆卡片。
- 场景 2 首轮输入后查看状态来源：通过 HTTP 快照与自动化测试验证。首轮后 `contextItems=2`，已确认事实包含水洗、埃塞、柑橘、红茶；待确认联想包含 `甜橙:SEND_AFTER_CONFIRMATION`、`青柠:PAGE_ONLY`、`葡萄柚:SEND_AFTER_CONFIRMATION`；候选记忆边界为“本地示例，不是真实长期数据库召回”。
- 场景 3 补充事实后查看模拟输出与事实边界：通过 HTTP 快照与自动化测试验证。草稿状态为 `DRAFTS_READY`，三版草稿生成，`modelOutput.sourceBoundary=模拟输出，未调用真实模型`，事实边界检查覆盖 `USER_CONFIRMED`、`PENDING_ASSOCIATION`、`UNSUPPORTED`、`CONFLICT`。
- 场景 4 候选记忆冲突：通过 HTTP 快照与自动化测试验证。冲突候选记忆默认 `EXCLUDED`，事实边界检查中冲突项建议 `REWRITE`。
- 场景 5 新建记录 / 清空当前会话：通过 HTTP 快照与自动化测试验证。确认清空后返回 `EMPTY`，`sessionId=null`，`agentState.contextItems=0`，`sessionControlAction.resultStatus=CLEARED`；前端本地恢复测试覆盖清空 `lastSessionId`、`draftInput` 和记录 `clearedSessionIds`。
- 场景 6 未接入外部能力边界：通过。后端 `capabilityBoundary.realModelConnected=false`、`longTermMemoryConnected=false`、`xiaohongshuConnected=false`；前端显示未调用真实模型、不是真实长期数据库召回、未执行小红书动作。

### 未验证内容与残余风险

- Codex in-app browser 当前页面运行环境没有页面侧 `fetch` 或 `XMLHttpRequest`，因此浏览器内无法完成真实 API 交互点击流。Codex Chrome 插件接口在当前环境不可用；另用临时 headless Chrome 验证了页面可加载、空状态和边界文案，但自动点击恢复错误态未稳定触发。完整交互已由 HTTP 快照、Vitest 流程测试和后端契约测试补足验证。
- 未使用真实模型、真实长期数据库或小红书账号；这是本切片设计边界，不是缺陷。
- 8080 上存在旧本地服务，正式人工验收时应确认前端代理指向本次启动的后端端口，避免误连旧快照契约。

### 敏感信息检查

命令：

```bash
rg -n "(sk-[A-Za-z0-9_-]{20,}|Authorization:|Bearer\s+[A-Za-z0-9._-]+|Cookie:|Session Token|api[_-]?key|API Key)" backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench frontend/src/features/agent-trace frontend/src/services/workbenchApi.ts
```

结果：未发现真实 API Key、Cookie、Authorization Header 或 Session Token。唯一命中是 `frontend/src/services/workbenchApi.ts` 中用于错误详情脱敏的正则模式。
