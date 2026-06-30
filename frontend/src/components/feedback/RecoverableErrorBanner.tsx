import type { ApiError } from "../../services/apiClient";

type RecoverableErrorBannerProps = {
  error: ApiError;
  onRetry: () => void;
  onRecreate: () => void;
};

export function RecoverableErrorBanner({ error, onRetry, onRecreate }: RecoverableErrorBannerProps) {
  return (
    <section className="error-banner" role="alert">
      <strong>{error.category}</strong>
      <p>{error.message}</p>
      <div className="next-actions">
        {error.nextActions.map((action) => <span key={action}>{action}</span>)}
      </div>
      <div className="error-actions">
        <button type="button" onClick={onRetry}>重试</button>
        <button type="button" onClick={onRecreate}>重新创建会话</button>
      </div>
    </section>
  );
}
