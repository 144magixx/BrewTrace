import type { ContextPreview, SendStatus } from "../../services/workbenchTypes";
import { redactSensitiveText } from "../../services/workbenchApi";

export function ContextPreviewPanel({ preview }: { preview: ContextPreview }) {
  return (
    <section className="context-preview-panel" aria-label="上下文预览">
      <div className="panel-heading">
        <h3>上下文预览</h3>
        <span>{preview.willSendCount} 项将发送</span>
        <span>{preview.excludedCount} 项排除</span>
      </div>
      <p>{preview.boundaryNote}</p>
      {preview.sections.map((section) => (
        <div className="agent-section" key={section.sectionType}>
          <h4>{section.title}</h4>
          {section.items.length > 0 ? (
            <ul>
              {section.items.map((item) => (
                <li key={`${section.sectionType}-${item.content}`}>
                  <span>{item.content}</span>
                  <small>{item.sourceLabel}</small>
                  <StatusBadge status={item.sendStatus} />
                  {item.exclusionReason ? <small>{item.exclusionReason}</small> : null}
                </li>
              ))}
            </ul>
          ) : <p className="empty-state">暂无内容</p>}
        </div>
      ))}
      {preview.requestPreview ? <JsonPreview title={preview.requestPreview.label} rawJson={preview.requestPreview.rawJson} meta={preview.requestPreview.endpointPath} /> : null}
      {preview.responsePreview ? <JsonPreview title={preview.responsePreview.label} rawJson={preview.responsePreview.rawJson} meta={preview.responsePreview.modelName ?? preview.responsePreview.mode} /> : null}
    </section>
  );
}

function JsonPreview({ title, rawJson, meta }: { title: string; rawJson: string; meta: string }) {
  const redacted = redactSensitiveText(rawJson ?? "");
  const changed = redacted !== rawJson;
  return (
    <div className="agent-section json-preview-block">
      <div className="panel-heading">
        <h4>{title}</h4>
        <span>{meta}</span>
        {changed ? <span>内容已脱敏</span> : null}
      </div>
      <pre><code>{redacted}</code></pre>
    </div>
  );
}

function StatusBadge({ status }: { status: SendStatus }) {
  return (
    <strong className={`send-status send-${status.toLowerCase().replaceAll("_", "-")}`}>
      {sendStatusLabel(status)}
      <span className="sr-only">{status}</span>
    </strong>
  );
}

function sendStatusLabel(status: SendStatus): string {
  const labels: Record<SendStatus, string> = {
    WILL_SEND: "将发送",
    PAGE_ONLY: "不会发送",
    SEND_AFTER_CONFIRMATION: "待确认后发送",
    EXCLUDED: "不会发送"
  };
  return labels[status];
}
