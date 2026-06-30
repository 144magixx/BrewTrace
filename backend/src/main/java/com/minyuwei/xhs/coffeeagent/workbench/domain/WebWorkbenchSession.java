package com.minyuwei.xhs.coffeeagent.workbench.domain;

public final class WebWorkbenchSession {
    private WebWorkbenchSession() {
    }

    public enum Status {
        EMPTY,
        SESSION_CREATED,
        WAITING_FOR_FACTS,
        DRAFTS_READY,
        ERROR_RECOVERABLE
    }
}
