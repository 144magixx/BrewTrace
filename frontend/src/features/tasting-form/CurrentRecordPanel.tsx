import type { RecordSummary } from "../../services/workbenchTypes";

export type CurrentRecordPanelState = {
  beanFields: string[];
  brewFields: string[];
  flavorFields: string[];
  sensorySummary?: string;
  pendingConfirmations: string[];
};

export function CurrentRecordPanel({ summary }: { summary?: RecordSummary }) {
  return (
    <section className="summary-panel">
      <h2>当前记录</h2>
      <SummaryList title="已确认事实" items={summary?.confirmedFacts ?? []} empty="还没有确认事实" />
      <SummaryList title="待回答问题" items={summary?.pendingQuestions ?? []} empty="暂无待回答问题" />
      <SummaryList title="待确认联想" items={summary?.suggestedFlavors ?? []} empty="暂无联想候选" />
      <SummaryList title="事实边界" items={summary?.factBoundaryNotes ?? []} empty="生成草稿后展示边界说明" />
      <p className="draft-status">草稿状态：{summary?.draftStatus ?? "HIDDEN"}</p>
    </section>
  );
}

function SummaryList({ title, items, empty }: { title: string; items: string[]; empty: string }) {
  return (
    <div className="summary-block">
      <h3>{title}</h3>
      {items.length > 0 ? <ul>{items.map((item) => <li key={item}>{item}</li>)}</ul> : <p>{empty}</p>}
    </div>
  );
}

export function renderCurrentRecordPanel(state: CurrentRecordPanelState): string {
  return [
    `咖啡豆:${state.beanFields.join(",")}`,
    `冲煮:${state.brewFields.join(",")}`,
    `风味:${state.flavorFields.join(",")}`,
    `感官:${state.sensorySummary ?? "未填写"}`,
    `待确认:${state.pendingConfirmations.join(",")}`
  ].join("\n");
}
