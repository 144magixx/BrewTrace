export function ClearSessionConfirmDialog({ onCancel, onConfirm, isSubmitting }: { onCancel: () => void; onConfirm: () => void; isSubmitting: boolean }) {
  return (
    <div className="dialog-backdrop" role="presentation">
      <section className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="clear-session-title">
        <h2 id="clear-session-title">清空当前会话？</h2>
        <p>这会清空当前会话可见状态和浏览器恢复草稿，不删除长期记忆、历史归档或外部平台数据。</p>
        <div className="dialog-actions">
          <button type="button" onClick={onCancel} disabled={isSubmitting}>取消</button>
          <button className="danger-action" type="button" onClick={onConfirm} disabled={isSubmitting}>确认清空</button>
        </div>
      </section>
    </div>
  );
}
