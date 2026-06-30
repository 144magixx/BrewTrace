export type ConversationComposerState = {
  placeholder: string;
  value: string;
};

type ConversationComposerProps = {
  value: string;
  disabled: boolean;
  isSubmitting: boolean;
  onChange: (value: string) => void;
  onSubmit: () => void;
};

export function ConversationComposer(props: ConversationComposerProps) {
  return (
    <form className="composer" onSubmit={(event) => { event.preventDefault(); props.onSubmit(); }}>
      <label htmlFor="coffee-input">今天喝了什么咖啡？</label>
      <textarea
        id="coffee-input"
        value={props.value}
        disabled={props.disabled}
        placeholder="例如：今天喝了一支水洗埃塞，有柑橘和红茶感"
        onChange={(event) => props.onChange(event.target.value)}
      />
      <button type="submit" disabled={props.disabled || props.isSubmitting || props.value.trim().length === 0}>
        {props.isSubmitting ? "提交中" : "发送"}
      </button>
    </form>
  );
}

export function conversationComposerFixture(value = ""): ConversationComposerState {
  return {
    placeholder: "今天喝了什么咖啡？",
    value
  };
}
