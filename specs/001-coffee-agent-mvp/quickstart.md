# Quickstart：咖啡品鉴创作 Agent MVP 验证指南

## 前置条件

- 本地已安装 Java 21、Maven、Node.js、Docker。
- 使用 Docker Compose 启动 PostgreSQL/pgvector、Kafka、Kafka UI 与 Redis。
- 用户通过仓库外私有 env 文件提供模型配置，例如 `~/.config/xhs-coffee-agent/env`。文件中包含 `OPENAI_API_KEY=sk-xxxxx`、`TEXT_MODEL=gpt-5.5`、`IMAGE_MODEL=gpt-image-2`、`EMBEDDING_API_KEY=sk-xxxxx`、`EMBEDDING_MODEL=text-embedding-v4`、`EMBEDDING_DIMENSIONS=1024` 等变量。不要把真实 Key 写入代码、文档或 Git。
- 后端通过 Spring Boot `@ConfigurationProperties` 绑定配置；启动失败应能明确提示缺少哪个必填配置，但不得打印完整密钥。
- 小红书自动化验证需要本机 Chrome 和 `/Users/minyuwei/.codex/skills/xiaohongshu-skills` 登录态。

## 启动方式（工程初始化后）

基础设施：

```bash
docker compose up -d postgres kafka kafka-ui redis
```

环境变量：

```bash
set -a
source ~/.config/xhs-coffee-agent/env
set +a
```

后端：

```bash
cd backend
./mvnw spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

测试：

```bash
cd backend
./mvnw test

