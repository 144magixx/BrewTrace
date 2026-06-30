import { useState } from "react";
import type { DraftTab } from "../../services/workbenchTypes";

const STYLE_LABELS: Record<DraftTab["style"], string> = {
  RESTRAINED: "克制版",
  EXAGGERATED: "夸张版",
  SHARP_REVIEW: "锐评版"
};

export function DraftTabs({ drafts }: { drafts: DraftTab[] }) {
  const [activeStyle, setActiveStyle] = useState<DraftTab["style"]>("RESTRAINED");
  if (drafts.length === 0) {
    return <section className="draft-tabs empty-drafts">补充豆子、冲煮参数和文案风格后，会显示三版草稿。</section>;
  }
  const activeDraft = drafts.find((draft) => draft.style === activeStyle) ?? drafts[0];
  return (
    <section className="draft-tabs" aria-label="文案草稿">
      <div className="tab-row">
        {drafts.map((draft) => (
          <button key={draft.draftId} type="button" className={draft.style === activeDraft.style ? "active" : ""} onClick={() => setActiveStyle(draft.style)}>
            {STYLE_LABELS[draft.style]}
          </button>
        ))}
      </div>
      <article>
        <h2>{activeDraft.title}</h2>
        <p>{activeDraft.body}</p>
        <ul>
          {activeDraft.factBoundaryNotes.map((note) => <li key={note}>{note}</li>)}
        </ul>
      </article>
    </section>
  );
}

export function renderDraftTabs(tabs: Pick<DraftTab, "style" | "title" | "body">[]): string {
  return tabs.map(tab => `${tab.style}:${tab.title}`).join("|");
}
