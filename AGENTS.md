# 仓库协作指南

## 项目结构与模块组织

本仓库目前处于咖啡品鉴内容 Agent 的规划阶段。文档必须按以下目录归档：

- `docs/prd/`：产品需求与范围。
- `docs/architecture/`：Agent 架构、模块设计、记忆设计和服务边界。
- `docs/research/`：平台、API、风味与可行性调研。
- `docs/meeting-notes/`：讨论纪要和需求对齐记录。
- `docs/review/`：业务链路重构和跨模块变更的代码审查文档。

引入源代码后，后端优先采用传统 Java 目录结构：

- `src/main/java/`：应用源代码。
- `src/main/resources/`：配置、提示词和模板。
- `src/test/java/`：单元测试与集成测试。
- `assets/`：按需存放示例图片、截图或其他非代码产物。

除 `README.md`、`AGENTS.md` 等仓库级指南外，不得将项目文档放在仓库根目录。

## 构建、测试与开发命令

尚未添加构建系统。Java 后端初始化后，应在此记录准确命令。预期命令如下：

- `./mvnw test` 或 `./gradlew test`：运行自动化测试。
- `./mvnw spring-boot:run` 或 `./gradlew bootRun`：本地运行后端。
- `./mvnw verify` 或 `./gradlew check`：运行完整验证。

在工具链实际存在前，不得臆造命令。

### Java 运行时约束

后端要求 Java 21 或更高版本。Maven Wrapper 会继承当前 Shell 的 `JAVA_HOME`，因此每次运行 `./mvnw test`、`./mvnw verify`、`./mvnw spring-boot:run` 或其他后端 Maven 命令前，必须先执行 `./mvnw -v`，确认 Maven 实际使用的 Java 版本不低于 21；仅检查独立的 `java -version` 不足以证明 Maven 使用了同一 JDK。

- 当前本机默认 Shell 的 `JAVA_HOME` 可能仍指向 Amazon Corretto 8，而运行中的后端服务使用 Homebrew OpenJDK 25。不得沿用未经校验的默认 `JAVA_HOME`。
- 若 `./mvnw -v` 显示 Java 8 或其他低于 21 的版本，必须停止构建并切换到已安装的 Java 21+。Homebrew OpenJDK 可使用：`JAVA_HOME="$(brew --prefix openjdk)/libexec/openjdk.jdk/Contents/Home" PATH="$JAVA_HOME/bin:$PATH" ./mvnw test`；其他 Maven 命令采用相同环境前缀。
- Java 版本不匹配时，编译器可能把 `record`、switch 表达式等现代 Java 语法误报为“需要 class、interface 或 enum”“孤立的 case”等源码错误。出现此类大面积语法错误时，必须先核对 `./mvnw -v`，不得直接判断源码损坏或修改业务代码。
- 启动服务与执行测试应使用同一主版本的 Java 21+ 运行时；完成启动后除校验模型配置外，还应确认实际 Java 进程版本与构建环境一致。

### 本地模型服务启动约束

每次启动或重启后端服务前，必须先读取 `$CODEX_HOME/config.toml`；当 `CODEX_HOME` 未设置时使用 `~/.codex/config.toml`。该文件是当前本地模型配置的唯一事实来源，不得继续沿用上一次进程参数、旧文档示例或记忆中的模型名称。

- 顶层 `model` 必须同步为后端启动环境变量 `TEXT_MODEL`。
- 顶层 `model_provider` 指定的 `[model_providers.<name>]` 配置中，`base_url` 必须同步为 `OPENAI_BASE_URL`。
- `requires_openai_auth` 决定是否需要提供本地鉴权环境；不得读取、打印、记录或写回真实 API Key、Token 或其他凭证。
- 配置文件缺失、字段不完整或目标 Provider 不存在时，必须停止启动并向用户说明，不得静默回退到 `application.yml` 默认模型。
- 服务启动后必须请求 `/api/workbench/snapshot`，验证响应中的实际 `modelName` 和模型代理地址与 `config.toml` 一致后，才能宣告服务可验收。

## 编码风格与命名约定

优先遵循已有约定。Java 代码应围绕 `context`、`planning`、`memory`、`tools` 和 `multiagent` 等 Agent 能力建立清晰的包边界。

使用描述性名称：

- 文档：小写 kebab-case，例如 `agent-architecture-v0.1.md`。
- Java 类：`PascalCase`。
- 方法与变量：`camelCase`。
- 常量：`UPPER_SNAKE_CASE`。

保持改动聚焦；除非直接支持当前任务，否则避免大范围重构。

每个新增或修改的方法、函数和构造器都必须具有契约注释，说明其目的、每个输入参数，以及返回值；无返回值时需说明副作用。Java 使用 Javadoc，包含摘要、每个参数的 `@param`、非 `void` 方法的 `@return`，当异常属于调用方可见契约时还必须包含 `@throws`。注释必须解释意图和边界，而不是复述名称或类型。简单 getter/setter、Java `record` 访问器以及框架或编译器生成的方法可以豁免。

