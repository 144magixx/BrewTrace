import type { FactBoundaryCheckResult, ModelOutputSnapshot, RiskLevel } from "../../services/workbenchTypes";

export function ModelOutputPanel({ output, checks }: { output: ModelOutputSnapshot | null; checks: FactBoundaryCheckResult[] }) {
  if (!output) {
    return (
      <section className="model-output-panel" aria-label="模型输出与事实边界">
        <h3>真实模型输出</h3>
        <p className="empty-state">输入后将调用 GPT-5.5 并展示结果。</p>
      </section>
    );
  }
  return (
    <section className="model-output-panel" aria-label="模型输出与事实边界">
      <div className="panel-heading">
        <h3>{output.outputType === "ERROR" ? "模型错误" : "真实模型输出"}</h3>
        <span>{output.outputType}</span>
        {output.modelName ? <span>{output.modelName.toUpperCase()}</span> : null}
      </div>
      <p className="boundary-note">{output.statusLabel}</p>
      <p className="boundary-note">{output.sourceBoundary}</p>
      {output.messageType ? <p className="boundary-note">路由：{output.messageType}</p> : null}
      {output.talk ? <blockquote>{output.talk}</blockquote> : null}
      {output.recoverableError ? <p className="error-inline">{output.recoverableError.message}</p> : null}
      {output.conversation ? (
        <div className="agent-section">
          <h4>继续追问</h4>
          {output.conversation.questions.length > 0 ? (
            <ul>{output.conversation.questions.map((question) => <li key={question}>{question}</li>)}</ul>
          ) : <p className="empty-state">暂无追问</p>}
          {output.conversation.answerOptions.length > 0 ? (
            <>
              <small>备选回答</small>
              <ul>
                {output.conversation.answerOptions.map((option) => (
                  <li key={option.id}>
                    <span>{option.label}</span>
                    <small>{option.content}</small>
                  </li>
                ))}
              </ul>
            </>
          ) : null}
          {output.conversation.warnings.length > 0 ? <small>{output.conversation.warnings.join("；")}</small> : null}
        </div>
      ) : null}
      {output.post?.variants?.length ? (
        <div className="variant-grid">
          {output.post.variants.map((variant) => (
            <article className="copy-variant" key={variant.style}>
              <div className="panel-heading">
                <h4>{variant.styleLabel}</h4>
                <span>{variant.style}</span>
              </div>
              <strong>{variant.title}</strong>
              <p>{variant.body}</p>
              <div className="tag-row">{variant.tags.map((tag) => <span key={tag}>#{tag}</span>)}</div>
              <FactUsageList title="事实依据" items={variant.factUsages} />
              <FactUsageList title="模型推断" items={variant.inferences} />
              <FactUsageList title="待确认联想" items={variant.pendingConfirmations} />
              {variant.warnings.length > 0 ? <small>{variant.warnings.join("；")}</small> : null}
            </article>
          ))}
        </div>
      ) : !output.talk && output.content ? <blockquote>{output.content}</blockquote> : null}
      <div className="agent-section">
        <h4>事实边界检查</h4>
        {checks.length > 0 ? (
          <ul>
            {checks.map((check) => (
              <li key={check.id}>
                <span>{check.expression}</span>
                <small>{check.basisType} · {check.recommendedAction}</small>
                <strong className={`risk-level risk-${check.riskLevel.toLowerCase()}`}>{riskLabel(check.riskLevel)}</strong>
                <small>{check.message}</small>
              </li>
            ))}
          </ul>
        ) : <p className="empty-state">暂无事实边界检查结果</p>}
      </div>
    </section>
  );
}

function FactUsageList({ title, items }: { title: string; items: NonNullable<ModelOutputSnapshot["variants"]>[number]["factUsages"] }) {
  if (!items || items.length === 0) {
    return null;
  }
  return (
    <div className="fact-usage-group">
      <small>{title}</small>
      <ul>
        {items.map((item) => (
          <li key={`${title}-${item.expression}-${item.sourceReference}`}>
            <span>{item.expression}</span>
            <small>{item.basisType} · {item.sourceReference}</small>
          </li>
        ))}
      </ul>
    </div>
  );
}

function riskLabel(riskLevel: RiskLevel): string {
  return riskLevel === "HIGH" ? "高风险" : riskLevel === "WARNING" ? "需确认" : riskLevel;
}
