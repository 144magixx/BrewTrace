# Quickstart：OpenAI GPT-5.5 真实模型接入验证

## 目标

验证 004 的端到端 vertical slice：

- 无 `OPENAI_API_KEY` 时，`offline-fake` 仍可完成现有流程。
- 有本地 Key 且启用 `openai-gpt55` 时，后端调用 `gpt-5.5` 生成克制版、夸张版、锐评版三版文案。
- 前端可用中文标签区分将发送、不会发送、已发送给大模型和大模型返回。
- 原始请求以 JSON 风格代码框展示，且不包含 API Key、Authorization、Cookie 或 Session Token。
- 超时、鉴权失败、限流和格式异常均展示可恢复错误并保留用户输入。

## 前置条件

- 后端使用 Java 21。
- 前端依赖已安装。
- 本地真实模型配置通过环境变量提供，不写入项目配置文件。
- 本地 env 文件可作为开发便利来源：`~/.config/xhs-coffee-agent/env`。

本地 env 中允许使用的非敏感配置：

```text
OPENAI_BASE_URL=https://saturday.sankuai.com/v1
TEXT_MODEL=gpt-5.5
MODEL_TIMEOUT_SECONDS=120
```

敏感变量只允许以环境变量形式进入后端进程：

```text
OPENAI_API_KEY
```

## 启动方式

### 1. 离线模式

不加载 `OPENAI_API_KEY`，或显式保持默认模式：

```bash
cd /Users/minyuwei/Documents/xhs/backend
./mvnw spring-boot:run
```

另开终端：

```bash
cd /Users/minyuwei/Documents/xhs/frontend
npm run dev
```

打开：

```text
http://127.0.0.1:5173/
```

### 2. 真实模型模式

加载本地 env 后启动后端：

```bash
set -a
. "$HOME/.config/xhs-coffee-agent/env"
set +a
cd /Users/minyuwei/Documents/xhs/backend
./mvnw spring-boot:run
```

前端仍使用：

```bash
cd /Users/minyuwei/Documents/xhs/frontend
npm run dev
```

页面或后端配置中启用 `openai-gpt55` 后，模型名应显示为 `gpt-5.5`。

## 自动化验证

后端：

```bash
cd /Users/minyuwei/Documents/xhs/backend
./mvnw test
```

前端：

```bash
cd /Users/minyuwei/Documents/xhs/frontend
npm test -- --run
node scripts/test.mjs
npm run build
```

预期：

- 后端模型网关测试覆盖 `offline-fake`、真实模式 fake client 成功、超时、鉴权失败、限流和格式异常。
- 前端组件测试覆盖中文标签、JSON 代码框、三版文案、错误恢复和敏感信息过滤。
- 静态检查确认 API Key、Authorization、Cookie、Session Token 不出现在前端可见字符串、测试快照或错误详情中。

## 人工验收场景

### 场景 1：无 Key 离线流程

1. 不加载 `OPENAI_API_KEY` 启动后端。
2. 打开工作台。
3. 输入：“今天喝了一支水洗埃塞，有柑橘和红茶感。”
4. 查看右侧模型输出区域。

预期：

- 页面可继续生成离线输出。
- 模型状态显示 `offline-fake` 或“模拟输出，未调用真实模型”。
- 上下文预览不暗示真实请求已经发送。

### 场景 2：真实 GPT-5.5 三版文案

1. 加载 `~/.config/xhs-coffee-agent/env` 后启动后端。
2. 启用 `openai-gpt55`。
3. 输入：“今天喝了一支水洗埃塞，酸质像柑橘，尾段有红茶感。”
4. 等待生成完成。

预期：

- 模型状态显示“真实模型输出 / GPT-5.5”。
- 结果包含 `克制版`、`夸张版`、`锐评版`。
- `modelName` 显示 `gpt-5.5`。
- 小红书发布、长期记忆数据库和外部工具仍显示未执行或未接入。

### 场景 3：请求与响应预览

1. 在真实模式完成一次生成。
2. 查看右侧上下文预览。

预期：

