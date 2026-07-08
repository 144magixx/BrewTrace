package com.minyuwei.xhs.coffeeagent.agent.infrastructure.advisor;

public final class ModelAdvisorContextKeys {
    public static final String MODEL_CONTEXT_PACKAGE = "coffeeAgent.modelContextPackage";
    public static final String MODEL_NAME = "coffeeAgent.modelName";
    public static final String REQUEST_PREVIEW = "coffeeAgent.requestPreview";
    public static final String REQUEST_SENT_AT = "coffeeAgent.requestSentAt";
    public static final String RESPONSE_RECEIVED_AT = "coffeeAgent.responseReceivedAt";
    public static final String RESPONSE_CONTENT = "coffeeAgent.responseContent";
    public static final String CALL_DURATION_MS = "coffeeAgent.callDurationMs";
    public static final String FACT_BOUNDARY_SUMMARY = "coffeeAgent.factBoundarySummary";
    public static final String TRACE_RECORDED = "coffeeAgent.traceRecorded";

    private ModelAdvisorContextKeys() {
    }
}
