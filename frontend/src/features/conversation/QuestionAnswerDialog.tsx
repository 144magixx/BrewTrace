import { useState } from "react";
import type { ConversationAnswerOption } from "../../services/workbenchTypes";

type QuestionAnswerDialogProps = {
  question: string;
  options: ConversationAnswerOption[];
  disabled: boolean;
  onSelect: (content: string) => void;
  onCustomSubmit: (content: string) => void;
};

export function QuestionAnswerDialog({ question, options, disabled, onSelect, onCustomSubmit }: QuestionAnswerDialogProps) {
  const [customAnswer, setCustomAnswer] = useState("");
  const trimmedCustomAnswer = customAnswer.trim();

  return (
    <section className="dialog-backdrop question-answer-dialog" role="dialog" aria-modal="true" aria-labelledby="question-answer-title">
      <div className="question-answer-panel">
        <div className="dialog-heading">
          <span>继续确认</span>
          <h2 id="question-answer-title">{question}</h2>
        </div>
        <div className="answer-option-grid" aria-label="备选回答">
          {options.map((option) => (
            <button
              key={option.id}
              type="button"
              disabled={disabled}
              onClick={() => onSelect(option.content)}
            >
              <strong>{option.label}</strong>
              <span>{option.content}</span>
            </button>
          ))}
        </div>
        <form
          className="custom-answer-form"
          onSubmit={(event) => {
            event.preventDefault();
            if (trimmedCustomAnswer.length > 0) {
              onCustomSubmit(trimmedCustomAnswer);
            }
          }}
        >
          <label htmlFor="custom-answer">自定义</label>
          <div className="custom-answer-row">
            <input
              id="custom-answer"
              value={customAnswer}
              disabled={disabled}
              placeholder="输入自己的回答"
              onChange={(event) => setCustomAnswer(event.target.value)}
            />
            <button type="submit" disabled={disabled || trimmedCustomAnswer.length === 0}>发送</button>
          </div>
        </form>
      </div>
    </section>
  );
}