- 能看到“将发送”“不会发送”“已发送给大模型”“大模型返回”等中文标签。
- “已发送给大模型”区域显示 JSON 风格代码框。
- JSON 中包含 `model: "gpt-5.5"` 和业务上下文。
- JSON 中不包含 API Key、Authorization、Cookie、Session Token。

### 场景 4：事实边界检查

1. 使用只确认“柑橘感”的输入触发生成。
2. 查看三版文案和事实边界检查。

预期：

- 用户明确输入的事实被标为用户依据。
- “甜橙”“葡萄柚”等扩展表达被标为模型推断或待确认联想。
- 未确认的豆庄、海拔、处理法、用户偏好不得成为用户确认事实。

### 场景 5：鉴权失败

1. 使用无效 `OPENAI_API_KEY` 启动后端。
2. 启用 `openai-gpt55` 并触发生成。

预期：

- 页面展示可恢复错误，提示检查本地 Key 或切换离线模式。
- 用户输入和当前会话保留。
- 错误消息不展示 Key、Authorization 或 provider 原始敏感错误。

### 场景 6：超时、限流和格式异常

通过 fake/stub 或测试配置模拟：

- `MODEL_TIMEOUT`
- `MODEL_RATE_LIMITED`
- `MODEL_FORMAT_INVALID`

预期：

- 页面展示对应错误类型和建议动作。
- 用户可重试或切换 `offline-fake`。
- 当前上下文预览、会话和输入不丢失。

## 敏感信息检查

运行以下检查时，不应输出真实凭证：

```bash
cd /Users/minyuwei/Documents/xhs
rg -n "sk-[A-Za-z0-9_-]{20,}|Authorization|Cookie|Session Token|OPENAI_API_KEY=" backend frontend specs docs
```

预期：

- 只允许出现变量名、脱敏说明或测试用假值。
- 不允许出现真实 API Key、Bearer token、Cookie 或 Session Token。

## 未验证内容记录格式

如果本地无法访问真实模型服务，在实现评审中记录：

```text
未验证：真实 openai-gpt55 调用
原因：本地网络、Key 或服务不可用
已验证替代：fake client 成功返回、错误映射、前端展示、敏感信息过滤
残余风险：真实服务响应字段可能与 fake 存在兼容差异
```

## 2026-06-30 实施验证记录

自动化验证：

- 后端：`cd backend && ./mvnw test` 通过，12 个测试通过。
- 前端：`cd frontend && npm test -- --run` 通过，8 个测试文件、13 个测试通过。
- 前端静态脚本：`cd frontend && node scripts/test.mjs` 通过。
- 前端构建：`cd frontend && npm run build` 通过。

敏感信息扫描：

```bash
rg -n "sk-[A-Za-z0-9_-]{20,}|Authorization:\s*Bearer|Cookie:|Session Token|OPENAI_API_KEY=" backend frontend specs docs
```

结果：未发现真实 API Key、Bearer token、Cookie 或 Session Token。命中项均为规格、计划、契约、任务、quickstart、旧切片文档、`.env.example` 空占位或安全说明中的禁止/检查文本。

真实模型人工验收：

已验证：真实 `openai-gpt55` 调用。
方式：后端从仓库外 `~/.config/xhs-coffee-agent/env` 注入 `OPENAI_API_KEY`、`OPENAI_BASE_URL=https://saturday.sankuai.com/v1` 和 `TEXT_MODEL=gpt-5.5`，启动时设置 `MODEL_TIMEOUT_SECONDS=120`；Chrome 打开 `http://127.0.0.1:5173/`，选择真实模型模式并提交咖啡记录。
结果：页面显示 `真实模型输出 / GPT-5.5`、`已发送给大模型`、`大模型返回`、`克制版`、`夸张版`、`锐评版`，且未显示 `模拟输出` 或可恢复模型错误。
残余风险：真实模型返回耗时约 80-90 秒，依赖本地网络和代理服务状态；后续如果 provider schema 兼容性变化，需要优先检查 `OpenAiResponsesRequestFactory` 与 `OpenAiResponsesParser`。
