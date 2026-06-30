import type { WebConversationMessage } from "../../services/workbenchTypes";

export type ConversationBubble = Pick<WebConversationMessage, "role" | "content">;

export function ConversationThread({ messages }: { messages: WebConversationMessage[] }) {
  if (messages.length === 0) {
    return <p className="empty-thread">创建会话后，把今天这杯咖啡的体验告诉我。</p>;
  }
  return (
    <ol className="message-thread">
      {messages.map((message) => (
        <li key={message.id} className={`message message-${message.role.toLowerCase()}`}>
          <span>{message.role === "USER" ? "你" : "助手"}</span>
          <p>{message.content}</p>
          <small>{message.sourceType}</small>
        </li>
      ))}
    </ol>
  );
}

export function renderConversationThread(messages: ConversationBubble[]): string {
  return messages.map(message => `${message.role}:${message.content}`).join("\n");
}
