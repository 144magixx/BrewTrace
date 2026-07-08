# Advisor 改造产品验收记录 v0.1

## 1. 验收目标

本次验收聚焦第一阶段 Advisor 化后的用户可见行为：

- 用户聊到与咖啡无关的话题时，Agent 应温和引导回咖啡记录，不生成草稿。
- 用户信息不足时，Agent 应一次只问一个自然问题，不一次性索要多个字段。
- 用户补全关键事实并明确不再补充后，Agent 应进入 `POST`，生成三版文案草稿。
- 工作台仍展示请求预览、响应预览、模型输出、事实边界和能力边界。

## 2. 验收环境

- 日期：2026-07-01
- 后端：`http://127.0.0.1:8081`
- 前端：`http://127.0.0.1:5174`
- 模型：`gpt-5.5`
- API Key 来源：本地环境变量 `OPENAI_API_KEY`
- 浏览器：Chrome

## 3. 验收用例一：跑题后拉回咖啡

输入：

```text
我今天在公司开会被老板骂了，好烦
```

结果：

- `messageType=CONVERSATION`
- 未生成草稿，草稿状态仍为 `HIDDEN`
- Agent 回复类似：“听起来今天确实有点烦。我们先回到咖啡记录：你今天喝的是什么咖啡呀？”
- `conversation.questions` 只有 1 个问题
- 响应预览包含 `advisorTraceRecorded=true`

截图：

![跑题拉回咖啡](./validation/advisor-offtopic-chrome.png)

## 4. 验收用例二：信息不足时逐步追问

输入一：

```text
今天喝了一杯巴拿马
```

结果：

- `messageType=CONVERSATION`
- 未生成草稿
- 只问 1 个问题：“这杯巴拿马是手冲、意式，还是其他冲煮方式呀？”

输入二：

```text
是手冲
```

结果：

- `messageType=CONVERSATION`
- 未生成草稿
- 只问 1 个问题：“这杯巴拿马手冲，豆子名称、庄园或烘焙商你还记得其中一个吗？”

输入三：

```text
豆子是翡翠庄园水洗瑰夏
```

结果：

- `messageType=CONVERSATION`
- 未生成草稿
- 只问 1 个问题：“这杯巴拿马翡翠庄园水洗瑰夏喝起来最明显的一个感受是什么？”

## 5. 验收用例三：补全后生成草稿

输入：

```text
喝起来有茉莉花、柑橘和蜂蜜甜，余韵很干净，没有更多补充了，直接生成文案吧
```

结果：

- `messageType=POST`
- 草稿状态变为 `VISIBLE`
- 页面弹出三版文案选择框
- 三版草稿包含：克制版、夸张版、锐评版
- 文案只使用用户已提供事实，没有自动补写海拔、烘焙商或冲煮参数

截图：

![逐步追问后生成三版草稿](./validation/advisor-stepwise-post-chrome.png)

## 6. 自动化验证

已通过：

```bash
cd backend && ./mvnw test
cd frontend && npm test -- --run
```

覆盖点：

- Prompt 模板加载和动态约束替换。
- `conversation.questions` schema 收紧为恰好 1 个问题。
- 模型返回多个追问时后端拒绝。
- Advisor 链真实挂入 `ChatClient.builder(...).defaultAdvisors(...)`。
- `POST` 仍生成三版草稿。
- 前端工作台与 Agent 状态组件测试通过。

## 7. 验收结论

通过。

本次改造后，用户体验从“一次性索要很多信息”变成“像采访一样逐步追问”。Advisor 链路也已经开始把请求预览、事实边界摘要和调用 trace 纳入 Spring AI 标准调用过程，为后续 Tool Calling、记忆召回和 RAG 接入打好了基础。

## 8. 后续建议

- 把 `POST -> DraftTab` 的产品动作路由抽成 `ModelMessageActionRouter`，让模型意图到产品动作的转换更清晰。
- 将 `FactBoundaryChecker` 的示例型 unsupported 检查升级为基于实际 `factUsages`、`inferences`、`pendingConfirmations` 的动态检查。
- 将 `AgentTraceRecorder` 从单步 trace 升级为完整工作流 trace，支持后续工具调用与记忆召回。
