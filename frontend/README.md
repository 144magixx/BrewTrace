# 前端说明

前端现在是 React + Vite + TypeScript 本地工作台。首页直接进入“当前记录”工作流，不是 landing page。

## 安装依赖

```bash
npm install
```

## 常用命令

```bash
npm test
npm run build
npm run test:e2e
npm run dev
```

开发服务启动后监听：

```text
http://127.0.0.1:5173
```

Vite 已配置 `/api` 代理到 `http://127.0.0.1:8080`，因此本地验收时先启动后端，再启动前端。

## 004 模型模式

工作台只保留真实文本模型模式：

- `openai-gpt55`：真实模式，后端通过环境变量读取 `OPENAI_BASE_URL`、`TEXT_MODEL` 和 `OPENAI_API_KEY`，请求 `POST /responses`，模型名为 `gpt-5.5`。
- 真实结构化输出可能需要 60-90 秒，本地默认 `MODEL_TIMEOUT_SECONDS=120`。

本地真实模式启动示例：

```bash
set -a
. "$HOME/.config/xhs-coffee-agent/env"
set +a
cd /Users/minyuwei/Documents/xhs/backend
./mvnw spring-boot:run
```

前端固定传递 `modelMode=openai-gpt55`，不会保存或展示密钥。右侧上下文预览会显示“将发送”“已发送给大模型”“大模型返回”，其中请求/响应 JSON 会做敏感信息兜底脱敏。没有本地 `OPENAI_API_KEY` 时，页面会显示可恢复模型错误，不会生成本地替代文案。

## 验收入口

1. 点击“开始记录”创建会话。
2. 输入“今天喝了一支水洗埃塞，有柑橘和红茶感”。
3. 确认页面显示正在调用 GPT-5.5；如果本地 Key 缺失，确认展示 `MODEL_AUTH_FAILED` 可恢复错误。
4. Key 可用时，确认中间聊天区左侧机器人气泡展示克制版、夸张版和锐评版文案，右侧用户气泡展示用户输入。
5. 查看右侧当前记录下方的 Agent 状态卡片，确认当前会话上下文、已确认事实、上下文预览、模型请求和模型返回可见，且中间对话区没有这些卡片或 JSON 调试详情。
6. 点击“新建记录 / 清空当前会话”，先取消，确认状态不变；再次点击并确认，页面回到空记录，刷新后不恢复旧草稿。

## 边界说明

- `openai-gpt55` 是唯一文本模型模式。
- 当前未接真实长期数据库，候选记忆保持空状态。
- 当前未执行小红书搜索、发布、评论、点赞或收藏动作。
- 清空当前会话只清空当前可见会话和浏览器恢复状态，不删除长期记忆、历史归档或外部平台数据。
