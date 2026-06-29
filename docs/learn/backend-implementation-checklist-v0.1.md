# 后端实现约束验证清单 v0.1

## 已确认事实

- 当前环境无法连接 Maven Central，现代 Spring Boot 与 Spring AI 依赖不能可靠下载。
- 后端实现已先落为 Java 21 可编译的应用内核，并保留 Spring 风格分层边界。

## 验证项

- [ ] 业务响应必须使用 `{ requestId, data, error }` envelope。
- [ ] 高影响工具必须由 `ToolCallPolicy` 校验确认状态。
- [ ] `ToolCallRecorder` 不记录 API Key、Authorization、Cookie。
- [ ] Prompt 文件必须放在 `backend/src/main/resources/prompts/` 并带版本号。
- [ ] Domain 对象不添加 JPA 注解。
- [ ] 真实 Key 不进入 Git、日志、Agent 轨迹或文档。

## 待依赖恢复后补充

- Spring Boot 4.x 启动验证。
- Spring AI 2.x 真实模型网关验证。
- Testcontainers PostgreSQL/pgvector/Kafka 集成验证。
