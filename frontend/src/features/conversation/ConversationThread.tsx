import type { ModelOutputSnapshot, WebConversationMessage } from "../../services/workbenchTypes";

export type ConversationBubble = Pick<WebConversationMessage, "role" | "content">;

type ChatMessage = {
  id: string;
  role: "USER" | "ASSISTANT";
  content: string;
  statusLabel?: string;
  streaming?: boolean;
  waitingForFirstToken?: boolean;
};

type ConversationThreadProps = {
  messages: WebConversationMessage[];
  drafts: unknown[];
  modelOutput: ModelOutputSnapshot | null;
  streamingAssistant: { id: string; content: string; waitingForFirstToken: boolean } | null;
};

export function ConversationThread({ messages, drafts, modelOutput, streamingAssistant }: ConversationThreadProps) {
  const chatMessages = toChatMessages(messages, drafts, modelOutput, streamingAssistant);
  if (chatMessages.length === 0) {
    return (
      <section className="message-thread" aria-label="聊天记录">
        <ChatBubble
          message={{
            id: "assistant-empty",
            role: "ASSISTANT",
            content: "开始记录后，把今天这杯咖啡的体验发给我。"
          }}
        />
      </section>
    );
  }
  return (
    <section className="message-thread" aria-label="聊天记录">
      {chatMessages.map((message) => <ChatBubble key={message.id} message={message} />)}
    </section>
  );
}

export function renderConversationThread(messages: ConversationBubble[]): string {
  return messages.map(message => `${message.role}:${message.content}`).join("\n");
}

function toChatMessages(
  messages: WebConversationMessage[],
  drafts: unknown[],
  modelOutput: ModelOutputSnapshot | null,
  streamingAssistant: { id: string; content: string; waitingForFirstToken: boolean } | null
): ChatMessage[] {
  const conversation = messages
    .filter((message) => message.role === "USER" || message.role === "ASSISTANT")
    .map((message) => ({
      id: message.id,
      role: message.role as "USER" | "ASSISTANT",
      content: message.content
    }));
  const hasAssistantMessage = conversation.some((message) => message.role === "ASSISTANT");
  if (streamingAssistant) {
    conversation.push({
      id: streamingAssistant.id,
      role: "ASSISTANT",
      content: streamingAssistant.content,
      streaming: true,
      waitingForFirstToken: streamingAssistant.waitingForFirstToken
    });
  } else if (modelOutput?.recoverableError) {
    conversation.push({
      id: "assistant-error-" + modelOutput.recoverableError.createdAt,
      role: "ASSISTANT",
      content: modelOutput.recoverableError.message
    });
  } else if (modelOutput?.talk && !hasAssistantMessage) {
    conversation.push({
      id: "assistant-talk-" + modelOutput.generatedAt,
      role: "ASSISTANT",
      content: formatModelOutputForChat(modelOutput),
      statusLabel: modelOutput.statusLabel
    });
  }
  return conversation;
}

function formatModelOutputForChat(modelOutput: ModelOutputSnapshot): string {
  const parts = [modelOutput.talk].filter((item) => item.trim().length > 0);
  if (modelOutput.messageType === "CONVERSATION" && modelOutput.conversation?.questions.length) {
    parts.push([
      "我想确认：",
      ...modelOutput.conversation.questions.map((question, index) => `${index + 1}. ${question}`)
    ].join("\n"));
  }
  return parts.join("\n\n");
}

function ChatBubble({ message }: { message: ChatMessage }) {
  const isUser = message.role === "USER";
  return (
    <article className={`chat-row ${isUser ? "chat-row-user" : "chat-row-assistant"}`}>
      {!isUser ? <Avatar kind="assistant" /> : null}
      <div className="chat-bubble">
        {message.statusLabel ? <span className="chat-status">{message.statusLabel}</span> : null}
        {message.waitingForFirstToken ? (
          <span className="typing-indicator" aria-label="等待大模型首字回复">
            <span />
            <span />
            <span />
          </span>
        ) : (
          <p className={message.streaming ? "typing-text" : undefined}>
            {message.content}
            {message.streaming ? <span className="typing-cursor" aria-hidden="true" /> : null}
          </p>
        )}
      </div>
      {isUser ? <Avatar kind="user" /> : null}
    </article>
  );
}

function Avatar({ kind }: { kind: "assistant" | "user" }) {
  return (
    <span className={`chat-avatar chat-avatar-${kind}`} aria-label={kind === "assistant" ? "大模型" : "用户"} role="img">
      {kind === "assistant" ? <RobotIcon /> : <UserIcon />}
    </span>
  );
}

function RobotIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <rect x="5" y="8" width="14" height="10" rx="3" />
      <path d="M12 5v3" />
      <path d="M8.5 12h.01" />
      <path d="M15.5 12h.01" />
      <path d="M9 16h6" />
    </svg>
  );
}

function UserIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="8" r="4" />
      <path d="M5 20c1.4-4 4-6 7-6s5.6 2 7 6" />
    </svg>
  );
}
