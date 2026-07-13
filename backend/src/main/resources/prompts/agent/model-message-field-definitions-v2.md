messageType/talk/post/conversation/factUpdates/warnings

`factUpdates` 每项字段：
- `action`：稳定状态动作枚举。
- `boundary`：模型识别的事实来源边界。
- `value`：规范化后的事实或联想值。
- `sourceMessageId`：真实用户消息 ID。
- `sourceQuote`：来源用户消息中的连续原文片段。
- `reason`：模型语义判断理由。
- `targetItemId`：已有目标状态项 ID；新增时为 null。

