# 实施计划：[FEATURE]

**分支**：`[###-feature-name]` | **日期**：[DATE] | **规格**：[link]

**输入**：来自 `/specs/[###-feature-name]/spec.md` 的功能规格

**说明**：本文档由 `/speckit-plan` 填充。除代码标识符、命令、API 字段和第三方专有名词外，正文必须使用简体中文。

## 概要

[从功能规格中提取：核心需求 + 技术实现思路 + 主要风险]

## 技术上下文

**语言/版本**：[例如 Java 21、TypeScript 5.x，或 NEEDS CLARIFICATION]

**主要依赖**：[例如 Spring Boot、Spring AI、React、Vite，或 NEEDS CLARIFICATION]

**存储**：[例如 PostgreSQL、pgvector、文件存储，或 N/A]

**测试**：[例如 JUnit、Spring Boot Test、Playwright、Vitest，或 NEEDS CLARIFICATION]

**目标平台**：[例如本地开发环境、Web 浏览器、Spring Boot 服务，或 NEEDS CLARIFICATION]

**项目类型**：[例如前后端分离 Web 应用、后端服务、Agent 工具，或 NEEDS CLARIFICATION]

**性能目标**：[领域相关目标，例如 5 分钟内完成记录与文案生成，或 NEEDS CLARIFICATION]

**约束**：[例如 API Key 不入库/不入 Git、小红书发布需确认，或 NEEDS CLARIFICATION]

**规模/范围**：[例如 MVP 单用户、本地运行、完整工作台，或 NEEDS CLARIFICATION]

## 宪法检查

*关卡：Phase 0 调研前必须通过；Phase 1 设计后必须复查。*

每项用 PASS/FAIL 回答，并给出简短理由。任何 FAIL 都必须写入复杂度追踪并给出缓解方案。

- **真实咖啡体验**：计划是否区分用户已确认事实、模型建议、创作联想和外部参考？
- **可追踪的 Agent 状态**：计划是否说明上下文、记忆、工具调用、接受/拒绝建议和归档记录的变化？
- **工具安全与用户确认**：公开、破坏性、产生成本或涉及凭证的动作是否通过工具适配器并要求用户确认？
- **可验证的垂直切片**：每个切片是否能独立验证后端、前端、持久化、prompt/工具和用户流程？
- **面向学习的克制架构**：计划是否使用已确认技术栈，并避免过早引入多运行时或复杂框架？新增或修改的方法是否计划提供说明作用、入参、返回值/副作用及必要异常契约的注释？
- **中文文档**：计划、调研、数据模型、快速开始、任务和学习材料是否默认使用简体中文？

## 项目结构

### 当前功能文档

```text
specs/[###-feature]/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
└── tasks.md
```

### 源码结构

```text
backend/
├── src/
│   ├── main/java/
│   ├── main/resources/
│   └── test/java/
└── pom.xml

frontend/
├── src/
│   ├── components/
│   ├── features/
│   ├── pages/
│   ├── services/
│   └── stores/
└── package.json

docs/
├── prd/
├── architecture/
└── research/
```

**结构决策**：[说明本功能实际使用的目录、原因和边界]

## 复杂度追踪

> 仅当宪法检查存在必须解释的 FAIL 时填写。

| 违反项 | 为什么需要 | 被拒绝的更简单方案及原因 |
|---|---|---|
| [例如新增独立服务] | [当前必要性] | [为什么应用内逻辑角色不足] |
