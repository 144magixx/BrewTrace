package com.minyuwei.xhs.coffeeagent.workbench.domain;

public final class AgentStateModels {
    private AgentStateModels() {
    }

    public enum SendStatus {
        WILL_SEND,
        PAGE_ONLY,
        SEND_AFTER_CONFIRMATION,
        EXCLUDED
    }

    public enum RiskLevel {
        NONE,
        INFO,
        WARNING,
        HIGH
    }

    public enum BasisType {
        USER_CONFIRMED,
        MODEL_INFERENCE,
        CANDIDATE_MEMORY,
        PENDING_ASSOCIATION,
        UNSUPPORTED,
        CONFLICT
    }

    public enum RecommendedAction {
        KEEP,
        ASK_USER_CONFIRMATION,
        EXCLUDE_FROM_FINAL_RECORD,
        REWRITE
    }

    public enum AgentCardType {
        SESSION_CONTEXT,
        CONFIRMED_FACT,
        PENDING_ASSOCIATION,
        CANDIDATE_MEMORY,
        CONTEXT_PREVIEW,
        MODEL_OUTPUT,
        FACT_BOUNDARY_CHECK,
        CAPABILITY_BOUNDARY,
        SESSION_CONTROL
    }
}
