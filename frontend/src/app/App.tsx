import { useEffect, useMemo, useState } from "react";
import { WorkbenchLayout } from "../components/layout/WorkbenchLayout";
import { ConversationComposer } from "../features/conversation/ConversationComposer";
import { ConversationThread } from "../features/conversation/ConversationThread";
import { DraftTabs } from "../features/conversation/DraftTabs";
import { CurrentRecordPanel } from "../features/tasting-form/CurrentRecordPanel";
import { AgentTracePanel } from "../features/agent-trace/AgentTracePanel";
import { RecoverableErrorBanner } from "../components/feedback/RecoverableErrorBanner";
import { createSession, fetchSnapshot, submitMessage, WorkbenchApiError } from "../services/workbenchApi";
import type { ApiError } from "../services/apiClient";
import type { WorkbenchSnapshot } from "../services/workbenchTypes";
import { readLocalResume, saveLocalResume } from "../stores/localResumeStore";

export function App() {
  const [snapshot, setSnapshot] = useState<WorkbenchSnapshot | null>(null);
  const [input, setInput] = useState(() => readLocalResume().draftInput);
  const [error, setError] = useState<ApiError | null>(null);
  const [isSubmitting, setSubmitting] = useState(false);
  const [isLoading, setLoading] = useState(true);

  useEffect(() => {
    const resume = readLocalResume();
    fetchSnapshot(resume.lastSessionId)
      .then((next) => {
        setSnapshot(next);
        setError(next.lastError);
        if (next.sessionId) {
          saveLocalResume({ lastSessionId: next.sessionId, lastKnownStatus: next.status });
        }
      })
      .catch((caught) => {
        const apiError = caught instanceof WorkbenchApiError ? caught.apiError : fallbackError();
        setError(apiError);
      })
      .finally(() => setLoading(false));
  }, []);

  const traceSteps = useMemo(() => {
    if (!snapshot) {
      return ["等待工作台快照"];
    }
    return [
      snapshot.sessionId ? "会话已创建" : "等待创建会话",
      snapshot.conversation.length > 0 ? "消息已进入 Agent 编排" : "等待用户输入",
      snapshot.draftTabs.length > 0 ? "三版草稿已生成" : "待补充事实后生成草稿"
    ];
  }, [snapshot]);

  async function handleCreateSession() {
    setSubmitting(true);
    setError(null);
    try {
      const next = await createSession();
      setSnapshot(next);
      saveLocalResume({ lastSessionId: next.sessionId, lastKnownStatus: next.status });
    } catch (caught) {
      setError(caught instanceof WorkbenchApiError ? caught.apiError : fallbackError());
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSubmit() {
    if (!snapshot?.sessionId || isSubmitting) {
      return;
    }
    setSubmitting(true);
    setError(null);
    saveLocalResume({ lastSessionId: snapshot.sessionId, draftInput: input, lastKnownStatus: snapshot.status });
    try {
      const next = await submitMessage(snapshot.sessionId, { content: input });
      setSnapshot(next);
      setInput("");
      saveLocalResume({ lastSessionId: next.sessionId, draftInput: "", lastKnownStatus: next.status });
    } catch (caught) {
      const apiError = caught instanceof WorkbenchApiError ? caught.apiError : fallbackError();
      setError(apiError);
      saveLocalResume({ draftInput: input, lastKnownStatus: "ERROR_RECOVERABLE" });
    } finally {
      setSubmitting(false);
    }
  }

  const main = (
    <section className="conversation-card" aria-label="对话创作区">
      <div className="mode-row">
        <span>显式工作流</span>
        <span>模型自主工具调用</span>
      </div>
      <h1>{snapshot?.heroQuestion ?? "今天喝了什么咖啡？"}</h1>
      {error ? <RecoverableErrorBanner error={error} onRetry={snapshot?.sessionId ? handleSubmit : handleCreateSession} onRecreate={handleCreateSession} /> : null}
      {!snapshot?.sessionId ? (
        <button className="primary-action" type="button" disabled={isSubmitting || isLoading} onClick={handleCreateSession}>
          开始记录
        </button>
      ) : null}
      <ConversationThread messages={snapshot?.conversation ?? []} />
      <DraftTabs drafts={snapshot?.draftTabs ?? []} />
      <ConversationComposer
        value={input}
        disabled={!snapshot?.sessionId || isSubmitting}
        isSubmitting={isSubmitting}
        onChange={(value) => {
          setInput(value);
          saveLocalResume({ draftInput: value, lastSessionId: snapshot?.sessionId ?? null, lastKnownStatus: snapshot?.status ?? "UNKNOWN" });
        }}
        onSubmit={handleSubmit}
      />
    </section>
  );

  return (
    <WorkbenchLayout
      main={main}
      recordPanel={<CurrentRecordPanel summary={snapshot?.recordSummary} />}
      agentTrace={<AgentTracePanel steps={traceSteps} />}
    />
  );
}

export default App;

function fallbackError(): ApiError {
  return {
    code: "SERVICE_UNAVAILABLE",
    category: "RETRYABLE",
    message: "本地服务暂时不可用，已保留你的输入。",
    recoverable: true,
    nextActions: ["CHECK_LOCAL_SERVICE", "RETRY"],
    details: {}
  };
}
