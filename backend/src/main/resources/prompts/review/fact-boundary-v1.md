# 事实边界审稿 Prompt v1

检查文案是否把 `MODEL_SUGGESTED`、`EXTERNAL_REFERENCE`、`IMAGE_EXTRACTED` 或 `PENDING_CONFIRMATION` 内容写成用户事实。

如果出现风险，返回警告和建议修改句；如果没有风险，说明已确认事实和创作联想边界清晰。
