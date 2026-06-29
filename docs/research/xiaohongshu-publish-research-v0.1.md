# 小红书发布能力调研 v0.1

## 1. 调研目标

确认咖啡品鉴创作 Agent 是否可以在用户确认后调用小红书发布能力，实现“一键发布笔记”。

## 2. 调研日期

2026-06-29

## 3. 初步结论

截至本次调研，未在公开官方资料中确认个人创作者可用的“笔记一键发布 API”。

但本机已发现可用的本地自动化 skill：`xiaohongshu-skills`。它提供小红书认证、搜索发现、内容发布、互动和复合运营能力，后续本项目的小红书抓帖和发帖能力优先基于该 skill 集成。

较稳妥的产品策略：

- MVP 保留“发布包生成”：标题、正文、标签、图片、封面文案。
- 在用户确认后，可通过 `xiaohongshu-skills` 尝试填写发布页或执行发布。
- 发布能力仍需抽象成工具接口，避免业务逻辑直接依赖具体脚本。
- 官方 API 合规性仍需继续调研；`xiaohongshu-skills` 属于本地自动化执行通道，不等同于官方开放 API。
- 任何发布、评论、互动类操作都必须经过用户明确确认。

## 4. 已查看资料

公开资料显示，小红书开放平台更偏向商家、应用、服务市场和电商场景。

参考来源：

- 小红书开放平台 API 文档：https://xiaohongshu.apifox.cn/
- 小红书开放平台发布服务说明：https://xiaohongshu.apifox.cn/doc-2810945
- 小红书创作服务平台：https://creator.xiaohongshu.com/

说明：

- 以上资料可以证明平台存在开放平台和创作服务入口。
- 但本次没有确认到面向个人创作者的公开笔记发布 API。
- “发布服务”相关资料需要进一步确认其适用对象、是否仅面向服务市场或商家生态。

## 5. 本地 skill 调研

### 5.1 skill 位置

本机存在以下 skill：

- `/Users/minyuwei/.codex/skills/xiaohongshu-skills/SKILL.md`
- `/Users/minyuwei/.codex/skills/xiaohongshu-skills/skills/xhs-publish/SKILL.md`
- `/Users/minyuwei/.codex/skills/xiaohongshu-skills/skills/xhs-explore/SKILL.md`

该 skill 的能力范围包括：

- `xhs-auth`：登录、检查登录、切换账号。
- `xhs-publish`：图文、视频、长文发布。
- `xhs-explore`：搜索笔记、查看详情、获取用户资料。
- `xhs-interact`：评论、回复、点赞、收藏。
- `xhs-content-ops`：复合运营工作流。

### 5.2 强制约束

根据 skill 文档，所有小红书操作只能通过该 skill 自带脚本执行：

```bash
python scripts/cli.py <子命令>
```

约束：

- 操作前应检查登录状态。
- 需要已登录且运行中的 Chrome。
- 发布和评论必须经过用户确认。
- 文件路径必须使用绝对路径。
- CLI 输出为 JSON。
- 操作频率不能过高，需要考虑风控。
- 不使用其他小红书 MCP、Go 工具或外部自动化方案。

### 5.3 抓帖能力

后续“小红书类似内容检索”优先使用 `xhs-explore`。

可用命令：

```bash
python scripts/cli.py search-feeds --keyword "关键词"
python scripts/cli.py get-feed-detail --feed-id FEED_ID --xsec-token XSEC_TOKEN
```

可支持：

- 搜索类似咖啡、处理法、烘焙商或风味关键词。
- 获取笔记详情和评论。
- 为文案生成提供外部参考。

注意：

- 外部内容只作为灵感参考，不直接复制。
- `feed_id` 和 `xsec_token` 必须配对使用。
- 批量获取详情需要降低频率，避免触发风控。

### 5.4 发帖能力

后续发布优先使用 `xhs-publish`。

推荐分步方式：

```bash
python scripts/cli.py fill-publish \
  --title-file /abs/path/title.txt \
  --content-file /abs/path/content.txt \
  --images "/abs/path/pic1.jpg"

python scripts/cli.py click-publish
```

流程建议：

1. Agent 生成发布包。
2. 用户确认标题、正文、图片和标签。
3. 调用 `fill-publish` 填写图文发布页，但不发布。
4. 用户在浏览器中确认预览。
5. 用户明确确认后调用 `click-publish`。
6. 如果用户取消，调用保存草稿能力，不直接关闭页面。

关键限制：

