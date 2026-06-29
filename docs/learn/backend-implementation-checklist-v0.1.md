# 后端实现约束验证清单 v0.1

## 已确认事实

- 当前环境无法连接 Maven Central，现代 Spring Boot 与 Spring AI 依赖不能可靠下载。
- 后端实现已先落为 Java 21 可编译的应用内核，并保留 Spring 风格分层边界。

## 验证项

- [X] 业务响应必须使用 `{ requestId, data, error }` envelope。
- [X] 高影响工具必须由 `ToolCallPolicy` 校验确认状态。
- [X] `ToolCallRecorder` 不记录 API Key、Authorization、Cookie。
- [X] Prompt 文件必须放在 `backend/src/main/resources/prompts/` 并带版本号。
- [X] Domain 对象不添加 JPA 注解。
- [X] 真实 Key 不进入 Git、日志、Agent 轨迹或文档。

## 2026-06-30 验证记录

- 后端验证：`./mvnw -q test` 通过；`TestRunner` 行为测试通过。
- 前端验证：`npm test`、`npm run build`、`npm run test:e2e` 通过。
- 真实密钥扫描：`rg -n "sk-[A-Za-z0-9_-]{20,}|AKIA[0-9A-Z]{16}|xox[baprs]-[A-Za-z0-9-]{20,}" .` 无命中。
- 临时文件检查：仅发现被 `.gitignore` 忽略的 `backend/target` 构建目录。

## 待依赖恢复后补充

- Spring Boot 4.x 启动验证。
- Spring AI 2.x 真实模型网关验证。
- Testcontainers PostgreSQL/pgvector/Kafka 集成验证。
