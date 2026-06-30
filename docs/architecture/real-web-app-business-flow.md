# 真实 Web 应用业务链路学习指南

## 已确认事实

- 本切片已经把后端接成 Spring Boot Web 服务。
- 前端已经接成 React + Vite 工作台。
- 当前主流程不依赖真实模型、数据库、小红书账号或图片生成。

## 链路总览

```text
浏览器动作
  -> frontend/src/app/App.tsx
  -> frontend/src/services/workbenchApi.ts
  -> backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/api/WorkbenchController.java
  -> backend/src/main/java/com/minyuwei/xhs/coffeeagent/workbench/application/WebWorkbenchService.java
  -> backend/src/main/java/com/minyuwei/xhs/coffeeagent/tasting/application/TastingSessionApplicationService.java
  -> backend/src/main/java/com/minyuwei/xhs/coffeeagent/agent/application/AgentOrchestrator.java
  -> InterviewAgent / DraftAgent / ReviewAgent
  -> WebWorkbenchService 映射成页面快照
  -> React 工作台更新消息、摘要、草稿和错误提示
```

## 创建会话

用户点击“开始记录”后：

1. `App.tsx` 调用 `createSession()`。
2. `workbenchApi.ts` 请求 `POST /api/workbench/sessions`。
3. `WorkbenchController` 接收请求并返回 `ApiResponse<WorkbenchSnapshot>`。
4. `WebWorkbenchService` 调用 `TastingSessionApplicationService.createSession()`。
5. 会话保存在内存 `TastingSessionRepositoryAdapter`。
6. 前端保存 `lastSessionId` 到 `localStorage`，用于刷新恢复。

## 首轮追问

用户输入“今天喝了一支水洗埃塞，有柑橘和红茶感”后：

1. `App.tsx` 调用 `submitMessage(sessionId, content)`。
2. 后端把用户消息追加到 `TastingSession`。
3. `AgentOrchestrator` 调用 `InterviewAgent.askMissingFacts()`。
4. 因缺少豆子/烘焙商、冲煮参数和文案风格，后端返回追问。
5. `WebWorkbenchService` 把当前状态映射为 `WAITING_FOR_FACTS`。
6. 页面展示用户消息、助手追问、确认事实和待确认联想。

## 三版草稿

用户补充豆子、冲煮参数和文案风格后：

1. `InterviewAgent` 判断缺失问题为空。
2. `DraftAgent` 生成克制版、夸张版和锐评版三类草稿。
3. `ReviewAgent` 保留事实边界检查结果。
4. `WebWorkbenchService` 返回 `DRAFTS_READY`。
5. 页面展示三版草稿，并显示“甜橙、青柠、葡萄柚仅作为待确认联想，不写成事实。”

## 错误恢复

当后端不可用时：

1. `workbenchApi.ts` 捕获 `fetch` 失败。
2. 前端生成 `SERVICE_UNAVAILABLE` 可恢复错误。
3. `App.tsx` 保留当前输入到 `localStorage`。
4. `RecoverableErrorBanner` 展示错误类别、恢复建议、重试和重新创建入口。

## 待用户确认假设

- 当前只支持本地单用户会话。
- 当前恢复状态只用于浏览器便利性，不是长期咖啡记录。
- 后续如果接入真实模型、数据库、小红书或生图，应分别重新做规格、计划、任务和验收。