- 图文发布必须有图片。
- 视频发布必须有视频，图片和视频不可混合。
- 标题长度需要控制在小红书限制内。
- 标题和正文应写入 UTF-8 临时文件，不在命令行内联中文正文。

## 6. 可能实现路径

### 6.1 官方 API，推荐但待确认

前提：

- 官方明确提供发布笔记接口。
- 支持个人创作者或当前账号类型。
- 有清晰鉴权、审核和调用限制。

优点：

- 合规。
- 稳定性最好。
- 适合长期维护。

风险：

- 可能并不开放给个人创作者。
- 可能需要企业、服务商或商家资质。

### 6.2 发布包生成，MVP 必做

系统生成：

- 标题。
- 正文。
- 标签。
- 图片或封面建议。
- 发布前检查清单。

用户手动复制到小红书发布。

优点：

- 合规风险低。
- 能先验证核心文案价值。
- 工程成本低。

缺点：

- 不是完整一键发布。
- 用户仍需手动操作。

### 6.3 基于 `xiaohongshu-skills` 的自动化，后续推荐

通过本地 `xiaohongshu-skills` 封装的小红书自动化能力完成搜索、详情获取、发布页填写和发布。

优点：

- 能覆盖抓帖和发帖核心诉求。
- 已有明确 CLI 边界，便于接入 Agent 工具系统。
- 支持分步发布，适合发布前人工确认。

风险：

- 仍然依赖页面自动化和登录态。
- 页面结构变化可能导致脚本失效。
- 登录态、验证码、风控不可控。
- 需要严格控制频率，避免异常行为。

当前建议：

- MVP 可以先接入 `search-feeds` 和 `fill-publish`。
- `click-publish` 必须放在用户二次确认之后。
- 真实发布失败时降级为发布包和草稿。

### 6.4 直接浏览器自动化，不推荐

不绕过 `xiaohongshu-skills` 另写 Playwright、Selenium 或其他自动化逻辑。

原因：

- skill 已经定义统一入口和风控约束。
- 多套自动化实现会增加维护成本和误操作风险。

### 6.5 移动端自动化，不推荐

通过移动端自动化控制 App。

风险更高：

- 环境复杂。
- 容易触发风控。
- 维护成本高。
- 不适合作为稳定产品能力。

## 7. 发布工具接口预留

MVP 应预留工具协议，并将 `xiaohongshu-skills` 作为默认实现候选。

建议接口：

```json
{
  "toolName": "xiaohongshu.publish",
  "requiresConfirmation": true,
  "input": {
    "title": "string",
    "content": "string",
    "tags": ["string"],
    "imagePaths": ["string"]
  },
  "output": {
    "status": "PACKAGE_CREATED | FORM_FILLED | DRAFT_SAVED | PUBLISHED | FAILED | UNSUPPORTED",
    "message": "string",
    "url": "string?"
  }
}
```

MVP 中该工具至少支持 `PACKAGE_CREATED`。接入 `xiaohongshu-skills` 后，应支持 `FORM_FILLED` 和 `PUBLISHED`。

抓帖工具协议：

```json
{
  "toolName": "xiaohongshu.search",
  "requiresConfirmation": false,
  "input": {
    "keyword": "string",
    "sortBy": "综合 | 最新 | 最多点赞 | 最多评论 | 最多收藏",
    "noteType": "不限 | 视频 | 图文"
  },
  "output": {
    "status": "SUCCESS | FAILED | LOGIN_REQUIRED",
    "feeds": []
  }
}
```

## 8. 学习重点

这个调研模块适合学习：

- Agent 工具能力不是都必须马上实现，可以先抽象协议。
- 外部平台能力要区分“技术上能做”和“合规上能做”。
- 高风险工具必须要求用户确认。
- 工具失败时要有降级方案。
- 本地 skill 可以作为 Agent 工具实现，但业务层应依赖抽象接口，而不是直接散落脚本调用。

## 9. 后续待办

- 继续查找小红书开放平台是否有内容发布相关申请入口。
- 确认创作服务平台是否提供开发者能力。
- 研究小红书平台规则中对自动化发布、第三方工具的限制。
- 如果后续发现官方 API，补充鉴权、权限、限流、发布审核和错误码设计。
- 阅读 `xhs-auth` 和 `xhs-content-ops` 子技能，补充登录、复合运营和异常处理细节。
- 在架构设计中把 `xiaohongshu.search` 和 `xiaohongshu.publish` 作为默认工具实现候选。
