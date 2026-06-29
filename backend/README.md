# 后端说明

后端当前实现为可离线编译验证的 Java 21 应用内核，保留 Spring Boot 分层命名、配置对象、Controller/Service/Repository 边界和后续接入点。

## 命令

```bash
./mvnw test
./mvnw -q test
JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home \
  /opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home/bin/java \
  -cp target/classes:target/test-classes \
  com.minyuwei.xhs.coffeeagent.support.TestRunner
```

由于当前环境无法连接 Maven Central，Spring Boot 4.x 与 Spring AI 2.x 依赖暂未写入构建主链路；实现差异记录在 `docs/architecture/backend-design-v0.1.md`。
