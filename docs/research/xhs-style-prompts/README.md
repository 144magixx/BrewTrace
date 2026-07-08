# 小红书咖啡风格提示词库

## 文件结构

- `custom/custom-style-prompt.md`：Captain_Orange_ / 柑橘科科长。
- `restrained/restrained-style-prompt.md`：1169409996 / 斯慕吉Smoothie + 119809953 / Apple，对齐代码风格 `RESTRAINED`。
- `sharp-review/sharp-review-style-prompt.md`：5543006937 / 北国清韵，对齐代码风格 `SHARP_REVIEW`。
- `exaggerated/exaggerated-style-prompt.md`：182627410 / 做作的Morpheus，对齐代码风格 `EXAGGERATED`。
- 各目录的 `sample-index.json`：主页标题与代表样本索引。
- 各目录的 `detail-sample-index.json`：详情正文索引，包含标题、正文预览、标签、时间、互动数与 JSON 文件路径。

## 抓取说明

原始 JSON 保存在 `/Users/minyuwei/Documents/xhs/docs/research/xhs-style-samples/`：

- `raw/`：搜索结果、主页列表、当前登录用户解析结果。
- `details/`：逐篇详情抓取结果，按账号保存。
- `logs/`：详情抓取过程日志。
- `detail-fetch-all-manifest.json`：详情抓取清单。
- `crawl-summary.json`：账号样本量、时间范围和详情成功数。
- `details/**/*.failed-*`：抓取过程中失败的临时返回，共 11 个；对应正文后来均已成功补齐，不计入详情缺口。

## 最终样本量

- Captain_Orange_ / 柑橘科科长：主页 338 条，详情成功 338 条，时间范围 2023-09-02 至 2026-06-28。
- 1169409996 / 斯慕吉Smoothie：详情成功 241 条，时间范围 2024-06-16 至 2026-06-30。
- 119809953 / Apple：详情成功 62 条，时间范围 2026-04-09 至 2026-06-30。
- 5543006937 / 北国清韵：详情成功 18 条，已覆盖公开可抓取全集，时间范围 2024-03-11 至 2026-04-03。
- 182627410 / 做作的Morpheus：详情成功 61 条，时间范围 2023-03-10 至 2026-06-30。
- 合计详情正文：720 条。

## 风格目录映射

- `custom/`：自定义风格，使用 Captain_Orange_ 全量样本。
- `restrained/`：中立克制风格，合并 1169409996 与 119809953 样本。
- `sharp-review/`：锐评风格，使用 5543006937 样本。
- `exaggerated/`：夸张菜单感风格，使用 182627410 样本。

## 使用建议

- 写个人账号文案时优先使用 `custom/custom-style-prompt.md`。
- 需要客观品鉴、参数完整、语气克制时使用 `restrained/restrained-style-prompt.md`。
- 需要咖啡圈现象批评时使用 `sharp-review/sharp-review-style-prompt.md`，并遵守文档中的风险边界。
- 需要风味密度高、菜单式、感官铺陈强的表达时使用 `exaggerated/exaggerated-style-prompt.md`。
