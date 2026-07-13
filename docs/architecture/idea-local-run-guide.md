# IDEA 本地运行与验收指南

## 目标

让你可以在 IntelliJ IDEA 中方便地启动后端、启动前端，并在浏览器验收真实 Web 工作台。

项目当前实现状态和能力边界见 [项目当前上下文](./current-project-context.md)。本文只说明 IDEA 与本地浏览器操作。

## 1. 打开项目

1. 用 IDEA 打开 `/Users/minyuwei/Documents/xhs`。
2. 等待 IDEA 识别 Maven 项目 `backend/pom.xml`。
3. 如果 IDEA 没有自动导入 Maven，打开右侧 Maven 面板，点击 Reload All Maven Projects。

## 2. 配置 JDK

1. 打开 `File -> Project Structure -> Project`。
2. `SDK` 选择 Java 21 或更高版本。
3. 本机可优先选择 Homebrew OpenJDK，路径可通过 `$(brew --prefix openjdk)/libexec/openjdk.jdk/Contents/Home` 获取。
4. `Language level` 选择 21 或更高。

注意：终端默认 `JAVA_HOME` 可能指向 Java 8，Maven Wrapper 会继承这个错误配置，并不会自动切换到 Homebrew OpenJDK。IDEA Project SDK、Maven Runner JRE、Spring Boot Run Configuration 和终端中的 `./mvnw -v` 都必须确认使用 Java 21+。

## 3. 后端运行配置

推荐方式 A：Spring Boot 配置

1. 打开 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/CoffeeAgentApplication.java`。
2. 点击 `main` 方法左侧运行按钮。
3. 确认运行配置的 JDK 是 Java 21+。
4. 按 [AGENTS.md](../../AGENTS.md) 读取 `$CODEX_HOME/config.toml` 或 `~/.codex/config.toml`，把当前模型名和 Provider 地址配置到运行环境；不要把真实凭证写入项目文件。
5. 启动后看到类似 `Tomcat started on port 8080`。

推荐方式 B：Maven 配置

1. 新建 Run Configuration。
2. 类型选择 Maven。
3. Working directory 设置为 `/Users/minyuwei/Documents/xhs/backend`。
4. Runner JRE 选择 Java 21+，并按 `config.toml` 配置非敏感模型环境变量和所需本地鉴权环境。
5. Command line 填写：

```text
spring-boot:run
```

## 4. 前端运行配置

1. 确认本机已执行过：

```bash
cd /Users/minyuwei/Documents/xhs/frontend
npm install
```

2. 在 IDEA 新建 npm Run Configuration。
3. `package.json` 选择 `/Users/minyuwei/Documents/xhs/frontend/package.json`。
4. Command 选择 `run`。
5. Scripts 选择 `dev`。
6. 启动后看到：

```text
http://127.0.0.1:5173/
```

## 5. 启动顺序

1. 读取 `config.toml` 并确认 Java 21+ 运行环境。
2. 启动后端 `CoffeeAgentApplication` 或 Maven `spring-boot:run`。
3. 请求 `http://127.0.0.1:8080/api/workbench/snapshot`，确认实际模型名和代理地址与 `config.toml` 一致。
4. 启动前端 `npm run dev`。
5. 浏览器打开 `http://127.0.0.1:5173/`。

## 6. IDEA 中运行测试

后端：

```bash
cd /Users/minyuwei/Documents/xhs/backend
export JAVA_HOME="$(brew --prefix openjdk)/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -v
./mvnw test
```

前端：

```bash
cd /Users/minyuwei/Documents/xhs/frontend
npm test
npm run build
npm run test:e2e
```

也可以在 IDEA 的 Maven/npm 面板里分别运行同名命令。

## 7. 浏览器验收流程

1. 打开 `http://127.0.0.1:5173/`。
2. 点击“开始记录”。
3. 输入：

```text
今天喝了一支水洗埃塞，有柑橘和红茶感
```

4. 期望看到用户消息进入会话，并开始调用工作台快照中显示的当前模型；不得用文档中的历史模型名代替运行态值。
5. 如需补充更多事实，可再输入：

```text
豆子是某烘焙商的埃塞水洗豆，水温 92 度，粉水比 1:15，想看克制、夸张和锐评。
```

6. 信息不足时，期望模型一次只提出一个自然追问；信息足够并返回 `POST` 时，期望看到克制版、夸张版和锐评版三类草稿。
7. 确认真正的事实边界说明可见：模型联想和 `flavor_suggestion` 返回只能作为待确认表达，不是用户已确认事实。
8. 打开请求预览，确认展示的是实际发送体，并包含当前会话历史和已注册工具的完整 `description`。

## 8. 常见问题

- 页面显示“本地服务暂时不可用”：后端没启动、8080 端口被占用，或前端代理无法连接后端。
- IDEA 或 Maven 出现大量 `record`、switch 表达式语法错误：检查 Project SDK、Maven Runner JRE、运行配置 JDK 和 `./mvnw -v`，确保全部为 Java 21+。
- 前端启动失败：先在 `frontend` 目录执行 `npm install`。
- 页面刷新后输入还在但会话不可用：当前切片只做本地单用户轻量恢复，后端内存会话在重启后会丢失，重新创建会话即可。

## 已验证与未验证

已验证：

- 后端使用 Java 25、按 Java 21 目标编译时，全量 51 项测试通过。
- 前端 `npm test`、`npm run build`、`npm run test:e2e` 通过。
- 本地真实模型链路已验证结构化消息路由、`flavor_suggestion` Tool Calling 和实际请求体预览。

未验证：

- 真实数据库持久化。
- 小红书真实搜索、发布、点赞、评论、收藏。
- 图片生成。
