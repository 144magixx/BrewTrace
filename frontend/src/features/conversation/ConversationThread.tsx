export type ConversationBubble = {
  role: "USER" | "ASSISTANT";
  content: string;
};

export function renderConversationThread(messages: ConversationBubble[]): string {
  return messages.map(message => `${message.role}:${message.content}`).join("\n");
}
