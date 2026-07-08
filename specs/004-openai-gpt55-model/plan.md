# 实施计划：OpenAI GPT-5.5 真实模型接入

**分支**：`004-openai-gpt55-model` | **日期**：2026-06-30 | **规格**：[spec.md](./spec.md)

**输入**：来自 `/specs/004-openai-gpt55-model/spec.md` 的功能规格

**说明**：本文档由 `/speckit-plan` 填充。除代码标识符、命令、API 字段和第三方专有名词外，正文必须使用简体中文。

## 概要

本功能把现有工作台收敛为唯一真实模型调用链路：移除本地替代生成模式，只保留 `openai-gpt55`，在本地提供 `OPENAI_API_KEY` 后，通过 Spring AI `ChatClient` 进入模型调用；当前内部兼容 transport 仍使用 OpenAI-compatible Responses API 调用 `model: gpt-5.5`，请求基础地址为 `https://saturday.sankuai.com/v1`。用户仍在现有工作台输入咖啡体验，右侧 Agent 状态区展示模型模式、上下文预览、已发送请求、模型返回、三版小红书文案和事实边界检查。

实现思路是继续以 `workbench` 快照作为前端可见边界，扩展现有 `AgentStateSnapshot`、`ContextPreview` 和 `ModelOutputSnapshot`；后端保留 `ModelGateway` 作为业务服务边界，并让 `SpringAiModelGateway` 通过 Spring AI `ChatClient` 调用模型。内部 `ResponsesApiChatModel` 负责把 Spring AI `Prompt` 转为 Responses API 请求，业务服务只处理模型上下文包、三版文案和事实边界结果，不直接依赖 HTTP、鉴权头、底层响应 JSON 或密钥读取细节。真实请求预览只展示脱敏后的业务 JSON，不展示 `Authorization`、Cookie、Session Token 或 API Key。

主要风险包括：真实模型输出格式不稳定导致三版文案缺失；代理 base URL 与官方 OpenAI Responses API 存在兼容差异；用户把待确认联想误认为已确认事实；失败重试时丢失当前咖啡输入；前端 JSON 预览或日志误泄露凭证；以及无 Key 或模型失败时误生成本地替代文案。

## 技术上下文

**语言/版本**：Java 21 作为后端目标运行基线；TypeScript 5.x 作为前端目标语言；本地运行继续使用现有 JDK 和 Node/Vite 环境。

**主要依赖**：后端升级为 Spring Boot 4.1.x、Spring AI 2.0.x、Maven、现有 `ModelGateway` 和工作台应用服务；真实模型调用从业务层看使用 Spring AI `ChatClient`，内部兼容层继续用 Java 21 `HttpClient` 封装 Responses API，避免业务层依赖 SDK/HTTP 细节。前端沿用 React、Vite、现有 `AgentStateCards`、`ContextPreviewPanel` 和 `ModelOutputPanel`。OpenAI 官方文档确认 Responses API 以 `POST /responses` 创建响应，并支持通过 `model`、`input`、`instructions` 和结构化输出约束组织请求。

**存储**：本切片继续使用后端内存会话状态和浏览器本地恢复状态；不新增 PostgreSQL、pgvector、Redis、Kafka 依赖，不写入真实长期记忆数据库。真实模型配置从本地运行环境读取；`~/.config/xhs-coffee-agent/env` 仅作为本地开发注入环境变量的来源，不提交、不展示、不入日志。

**测试**：后端使用现有 Java 行为测试、Spring Boot 契约测试和本地 `TestRunner`；模型客户端使用可替换 fake/stub 覆盖成功返回、超时、鉴权失败、限流和格式异常。前端使用 Vitest、组件测试、工作台流程测试、静态敏感信息检查和人工 quickstart 验收。

**目标平台**：本地开发环境、桌面 Web 浏览器、本地 Spring Boot 服务、本地 Vite 前端服务；真实模型模式通过 `OPENAI_BASE_URL=https://saturday.sankuai.com/v1` 和 `TEXT_MODEL=gpt-5.5` 访问 OpenAI-compatible 服务。

**项目类型**：前后端分离 Web 应用的真实模型调用垂直切片。

**性能目标**：真实模型模式下用户提交后 120 秒内看到三版文案、真实模型状态或可恢复错误；用户 1 分钟内能区分“将发送”“不会发送”“已发送给大模型”“大模型返回”。

**约束**：API Key、Authorization、Cookie、Session Token 和任何可复用凭证不得进入配置文件、Git、前端、日志、测试快照、错误详情或请求预览；真实模型仅通过 `openai-gpt55` 触发；无 Key 或模型失败时必须返回可恢复错误，不得生成本地替代文案；不接入真实小红书发布、真实长期记忆数据库、外部工具执行或自动发布动作；待确认联想和候选记忆不得写成用户确认事实。

**规模/范围**：MVP 本地单用户；实现一条端到端真实模型 vertical slice，覆盖工作台输入、模型上下文包、Spring AI 模型调用、Responses API 兼容 transport、三版文案、请求/响应可视化、事实边界检查和错误恢复。不做多用户权限、远程部署、流式输出、成本统计、长期记忆真实召回、小红书发布或完整 Agent 框架。

