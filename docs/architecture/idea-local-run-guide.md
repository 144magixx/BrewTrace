# IDEA 本地运行与验收指南

## 目标

让你可以在 IntelliJ IDEA 中方便地启动后端、启动前端，并在浏览器验收真实 Web 工作台。

## 1. 打开项目

1. 用 IDEA 打开 `/Users/minyuwei/Documents/xhs`。
2. 等待 IDEA 识别 Maven 项目 `backend/pom.xml`。
3. 如果 IDEA 没有自动导入 Maven，打开右侧 Maven 面板，点击 Reload All Maven Projects。

## 2. 配置 JDK

1. 打开 `File -> Project Structure -> Project`。
2. `SDK` 选择 Java 21 或更高版本。
3. 本机可优先选择 Homebrew OpenJDK，例如 `/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home`。
4. `Language level` 选择 21 或更高。

注意：终端里的 `java -version` 可能显示 Java 8，但 Maven wrapper 当前使用的是 Homebrew OpenJDK 25。IDEA 里也要确认不要用 Java 8。

## 3. 后端运行配置

推荐方式 A：Spring Boot 配置

1. 打开 `backend/src/main/java/com/minyuwei/xhs/coffeeagent/CoffeeAgentApplication.java`。
2. 点击 `main` 方法左侧运行按钮。
3. 确认运行配置的 JDK 是 Java 21+。
4. 启动后看到类似 `Tomcat started on port 8080`。

推荐方式 B：Maven 配置

1. 新建 Run Configuration。
2. 类型选择 Maven。
3. Working directory 设置为 `/Users/minyuwei/Documents/xhs/backend`。
4. Command line 填写：

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

1. 先启动后端 `CoffeeAgentApplication` 或 Maven `spring-boot:run`。
2. 再启动前端 `npm run dev`。
3. 浏览器打开 `http://127.0.0.1:5173/`。

## 6. IDEA 中运行测试

后端：

```bash
cd /Users/minyuwei/Documents/xhs/backend
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

4. 期望看到助手追问豆子信息、冲煮参数和文案风格，并且不出现最终草稿。
5. 再输入：

```text
豆子是某烘焙商的埃塞水洗豆，水温 92 度，粉水比 1:15，想看克制、夸张和锐评。
```

6. 期望看到克制版、夸张版和锐评版三类草稿。
7. 确认真正的事实边界说明可见：甜橙、青柠、葡萄柚只是待确认联想，不是用户已确认事实。

## 8. 常见问题

- 页面显示“本地服务暂时不可用”：后端没启动、8080 端口被占用，或前端代理无法连接后端。
- IDEA 编译失败并提示 Java 版本过低：把 Project SDK 和运行配置 JDK 都切到 Java 21+。
- 前端启动失败：先在 `frontend` 目录执行 `npm install`。
- 页面刷新后输入还在但会话不可用：当前切片只做本地单用户轻量恢复，后端内存会话在重启后会丢失，重新创建会话即可。

## 已验证与未验证

已验证：

- 后端 `./mvnw test` 通过。
- 前端 `npm test`、`npm run build`、`npm run test:e2e` 通过。
- Codex 内置浏览器已完成主流程和错误恢复验收。

未验证：

- 真实数据库持久化。
- 真实模型调用。
- 小红书真实搜索、发布、点赞、评论、收藏。
- 图片生成。
