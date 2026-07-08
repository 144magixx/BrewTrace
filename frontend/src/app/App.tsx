import { useEffect, useMemo, useState } from "react";
import { WorkbenchLayout } from "../components/layout/WorkbenchLayout";
import { ConversationComposer } from "../features/conversation/ConversationComposer";
import { ConversationThread } from "../features/conversation/ConversationThread";
import { DraftTabs } from "../features/conversation/DraftTabs";
import { DraftSelectionDialog } from "../features/conversation/DraftSelectionDialog";
import { QuestionAnswerDialog } from "../features/conversation/QuestionAnswerDialog";
import { CurrentRecordPanel } from "../features/tasting-form/CurrentRecordPanel";
import { AgentTracePanel } from "../features/agent-trace/AgentTracePanel";
import { AgentStateCards } from "../features/agent-trace/AgentStateCards";
import { RecoverableErrorBanner } from "../components/feedback/RecoverableErrorBanner";
import { ClearSessionConfirmDialog } from "../components/feedback/ClearSessionConfirmDialog";
import { clearSession, createSession, fetchSnapshot, submitMessageStream, WorkbenchApiError } from "../services/workbenchApi";
import type { ApiError } from "../services/apiClient";
import type { WebConversationMessage, WorkbenchSnapshot } from "../services/workbenchTypes";
import { clearSessionResume, readLocalResume, saveLocalResume } from "../stores/localResumeStore";