不得在 Java、TypeScript、测试或看起来像运行时代码的文档示例中硬编码模型提示词或可复用 JSON 格式。Agent 行为提示词、字段定义、事实边界规则、工具指令和输出契约，以及 JSON Schema、结构化输出契约、工具输入/输出定义、模型请求/响应示例和可复用 JSON 模板，必须以场景化、版本化文件存放于 `backend/src/main/resources/prompts/`；测试夹具存放于 `backend/src/test/resources/prompts/`。运行时代码必须通过提示词资源加载器读取这些资源。允许使用类型化 DTO 及运行时对象或 Map 的序列化；不允许嵌入 JSON 字符串、手工构建可复用 Schema 或手工拼接 JSON。代码中仅可保留稳定资源路径、占位符名称、API 字段名和枚举值。

## 测试指南

新增行为必须同步添加测试。Java 测试放在 `src/test/java/`，并以被测单元命名，例如 `MemoryRetrieverTest`。

Agent 行为必须测试结构化输入、工具输出、记忆召回和提示词约束。若外部 API 阻碍完整测试，应记录缺口并提供本地验证路径。

新增或修改提示词、JSON 资源时，必须添加测试，证明资源已被加载、JSON 资源可解析、动态占位符已替换，并且运行时请求不依赖重复的硬编码提示词或 JSON 文本。

## 提交与拉取请求指南

本仓库尚无提交历史，因此不存在本地惯例。提交信息使用简洁的祈使句，例如：

- `docs: add coffee agent PRD`
- `feat: add memory retrieval prototype`
- `test: cover flavor suggestion ranking`

### 功能分支与合并策略

- 每项开发工作开始前，必须从最新 `main` 签出独立功能分支，名称使用 `feature-{英文内容概括}`，例如 `feature-conversation-history`。
- 禁止直接在 `main` 上进行功能开发或提交功能改动。
- 功能分支必须在相关测试和验证通过后，才可通过合并操作进入 `main`；合并后保留可追溯的提交历史。
- 任何需要紧急修复的线上问题使用 `hotfix-{英文内容概括}` 分支，并在修复验证后合并回 `main`。
- 合并前应确保工作区干净、变更范围与分支主题一致；发生故障时，以已验证提交为单位执行回滚，不得通过删除历史来掩盖问题。

拉取请求应包含简短摘要、验证步骤、关联的 Issue 或决策（如适用），以及 UI 变更的截图。涉及调研依据的变更，应链接至 `docs/research/` 下的相关文档。

## 代码审查文档规范

每次涉及业务链路改造或跨模块变更时，必须生成 CR（代码审查）文档，放置于 `docs/review/` 目录下，文件名采用 `cr-{变更主题}.md` 格式。

CR 文档必须包含以下结构：

1. **改造概述**：对比改造前后的架构差异，使用表格呈现。
2. **按业务链路的变动清单**：按链路分组，每个文件列出具体行号和变动说明。行号标注格式为 `L{行号}`，需与文件实际内容对应。
3. **已删除逻辑**：列出被移除的旧逻辑及其原位置。
4. **架构决策记录**：记录关键设计决策及其理由。

行号来源以文件当前最新内容为准，不得使用近似值或估算值。如果后续迭代导致行号偏移，需更新 CR 文档或在新 CR 中注明。

## Agent 专项约定

项目协作须遵循 [AI_Coding_行为准则.md](./AI_Coding_行为准则.md)。尤其应当先阅读现有文档再修改文件，在需求存在实质性歧义时提问，在可能时验证改动，并清楚说明任何未验证的假设。

项目治理须遵循 [.specify/memory/constitution.md](./.specify/memory/constitution.md)。新的规格、计划、任务和实施评审必须保护真实咖啡记录、追踪 Agent 状态、确认高影响工具操作、交付经过验证的垂直切片，并保持架构的学习导向。

除代码标识符、命令、API 字段、库名称和必要源引文外，所有生成的项目文档默认必须使用简体中文。这包括 PRD、架构说明、调研、Spec Kit 规格、计划、任务、评审和学习材料。

所有流程图及其他技术图表必须使用 `fireworks-tech-graph` skill 生成。生成的 SVG 应与文档放在同一目录或文档的资源目录中，并由文档引用；禁止嵌入 Mermaid 或手写图表语法。

<!-- PROJECT CONTEXT START -->
如需了解当前技术栈、项目结构、真实实现状态、Shell 命令、能力边界、文档导航及下一阶段重点，请先阅读持续维护的项目上下文：
`docs/architecture/current-project-context.md`。

`specs/*/plan.md` 只描述对应功能切片。仅在当前任务涉及该功能时按需阅读，不得把任意单个功能 plan 当作整个仓库的长期事实来源。

任何会改变技术栈、项目结构、构建或启动命令、运行配置来源、已实现能力、假实现边界、核心业务链路、外部集成状态、测试基线或下一阶段优先级的变更，都必须在同一任务中同步更新 `docs/architecture/current-project-context.md` 的相关章节。不得把项目上下文更新留作无明确负责人的后续事项；如果本次变更不影响该文档，也应在交付检查中明确完成过影响判断。
<!-- PROJECT CONTEXT END -->
