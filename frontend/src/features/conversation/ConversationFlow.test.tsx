import { ConversationComposer } from "./ConversationComposer";
import { renderConversationThread } from "./ConversationThread";
import { renderDraftTabs } from "./DraftTabs";

export function conversationFlowFixture(): string {
  const composer = ConversationComposer("今天喝了一支水洗埃塞，有柑橘和红茶感");
  const thread = renderConversationThread([
    { role: "USER", content: composer.value },
    { role: "ASSISTANT", content: "还需要确认豆子信息、冲煮参数和你想要的文案风格。" }
  ]);
  const drafts = renderDraftTabs([
    { style: "RESTRAINED", title: "一杯干净的水洗埃塞", body: "未确认风味只作为联想" }
  ]);
  return `${composer.placeholder}\n${thread}\n${drafts}`;
}
