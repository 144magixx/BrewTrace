export type ConversationComposerState = {
  placeholder: string;
  value: string;
};

type ConversationComposerProps = {
  value: string;
  disabled: boolean;
  onChange: (value: string) => void;
  onSubmit: () => void;
};

export function ConversationComposer(props: ConversationComposerProps) {
  return (
    <form className="composer" autoComplete="off" onSubmit={(event) => { event.preventDefault(); props.onSubmit(); }}>
      <label className="sr-only" htmlFor="coffee-input">今天喝了什么咖啡？</label>
      <input
        id="coffee-input"
        name="coffee-input-no-history"
        autoComplete="off"
        value={props.value}
        disabled={props.disabled}
        placeholder="输入今天这杯的体验"
        onChange={(event) => props.onChange(event.target.value)}
      />
      <button type="submit" disabled={props.disabled || props.value.trim().length === 0}>
        <span>发送</span>
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M5 12h12" />
          <path d="M13 6l6 6-6 6" />
        </svg>
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