export function App() {
  const [snapshot, setSnapshot] = useState<WorkbenchSnapshot | null>(null);
  const [input, setInput] = useState(() => readLocalResume().draftInput);
  const [error, setError] = useState<ApiError | null>(null);
  const [isSubmitting, setSubmitting] = useState(false);
  const [isLoading, setLoading] = useState(true);
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  const [showDraftSelection, setShowDraftSelection] = useState(false);
  const [openedDraftSetId, setOpenedDraftSetId] = useState<string | null>(null);
  const [showQuestionAnswer, setShowQuestionAnswer] = useState(false);
  const [openedQuestionAnswerId, setOpenedQuestionAnswerId] = useState<string | null>(null);
  const [selectedDraftId, setSelectedDraftId] = useState<string | null>(null);
  const [streamingMessages, setStreamingMessages] = useState<WebConversationMessage[]>([]);
  const [streamingAssistant, setStreamingAssistant] = useState<{ id: string; content: string; waitingForFirstToken: boolean } | null>(null);
  const lastRunMode = snapshot?.agentState.modelMode?.mode ?? "尚未运行";
  const draftSetId = snapshot?.draftTabs.length
    ? snapshot.draftTabs.map((draft) => draft.draftId).join("|")
    : null;
  const questionAnswerPrompt = useMemo(() => {
    const modelOutput = snapshot?.agentState.modelOutput;
    const conversation = modelOutput?.conversation;
    const question = conversation?.questions[0];
    const options = conversation?.answerOptions ?? [];
    if (modelOutput?.messageType !== "CONVERSATION" || !question || options.length === 0) {
      return null;
    }
    return {
      id: [modelOutput.generatedAt, question, ...options.map((option) => option.id)].join("|"),
      question,
      options
    };
  }, [snapshot]);

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

  useEffect(() => {
    if (!draftSetId) {
      setShowDraftSelection(false);
      setSelectedDraftId(null);
      return;
    }
    if (draftSetId !== openedDraftSetId) {
      setShowDraftSelection(true);
      setOpenedDraftSetId(draftSetId);
      setSelectedDraftId(null);
    }
  }, [draftSetId, openedDraftSetId]);

  useEffect(() => {
    if (!questionAnswerPrompt) {
      setShowQuestionAnswer(false);
      return;
    }
    if (questionAnswerPrompt.id !== openedQuestionAnswerId) {
      setShowQuestionAnswer(true);
      setOpenedQuestionAnswerId(questionAnswerPrompt.id);
    }
  }, [questionAnswerPrompt, openedQuestionAnswerId]);

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
    await submitContent(input);
  }

  async function submitContent(content: string) {
    if (!snapshot?.sessionId || isSubmitting) {
      return;
    }
    const submittedContent = content.trim();
    if (submittedContent.length === 0) {
      return;
    }
    const optimisticId = `optimistic-user-${Date.now()}`;
    const optimisticMessage: WebConversationMessage = {
      id: optimisticId,
      role: "USER",
      content: submittedContent,
      sourceType: "USER_CONFIRMED",
      createdAt: new Date().toISOString()
    };
    setSubmitting(true);
    setError(null);
    setStreamingMessages((current) => [...current, optimisticMessage]);
    setStreamingAssistant({ id: "assistant-waiting", content: "", waitingForFirstToken: true });
    setShowQuestionAnswer(false);
    setInput("");
    saveLocalResume({ lastSessionId: snapshot.sessionId, draftInput: "", lastKnownStatus: snapshot.status });
    try {
      await submitMessageStream(snapshot.sessionId, { content: submittedContent, modelMode: "openai-gpt55" }, {
        onUserMessage: (message) => {
          setStreamingMessages((current) => current.map((item) => item.id === optimisticId ? message : item));
        },
        onAssistantStart: (id) => {
          setStreamingAssistant({ id, content: "", waitingForFirstToken: true });
        },
        onAssistantDelta: (id, delta) => {
          setStreamingAssistant((current) => {
            if (!current || current.id !== id) {
              return { id, content: delta, waitingForFirstToken: false };
            }
            return { ...current, content: current.content + delta, waitingForFirstToken: false };
          });
        },
        onSnapshot: (next) => {
          setSnapshot(next);
          setStreamingMessages([]);
          setStreamingAssistant(null);
          saveLocalResume({ lastSessionId: next.sessionId, draftInput: "", lastKnownStatus: next.status });
        }
      });
    } catch (caught) {
      const apiError = caught instanceof WorkbenchApiError ? caught.apiError : fallbackError();
      setError(apiError);
      setInput(submittedContent);
      setStreamingMessages((current) => current.filter((message) => message.id !== optimisticId));
      setStreamingAssistant(null);
      saveLocalResume({ draftInput: submittedContent, lastKnownStatus: "ERROR_RECOVERABLE" });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleConfirmClearSession() {
    if (!snapshot?.sessionId) {
      setShowClearConfirm(false);
      setInput("");
      return;
    }
    const clearingSessionId = snapshot.sessionId;
    setSubmitting(true);
    setError(null);
    try {
      const next = await clearSession(clearingSessionId, { confirmed: true });
      clearSessionResume(clearingSessionId);
      setSnapshot(next);
      setInput("");
      setShowClearConfirm(false);
    } catch (caught) {
      setError(caught instanceof WorkbenchApiError ? caught.apiError : fallbackError());
    } finally {
      setSubmitting(false);
    }
  }

  const main = (
    <section className="conversation-card" aria-label="对话创作区">
      <h1>{snapshot?.heroQuestion ?? "今天喝了什么咖啡？"}</h1>
      {error ? <RecoverableErrorBanner error={error} onRetry={snapshot?.sessionId ? handleSubmit : handleCreateSession} onRecreate={handleCreateSession} /> : null}
      {!snapshot?.sessionId ? (
        <button className="primary-action" type="button" disabled={isSubmitting || isLoading} onClick={handleCreateSession}>
          开始记录
        </button>
      ) : null}
      <ConversationThread
        messages={[...(snapshot?.conversation ?? []), ...streamingMessages]}
        drafts={snapshot?.draftTabs ?? []}
        modelOutput={snapshot?.agentState.modelOutput ?? null}
        streamingAssistant={streamingAssistant}
      />
      {!showQuestionAnswer ? (
        <ConversationComposer
          value={input}
          disabled={!snapshot?.sessionId}
          onChange={(value) => {
            setInput(value);
            saveLocalResume({ draftInput: value, lastSessionId: snapshot?.sessionId ?? null, lastKnownStatus: snapshot?.status ?? "UNKNOWN" });
          }}
          onSubmit={handleSubmit}
        />
      ) : null}
    </section>
  );

  const recordPanel = (
    <>
      <CurrentRecordPanel summary={snapshot?.recordSummary} />
      <section className="session-control-panel" aria-label="会话控制">
        <button type="button" onClick={() => setShowClearConfirm(true)} disabled={isSubmitting}>
          新建记录 / 清空当前会话
        </button>
      </section>
      <AgentStateCards state={snapshot?.agentState} />
      <section className="mode-panel" aria-label="Agent 模式">
        <h2>Agent 模式</h2>
        <div className="mode-row">
          <span>显式工作流</span>
          <span>模型自主工具调用</span>
          <span>本次提交：openai-gpt55</span>
          <span>上次运行：{lastRunMode}</span>
        </div>
      </section>
      <DraftTabs drafts={snapshot?.draftTabs ?? []} selectedDraftId={selectedDraftId} />
    </>
  );

  return (
    <>
      <WorkbenchLayout
        main={main}
        recordPanel={recordPanel}
        agentTrace={<AgentTracePanel steps={traceSteps} />}
      />
      {showClearConfirm ? (
        <ClearSessionConfirmDialog
          isSubmitting={isSubmitting}
          onCancel={() => setShowClearConfirm(false)}
          onConfirm={handleConfirmClearSession}
        />
      ) : null}
      {showDraftSelection && snapshot?.draftTabs.length ? (
        <DraftSelectionDialog
          drafts={snapshot.draftTabs}
          onClose={() => setShowDraftSelection(false)}
          onSelect={(draftId) => {
            setSelectedDraftId(draftId);
            setShowDraftSelection(false);
          }}
        />
      ) : null}
      {showQuestionAnswer && questionAnswerPrompt ? (
        <QuestionAnswerDialog
          question={questionAnswerPrompt.question}
          options={questionAnswerPrompt.options}
          disabled={isSubmitting}
          onSelect={(content) => {
            void submitContent(content);
          }}
          onCustomSubmit={(content) => {
            void submitContent(content);
          }}
        />
      ) : null}
    </>
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