cd ../frontend
npm test
npm run test:e2e
```

当前仓库尚未初始化源码，上述命令是实现阶段的目标命令。生成任务时不得把它们当成已存在能力。

## 测试分层要求

后端自动化测试：

- Unit Test：领域聚合、值对象、用例编排、工具策略、错误分类。
- Integration Test：PostgreSQL、pgvector、Kafka、Outbox Publisher、SSE event stream，优先使用 Testcontainers。
- Persistence Test：Spring Data JPA 覆盖结构化表的 Repository Adapter；JDBC/SQL 覆盖 `memory_embeddings` 写入、HNSW 索引和向量相似度查询。
- Transaction Test：验证 Application Service 在同一事务内提交核心记录和 `DomainEventOutbox`，并验证异常回滚时二者同时回滚。
- Agent Contract Test：使用 `FakeModelGateway`、`FakeToolAdapter`、`FakeMemoryRetriever`，验证 prompt 约束、事实边界、工具调用和 AgentTrace。
- Model Gateway Test：验证模型超时最多 2 次短重试，失败后返回分级错误，并写入 Agent 轨迹。
- File Storage Test：验证豆袋图片、生图候选和发布临时文件按会话/资源类型落本地目录，数据库只保存路径。
- Redis Health Test：验证 Redis 配置和健康检查可用，但核心工作流不依赖 Redis。

前端自动化测试：

- Vitest / React Testing Library：组件状态、表单、轨迹卡片渲染。
- Playwright：三栏工作台、模式切换、SSE 增量轨迹、发布确认流程。

人工验证：

- 真实模型调用。
- 小红书登录、验证码、发布页填写和二次确认。
- 真实图片生成。

## 验证场景 0：前端工作台设计对齐

1. 打开前端工作台首页。
2. 检查页面是否符合 [前端工作台设计 v0.1](../../docs/architecture/frontend-design-v0.1.md)。
3. 期望看到三栏布局：左侧导航、中间对话创作区 + 当前记录紧凑面板、右侧 Agent 轨迹卡片栏。
4. 中间主工作区默认显示“今天喝了什么咖啡？”、Agent 模式切换和底部文本框。
5. Agent 模式切换必须包含“显式工作流”和“模型自主工具调用”。
6. 右侧 Agent 轨迹使用不同低饱和颜色卡片区分真实 Prompt、上下文组装、模型决策、记忆召回、工具调用、审稿风险、编排模式和系统状态。

通过标准：首屏不是 landing page；整体以白色和极低饱和冷色调为主；咖啡元素仅以简洁线条风咖啡豆 Logo 表达；后续实现不得偏离该设计文档。

## 验证场景 0.1：双 Agent 模式切换

1. 新建品鉴会话，默认模式应为“显式工作流”。
2. 输入一次咖啡感受，检查右侧轨迹栏是否显示本轮使用显式工作流模式，并展示 Planner 步骤。
3. 切换到“模型自主工具调用”。
4. 再输入“别人怎么评价这支豆子？”。
5. 检查右侧轨迹栏是否显示本轮使用模型自主工具调用模式，并展示模型选择小红书搜索工具的原因。

通过标准：模式切换只影响后续轮次；高影响工具仍需确认；Agent 不直接执行 skill 脚本，只能通过后端工具适配器调用。

## 验证场景 1：对话记录并生成文案

1. 创建品鉴会话。
2. 输入：“今天喝了一支水洗埃塞，有柑橘和红茶感。”
3. 期望系统追问豆子信息、冲煮参数和文案风格。
4. 补充事实后生成克制版、夸张版、锐评版文案。
5. 检查文案是否区分用户事实和模型联想。

通过标准：未确认的“甜橙、青柠、葡萄柚”等候选不会被写成事实。

## 验证场景 2：模板记录与风味联想

1. 在模板中填写咖啡豆信息、冲煮参数和感官评分。
2. 在风味输入框填写“柑橘”。
3. 期望系统返回至少 5 个候选，例如柠檬、青柠、甜橙、血橙、葡萄柚、蜜柑、柚子。
4. 接受“甜橙”和“柚子”，拒绝“葡萄柚”。
5. 保存记录。

通过标准：记录保留高温/中温/低温、香气/味道、0-10 强度评分，并只保存用户接受或编辑后的风味。

## 验证场景 3：长期记忆与相似记录召回

1. 归档第一条包含“甜橙、红茶”的咖啡记录。
2. 新建第二条相似记录。
3. 生成文案前触发记忆召回。
4. 期望系统展示相似记录摘要、相似原因和可能重复提示。

通过标准：系统只提示“可能相似”，不自动合并或强判重复；用户偏好自动写入时必须显示依据，并可编辑或删除。

## 验证场景 4：Agent 真实交互侧边栏

1. 完成一次对话或模板文案生成。
2. 打开工作台侧边栏。
3. 检查是否按时间顺序展示用户输入、上下文组装、模型调用、工具调用或记忆召回、审稿判断、最终决策。
4. 检查真实 prompt、工具输入摘要、工具返回摘要和模型决策是否可见。
5. 点击轨迹卡片详情，检查摘要字段用于卡片展示，JSONB snapshot 用于展示真实 prompt、模型输出、工具入参、工具返回和记忆召回。

通过标准：侧边栏至少展示 5 类关键轨迹信息；MVP 默认不脱敏，但不得出现完整 API Key。

## 验证场景 4.0：SSE 实时轨迹推送

1. 打开工作台后，前端通过 REST 获取 workspace 快照。
2. 前端订阅 `GET /api/v1/tasting-sessions/{sessionId}/events`。
3. 用户提交一次对话。
4. 观察右侧 Agent 轨迹栏是否实时追加上下文组装、模型调用、工具调用或记忆召回卡片。
5. 模拟 SSE 断线，前端重新获取 workspace 快照后继续订阅。

通过标准：不刷新页面即可看到实时轨迹；断线重连后状态不丢；SSE 事件中不得出现完整 API Key；发布、生图等确认动作仍通过 REST 接口完成。

## 验证场景 4.1：Kafka 异步事件与 Outbox

1. 用户确认最终文案并归档咖啡记录。
2. 后端同步写入 `CoffeeRecord`、`DraftSet` 状态和 `AgentTrace`。
3. 同一事务写入 `DomainEventOutbox`，事件类型为 `CoffeeRecordArchivedEvent`。
4. 后端定时 Outbox Publisher 轮询到该事件并投递到 Kafka。
5. Memory consumer 生成 embedding 并写入 `memory_embeddings`。
6. Preference consumer 生成用户偏好候选。

通过标准：用户归档结果不等待 embedding 完成；Outbox 投递失败会进入可重试状态并按退避策略重试；Kafka 消费失败时可以重试；重复消费不会重复写入向量、偏好或触发高影响动作；`memory_embeddings` 使用 `text-embedding-v4` 和 1024 维向量，并为向量列创建 HNSW 索引。

## 验证场景 4.2：错误分级与降级

1. 模拟外部参考搜索失败。
2. 期望系统返回 `DEGRADED`，继续生成无外部参考文案，并在 Agent 轨迹栏展示降级原因。
3. 模拟小红书未登录。
4. 期望系统返回 `USER_FIXABLE`，停止发布流程，保留发布包，并给出检查登录和重试操作。
5. 模拟模型调用超时。
6. 期望系统返回 `RETRYABLE`，保留会话和已填写内容，允许用户重试。

通过标准：用户已填写内容不丢；错误响应使用 `{ requestId, data, error }` envelope，且 `error` 包含 `category`、`recoverable`、`nextActions`；Agent 轨迹栏出现系统状态或错误卡片。

## 验证场景 5：小红书外部参考与发布确认

1. 用户询问：“别人是什么感受？”
2. 系统通过封装后的 `xiaohongshu.searchFeeds` 和详情工具获取最多 5 条参考摘要。
3. 生成发布包后，用户确认标题、正文、标签和图片。
4. 系统调用发布页填写工具，但不公开发布。
5. 用户在浏览器预览后再次确认，系统才调用公开发布工具。

通过标准：外部参考不混入用户事实；未二次确认时公开发布执行率为 0；发布失败或取消时保留发布包和失败原因。

## 验证场景 6：用户主动请求生图

1. 用户基于已生成文案输入：“这个咖啡让我想到清透的甜橙和红茶，请画一张配图。”
2. 系统调用图片生成工具。
3. 返回配图候选，并在轨迹中展示调用原因。

通过标准：用户未主动请求时不调用 `gpt-image-2`；图片提示词基于用户提示、已生成文案和已确认风味，不伪造杯测结论。

## 验证场景 7：后端架构约束

1. 检查源码包结构是否按 `tasting/flavor/copywriting/memory/agent/tools/publishing/trace/user/shared` 等业务边界组织。
2. 检查每个主要边界内部是否分为 `api/application/domain/infrastructure`。
3. 检查 Domain 对象是否没有 JPA 注解。
4. 检查 Prompt 是否位于 `src/main/resources/prompts/` 并带版本。
5. 检查业务用例是否依赖 `ModelGateway`，而不是直接依赖 Spring AI Client。

通过标准：后端实现符合 [后端设计 v0.1](../../docs/architecture/backend-design-v0.1.md)，并能解释每个关键设计在 [技术选型学习文档 v0.1](../../docs/learn/technology-selection-learning-v0.1.md) 中的原因。
