import { useEffect, useState } from "react";
import type { DraftTab } from "../../services/workbenchTypes";

const STYLE_LABELS: Record<DraftTab["style"], string> = {
  RESTRAINED: "克制版",
  EXAGGERATED: "夸张版",
  SHARP_REVIEW: "锐评版"
};

export function DraftTabs({ drafts, selectedDraftId }: { drafts: DraftTab[]; selectedDraftId?: string | null }) {
  const [activeStyle, setActiveStyle] = useState<DraftTab["style"]>("RESTRAINED");
  useEffect(() => {
    const selectedDraft = drafts.find((draft) => draft.draftId === selectedDraftId);
    if (selectedDraft) {
      setActiveStyle(selectedDraft.style);
    }
  }, [drafts, selectedDraftId]);

  if (drafts.length === 0) {
    return <section className="draft-tabs empty-drafts">补充豆子、冲煮参数和文案风格后，会显示三版草稿。</section>;
  }
  const orderedDrafts = orderDrafts(drafts);
  const activeDraft = orderedDrafts.find((draft) => draft.style === activeStyle) ?? orderedDrafts[0];
  const isSelected = selectedDraftId === activeDraft.draftId;
  return (
    <section className="draft-tabs" aria-label="文案草稿">
      <div className="tab-row">
        {orderedDrafts.map((draft) => (
          <button key={draft.draftId} type="button" className={draft.style === activeDraft.style ? "active" : ""} onClick={() => setActiveStyle(draft.style)}>
            {STYLE_LABELS[draft.style]}
          </button>
        ))}
      </div>
      <article>
        {isSelected ? <p className="selected-draft-note">已选择此版作为后续修改基础</p> : null}
        <h2>{activeDraft.title}</h2>
        <p>{activeDraft.body}</p>
        {activeDraft.tags.length ? (
          <div className="tag-row" aria-label="草稿标签">
            {activeDraft.tags.map((tag) => <span key={tag}>#{tag}</span>)}
          </div>
        ) : null}
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

function orderDrafts(drafts: DraftTab[]): DraftTab[] {
  const order: DraftTab["style"][] = ["RESTRAINED", "EXAGGERATED", "SHARP_REVIEW"];
  return [...drafts].sort((left, right) => order.indexOf(left.style) - order.indexOf(right.style));
}
