# 工具契约：咖啡品鉴创作 Agent MVP

## 工具注册协议

每个工具在后端 `ToolRegistry` 中注册，Agent 只能调用注册后的工具适配器。

```json
{
  "name": "tool.name",
  "description": "何时使用",
  "inputSchema": {},
  "outputSchema": {},
  "riskLevel": "LOW",
  "requiresConfirmation": false
}
```

工具调用必须生成 `ToolCallRecord`，并写入 Agent 轨迹侧边栏。

## 工具系统组件

- `ToolRegistry`：维护工具定义、输入输出 schema、风险等级、是否需要确认、允许的 Agent 编排模式。
- `ToolAdapter`：封装具体执行逻辑。小红书、图片生成、豆袋解析、记忆召回、外部参考检索都必须有独立适配器。
- `ToolCallPolicy`：执行前判断工具是否允许在当前模式下调用，是否缺少用户确认，是否属于高影响或产生成本动作。
- `ToolCallRecorder`：执行前后写入工具调用记录，供 Agent 轨迹栏展示。

两种 Agent 编排模式都必须使用同一套工具系统。模型自主工具调用模式只能让模型选择已注册工具，不得直接执行脚本、HTTP 请求或 SDK 调用。

## 工具错误与降级

工具适配器必须返回统一错误分类：

- `USER_FIXABLE`：用户处理后可继续，例如小红书未登录、验证码、缺图片。
- `RETRYABLE`：系统可重试，例如网络超时、临时限流。
- `DEGRADED`：可降级，例如外部参考搜索失败后继续生成文案。
- `FATAL`：工具配置错误或不可用。
- `SAFETY_BLOCKED`：缺少用户确认或风险策略阻断。

工具失败时必须写入 `ToolCallRecord`，包含错误分类、失败原因、是否可重试和建议下一步。

## 风味联想工具

**name**：`flavor.suggest`

**用途**：根据用户输入的模糊风味词生成更具体候选。

**输入**：

```json
{
  "inputTerm": "柑橘",
  "temperatureStage": "HOT",
  "senseType": "TASTE",
  "existingAcceptedFlavors": ["红茶"]
}
```

**输出**：候选列表，每项包含 `name`、`description`、`temperatureStage`、`senseType`、`polarity`、`sensoryDimensions`、`reason`。

**确认要求**：无需执行前确认，但候选写入事实记录前必须由用户接受。

## 记忆召回工具

**name**：`memory.recall`

**用途**：召回相似咖啡、相似文案和用户偏好。

**输入**：

```json
{
  "query": "水洗埃塞 柑橘 红茶",
  "filters": { "recordType": "COFFEE_RECORD" },
  "limit": 3
}
```

**输出**：召回摘要、相似原因、来源记录、是否可能重复。

**确认要求**：无需执行前确认；标记重复必须由用户手动确认。

## 小红书搜索工具

## 小红书登录状态工具

**name**：`xiaohongshu.checkLogin`

**底层能力**：封装 `/Users/minyuwei/.codex/skills/xiaohongshu-skills/scripts/cli.py check-login`。

**输入**：

```json
{}
```

**输出**：当前登录状态、账号摘要、是否需要用户处理验证码或重新登录。

**确认要求**：无需确认；不得把 Cookie、Token 或完整账号敏感信息写入 Agent 轨迹。

## 小红书搜索工具

**name**：`xiaohongshu.searchFeeds`

**底层能力**：封装 `/Users/minyuwei/.codex/skills/xiaohongshu-skills/scripts/cli.py search-feeds`。

**输入**：

```json
{ "keyword": "咖啡豆名 烘焙商 柑橘", "limit": 5 }
```

**输出**：最多 5 条搜索结果摘要，包含 `feedId`、`xsecToken`、标题、作者、摘要和来源平台。

**确认要求**：搜索无需确认；频率必须受控，失败时降级为无外部参考创作。

## 小红书详情工具

**name**：`xiaohongshu.getFeedDetail`

**底层能力**：封装 `get-feed-detail`。

**输入**：

```json
{ "feedId": "xxx", "xsecToken": "xxx" }
```

**输出**：笔记正文、图片信息、评论摘要和可用于文案灵感的观点摘要。

**确认要求**：无需确认；外部内容必须标记为外部参考，不得写成用户体验。

## 小红书发布页填写工具

**name**：`xiaohongshu.fillPublish`

**底层能力**：封装 `fill-publish`。

**输入**：

```json
{
  "titleFile": "/abs/path/title.txt",
  "contentFile": "/abs/path/content.txt",
  "imagePaths": ["/abs/path/image.jpg"]
}
```

**输出**：发布页填写状态、失败原因、下一步提示。

**确认要求**：需要用户确认发布包后才能执行。此工具只填写发布页，不公开发布。

## 小红书公开发布工具

**name**：`xiaohongshu.clickPublish`

**底层能力**：封装 `click-publish`。

**输入**：

```json
{ "publishingPackageId": "uuid", "confirmedAfterPreview": true }
```

**输出**：发布成功状态、发布 URL 或失败原因。

**确认要求**：高风险工具。必须在用户确认发布包并完成发布页预览后二次确认后执行。

## 小红书保存草稿工具

**name**：`xiaohongshu.saveDraft`

**用途**：用户取消或发布受阻时保存草稿。

**确认要求**：建议确认；失败时保留本地发布包。

## 豆袋图片解析工具

**name**：`image.extractBeanInfo`

**用途**：从豆袋图片中提取豆名、烘焙商、产区、处理法、烘焙日期等候选字段。

**实现方式**：通过 `ModelGateway` 调用多模态模型能力；如后续引入 OCR，也只能作为预处理，最终字段仍需模型结构化和用户确认。

**输出规则**：所有字段默认 `PENDING_CONFIRMATION`，用户修改或确认后才写入事实。

## 图片生成工具

**name**：`image.generate`

**用途**：仅在用户主动请求时，根据用户提示词、已生成文案和已确认风味生成配图候选。

**输入**：

```json
{
  "userPrompt": "画一张甜橙和红茶感的咖啡插画",
  "draftId": "uuid",
  "confirmedFlavors": ["甜橙", "红茶"]
}
```

**确认要求**：模型调用会产生成本，必须由用户主动触发；不得自动调用。

## 审稿工具

**name**：`draft.review`

**用途**：检查文案是否混淆事实、过度夸张、重复历史表达或包含未确认风味。

**输出**：审稿提示、风险等级、建议修改点。

**确认要求**：无需确认；审稿判断必须写入 Agent 轨迹。

## 实现差异记录（2026-06-30）

- 已实现 `ToolRegistry`、`ToolAdapter`、`ToolCallPolicy` 和 `ToolCallRecorder` 的离线版本。
- 已注册小红书 `checkLogin/searchFeeds/getFeedDetail/fillPublish/clickPublish/saveDraft` 工具定义，其中 `fillPublish`、`clickPublish`、`saveDraft` 需要确认。
- `xiaohongshu.clickPublish` 在缺少确认时返回失败，不执行公开发布。
- `image.generate` 只有在 `userInitiated=true` 时返回生成图片候选。
- 当前小红书适配器是 Fake/离线结构化适配器，尚未调用本机 `xiaohongshu-skills` CLI；真实调用需按人工验证模板记录结果。
