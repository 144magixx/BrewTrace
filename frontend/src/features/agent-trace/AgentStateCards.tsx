import type { AgentStateSnapshot, CandidateMemory, ConfirmedFact, ContextItem, PendingAssociation, RiskLevel, SendStatus } from "../../services/workbenchTypes";
import { CapabilityBoundaryPanel } from "./CapabilityBoundaryPanel";
import { ContextPreviewPanel } from "./ContextPreviewPanel";
import { ModelOutputPanel } from "./ModelOutputPanel";

export function AgentStateCards({ state }: { state?: AgentStateSnapshot }) {
  if (!state) {
    return (
      <section className="agent-state-panel" aria-label="Agent 状态卡片">
        <h2>Agent 状态卡片</h2>
        <p className="empty-state">当前没有可发送上下文。</p>
      </section>
    );
  }
  return (
    <section className="agent-state-panel" aria-label="Agent 状态卡片">
      <h2>Agent 状态卡片</h2>
      <div className="agent-status-grid">
        {state.statusCards.map((card) => (
          <article className={`agent-state-card card-${card.type.toLowerCase().replaceAll("_", "-")}`} key={card.id}>
            <h3>{card.title}</h3>
            <p>{card.summary}</p>
            <small>{card.sourceLabel}</small>
            <div className="badge-row">
              <SendBadge status={card.sendStatus} />
              <RiskBadge risk={card.riskLevel} />
            </div>
          </article>
        ))}
      </div>
      {state.modelOutput?.messageType ? (
        <div className="agent-section">
          <h3>模型路由</h3>
          <p>{state.modelOutput.messageType}</p>
          <small>{state.modelOutput.talk}</small>
        </div>
      ) : null}
      <StateSection title="当前会话上下文" items={state.contextItems} empty="当前没有可发送上下文。" renderItem={renderContextItem} />
      <StateSection title="已确认事实" items={state.confirmedFacts} empty="暂无已确认事实" renderItem={renderConfirmedFact} />
      <StateSection title="待确认联想" items={state.pendingAssociations} empty="暂无待确认联想" renderItem={renderPendingAssociation} />
      <StateSection title="候选记忆" items={state.candidateMemories} empty="暂无候选记忆" renderItem={renderCandidateMemory} />
      <ContextPreviewPanel preview={state.contextPreview} />
      <ModelOutputPanel output={state.modelOutput} checks={state.factBoundaryChecks} />
      <CapabilityBoundaryPanel boundary={state.capabilityBoundary} />
    </section>
  );
}

function StateSection<T>({ title, items, empty, renderItem }: { title: string; items: T[]; empty: string; renderItem: (item: T) => JSX.Element }) {
  return (
    <div className="agent-section">
      <h3>{title}</h3>
      {items.length > 0 ? <ul>{items.map(renderItem)}</ul> : <p className="empty-state">{empty}</p>}
    </div>
  );
}

function renderContextItem(item: ContextItem) {
  return (
    <li key={item.id}>
      <span>{item.content}</span>
      <small>{item.sourceType} · {item.confirmationStatus}</small>
      <SendBadge status={item.sendStatus} />
    </li>
  );
}

function renderConfirmedFact(fact: ConfirmedFact) {
  return (
    <li key={fact.id}>
      <span>{fact.value}</span>
      <small>{fact.factType}</small>
      <SendBadge status={fact.sendStatus} />
    </li>
  );
}

function renderPendingAssociation(association: PendingAssociation) {
  return (
    <li key={association.id}>
      <span>{association.value}</span>
      <small>{association.reason}</small>
      <SendBadge status={association.sendStatus} />
    </li>
  );
}

function renderCandidateMemory(memory: CandidateMemory) {
  return (
    <li key={memory.id}>
      <span>{memory.title}：{memory.content}</span>
      <small>{memory.sourceBoundary}</small>
      <small>{memory.reason} · {memory.similarityLabel}</small>
      <SendBadge status={memory.sendStatus} />
    </li>
  );
}

function SendBadge({ status }: { status: SendStatus }) {
  return <strong className={`send-status send-${status.toLowerCase().replaceAll("_", "-")}`}>{status}</strong>;
}

function RiskBadge({ risk }: { risk: RiskLevel }) {
  return <strong className={`risk-level risk-${risk.toLowerCase()}`}>{risk === "HIGH" ? "高风险" : risk}</strong>;
}
