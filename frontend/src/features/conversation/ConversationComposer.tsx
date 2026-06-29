export type ConversationComposerState = {
  placeholder: string;
  value: string;
};

export function ConversationComposer(value = ""): ConversationComposerState {
  return {
    placeholder: "今天喝了什么咖啡？",
    value
  };
}
