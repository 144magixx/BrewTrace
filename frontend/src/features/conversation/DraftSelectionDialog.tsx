import { useMemo, useState } from "react";
import type { DraftTab } from "../../services/workbenchTypes";

const STYLE_LABELS: Record<DraftTab["style"], string> = {
  RESTRAINED: "克制版",
  EXAGGERATED: "夸张版",
  SHARP_REVIEW: "锐评版"
};

type DraftSelectionDialogProps = {
  drafts: DraftTab[];
  onClose: () => void;
  onSelect: (draftId: string) => void;
};

export function DraftSelectionDialog({ drafts, onClose, onSelect }: DraftSelectionDialogProps) {
  const orderedDrafts = useMemo(() => orderDrafts(drafts), [drafts]);
  const [activeDraftId, setActiveDraftId] = useState(orderedDrafts[0]?.draftId ?? "");
  const activeDraft = orderedDrafts.find((draft) => draft.draftId === activeDraftId) ?? orderedDrafts[0];

  if (!activeDraft) {
    return null;
  }

  return (
    <div className="dialog-backdrop draft-dialog-backdrop" role="presentation">
      <section className="draft-selection-dialog" role="dialog" aria-modal="true" aria-labelledby="draft-selection-title">
        <header className="draft-dialog-header">
          <div>
            <h2 id="draft-selection-title">选择文案草稿</h2>
            <p>模型已生成三版文案，先选一版作为后续修改基础。</p>
          </div>
          <button className="ghost-action" type="button" onClick={onClose} aria-label="关闭文案选择框">
            关闭
          </button>
        </header>

        <div className="draft-choice-row" role="tablist" aria-label="文案版本">
          {orderedDrafts.map((draft) => (
            <button
              key={draft.draftId}
              type="button"
              role="tab"
              aria-selected={draft.draftId === activeDraft.draftId}
              className={draft.draftId === activeDraft.draftId ? "active" : ""}
              onClick={() => setActiveDraftId(draft.draftId)}
            >
              {STYLE_LABELS[draft.style]}
            </button>
          ))}
        </div>

        <article className="draft-dialog-body">
          <h3>{activeDraft.title}</h3>
          <p>{activeDraft.body}</p>
          {activeDraft.tags.length ? (
            <div className="tag-row" aria-label="当前草稿标签">
              {activeDraft.tags.map((tag) => <span key={tag}>#{tag}</span>)}
            </div>
          ) : null}
          {activeDraft.factBoundaryNotes.length || activeDraft.reviewWarnings.length ? (
            <ul className="draft-boundary-list" aria-label="事实边界提醒">
              {[...activeDraft.factBoundaryNotes, ...activeDraft.reviewWarnings].map((note) => <li key={note}>{note}</li>)}
            </ul>
          ) : null}
        </article>

        <div className="dialog-actions">
          <button type="button" onClick={onClose}>稍后再选</button>
          <button className="primary-action" type="button" onClick={() => onSelect(activeDraft.draftId)}>
            选用此版
          </button>
        </div>
      </section>
    </div>
  );
}

function orderDrafts(drafts: DraftTab[]): DraftTab[] {
  const order: DraftTab["style"][] = ["RESTRAINED", "EXAGGERATED", "SHARP_REVIEW"];
  return [...drafts].sort((left, right) => order.indexOf(left.style) - order.indexOf(right.style));
}
