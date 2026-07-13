# 后端说明

后端现在是可启动的本地 Spring Boot Web 服务，复用现有咖啡品鉴 Agent 离线内核，并新增 `workbench` Web 契约层。

项目全貌、当前真实能力和文档导航见 [项目当前上下文](../docs/architecture/current-project-context.md)。本文只保留后端快速运行信息。

## 前置检查

```bash
export JAVA_HOME="$(brew --prefix openjdk)/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -v
```

Maven 必须实际使用 Java 21 或更高版本。当前本机默认 Shell 的 `JAVA_HOME` 可能仍指向 Java 8，Maven Wrapper 不会自动切换到 Homebrew OpenJDK；必须以 `./mvnw -v` 的输出为准。

启动前还必须按 [AGENTS.md](../AGENTS.md) 读取 `$CODEX_HOME/config.toml` 或 `~/.codex/config.toml`，同步 `TEXT_MODEL`、`OPENAI_BASE_URL` 和本地鉴权环境。不得继续使用 README 中的历史模型示例，也不得输出真实凭证。

## 常用命令

```bash
./mvnw test
./mvnw spring-boot:run
```

服务启动后监听：

```text
http://127.0.0.1:8080
```

## 工作台接口

- `GET /api/workbench/snapshot`
- `POST /api/workbench/sessions`
- `POST /api/workbench/sessions/{sessionId}/messages`
- `POST /api/workbench/sessions/{sessionId}/messages/stream`
- `POST /api/workbench/sessions/{sessionId}/clear`

所有响应都使用 `ApiResponse` envelope，错误响应包含 `code`、`category`、`recoverable` 和 `nextActions`。

## 常见问题

- 如果端口 8080 被占用，先停止已有后端进程，或临时修改 `src/main/resources/application.yml` 的 `server.port`。
- 如果出现大量 `record`、switch 表达式或“需要 class、interface 或 enum”错误，先检查 `./mvnw -v` 是否仍在使用 Java 8。
- 当前主链路会调用配置的真实文本模型；鉴权要求以 `config.toml` 中当前 Provider 的 `requires_openai_auth` 为准。
- 当前不需要 PostgreSQL、pgvector、Redis、Kafka 或小红书登录态；相关包和迁移文件仍属于尚未接通的基础设施骨架。
- 服务启动后必须请求 `GET /api/workbench/snapshot`，确认返回的模型名和代理地址与 `config.toml` 一致。