## 宪法检查

*关卡：Phase 0 调研前必须通过；Phase 1 设计后必须复查。*

- **真实咖啡体验**：PASS。计划要求模型上下文包和三版文案都保留用户确认事实、模型推断、待确认联想、候选记忆和无依据表达的边界，事实边界检查负责阻止未确认内容成为事实。
- **可追踪的 Agent 状态**：PASS。计划扩展 `AgentStateSnapshot`，展示模型模式、模型状态、上下文预览、脱敏真实请求、模型返回、三版文案、错误状态和事实边界检查。
- **工具安全与用户确认**：PASS。真实模型调用会产生成本，但仅在用户启用 `openai-gpt55` 且本地环境有 Key 时发生；凭证仅后端读取并脱敏，且本切片不执行小红书或其他高影响外部工具。
- **可验证的垂直切片**：PASS。真实模式、上下文预览、三版输出、错误恢复和敏感信息过滤均可通过独立测试和 quickstart 场景验证。
- **面向学习的克制架构**：PASS。沿用 Spring Boot、Maven、React、Vite 和应用内模型网关边界；不引入独立 Agent 运行时、复杂编排框架或真实长期数据库。
- **中文文档**：PASS。计划、调研、数据模型、契约、quickstart 和后续任务默认使用简体中文。

## 项目结构

### 当前功能文档

```text
specs/004-openai-gpt55-model/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openai-gpt55-model-contract.md
└── checklists/
    └── requirements.md
```

### 源码结构

```text
backend/
├── src/main/java/com/minyuwei/xhs/coffeeagent/
│   ├── agent/
│   │   ├── application/
│   │   └── infrastructure/
│   ├── workbench/
│   │   ├── api/
│   │   ├── application/
│   │   └── domain/
│   ├── shared/
│   │   └── config/
│   └── tasting/
├── src/main/resources/
└── src/test/java/com/minyuwei/xhs/coffeeagent/

frontend/
├── src/app/
├── src/components/feedback/
├── src/features/agent-trace/
├── src/features/conversation/
├── src/services/
└── src/stores/
```

**结构决策**：本功能继续以 `workbench` 作为用户可见快照边界；`agent/application/ModelGateway` 承担业务模型边界；`agent/infrastructure` 使用 `SpringAiModelGateway + ChatClient + ResponsesApiChatModel` 作为默认模型入口，并保留 OpenAI-compatible Responses API 客户端和错误映射；`shared/config` 扩展模型模式、base URL、模型名、超时和重试等非敏感配置，密钥只从环境读取。前端只扩展现有右侧状态组件，不新增页面；`ContextPreviewPanel` 负责中文标签和 JSON 风格请求预览，`ModelOutputPanel` 负责真实模型输出状态、三版文案和事实边界检查。

## 复杂度追踪

当前无宪法检查 FAIL，无需复杂度例外。

## Phase 0 调研产物

已生成：[research.md](./research.md)

调研已解决以下问题：

- 是否通过 `ModelGateway` 复用现有模型边界，还是新增与工作台强耦合的模型服务。
- OpenAI-compatible Responses API 请求如何组织，如何兼容 `https://saturday.sankuai.com/v1` 与 `model: gpt-5.5`。
- 三版小红书文案如何要求结构化返回，并在返回格式异常时变成可恢复错误。
- 本地 env 文件如何参与开发，同时保证运行时只消费环境变量且不泄露密钥。
- 上下文预览如何同时展示会发送、不会发送、已发送和大模型返回。
- 真实模型失败如何映射为可恢复错误，并保留当前会话状态。

## Phase 1 设计产物

已生成：

- [data-model.md](./data-model.md)
- [contracts/openai-gpt55-model-contract.md](./contracts/openai-gpt55-model-contract.md)
- [quickstart.md](./quickstart.md)

## Phase 1 宪法复查

- **真实咖啡体验**：PASS。数据模型把 `ConfirmedFact`、`PendingAssociation`、`CandidateMemoryBoundary`、`CopyVariant` 和 `FactBoundaryCheckResult` 分离，契约要求每版文案保留依据类型。
- **可追踪的 Agent 状态**：PASS。契约扩展工作台快照，覆盖 `modelMode`、`modelStatus`、`requestPreview`、`responsePreview`、`copyVariants`、`factBoundaryChecks` 和 `recoverableModelError`。
- **工具安全与用户确认**：PASS。设计要求真实请求预览为脱敏业务 JSON，凭证只在后端模型客户端内部使用；小红书、长期记忆和外部工具状态保持未执行。
- **可验证的垂直切片**：PASS。quickstart 覆盖无 Key 可恢复错误、有 Key 真实生成、请求 JSON 预览、四类错误、事实边界和敏感信息扫描。
- **面向学习的克制架构**：PASS。设计沿用已有 `ModelGateway` 和工作台快照，不引入新运行时；OpenAI-compatible HTTP 细节隔离在 infrastructure。
- **中文文档**：PASS。新增计划产物均使用简体中文。
