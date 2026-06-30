# 后端说明

后端现在是可启动的本地 Spring Boot Web 服务，复用现有咖啡品鉴 Agent 离线内核，并新增 `workbench` Web 契约层。

## 前置检查

```bash
./mvnw -v
```

期望 Maven 使用 Java 21 或更高版本。当前本机默认 `java -version` 可能是 Java 8，但 `./mvnw` 会使用 Homebrew OpenJDK 25。

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

所有响应都使用 `ApiResponse` envelope，错误响应包含 `code`、`category`、`recoverable` 和 `nextActions`。

## 常见问题

- 如果端口 8080 被占用，先停止已有后端进程，或临时修改 `src/main/resources/application.yml` 的 `server.port`。
- 如果 IDEA 使用 Java 8 编译，切到 Java 21+ SDK 后重新导入 Maven。
- 当前切片不需要 PostgreSQL、pgvector、Redis、Kafka、真实模型 API Key 或小红书登录态。
