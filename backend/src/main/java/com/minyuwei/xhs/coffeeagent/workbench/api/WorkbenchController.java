package com.minyuwei.xhs.coffeeagent.workbench.api;

import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.error.ApiError;
import com.minyuwei.xhs.coffeeagent.shared.error.CoffeeAgentException;
import com.minyuwei.xhs.coffeeagent.workbench.application.WebWorkbenchService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/workbench")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:5174", "http://127.0.0.1:5174"})
public class WorkbenchController {
    private final WebWorkbenchService workbenchService;

    public WorkbenchController(WebWorkbenchService workbenchService) {
        this.workbenchService = workbenchService;
    }

    @GetMapping("/snapshot")
    public ApiResponse<WebWorkbenchDtos.WorkbenchSnapshot> snapshot(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestParam(value = "sessionId", required = false) String sessionId
    ) {
        return ApiResponse.success(requestId(requestId), workbenchService.snapshot(sessionId));
    }

    @PostMapping("/sessions")
    public ApiResponse<WebWorkbenchDtos.WorkbenchSnapshot> createSession(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestBody(required = false) WebWorkbenchDtos.CreateSessionRequest request
    ) {
        OrchestrationMode mode = request == null ? OrchestrationMode.EXPLICIT_WORKFLOW : request.mode();
        return ApiResponse.success(requestId(requestId), workbenchService.createSession(mode));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<WebWorkbenchDtos.WorkbenchSnapshot> submitMessage(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @PathVariable String sessionId,
            @RequestBody WebWorkbenchDtos.SubmitMessageRequest request
    ) {
        try {
            String content = request == null ? "" : request.content();
            String modelMode = request == null ? null : request.modelMode();
            return ApiResponse.success(requestId(requestId), workbenchService.submitMessage(sessionId, content, modelMode));
        } catch (CoffeeAgentException exception) {
            ApiError error = exception.apiError();
            return ApiResponse.failure(requestId(requestId), error);
        }
    }

    @PostMapping(path = "/sessions/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @PathVariable String sessionId,
            @RequestBody WebWorkbenchDtos.SubmitMessageRequest request
    ) {
        SseEmitter emitter = new SseEmitter(140_000L);
        String effectiveRequestId = requestId(requestId);
        CompletableFuture.runAsync(() -> {
            try {
                String content = request == null ? "" : request.content();
                String modelMode = request == null ? null : request.modelMode();
                WebWorkbenchDtos.WebConversationMessage userMessage = workbenchService.recordUserMessage(sessionId, content);
                send(emitter, "user_message", Map.of(
                        "requestId", effectiveRequestId,
                        "message", userMessage
                ));
                String assistantStreamId = "assistant-stream-" + UUID.randomUUID();
                send(emitter, "assistant_start", Map.of(
                        "requestId", effectiveRequestId,
                        "id", assistantStreamId
                ));
                WebWorkbenchService.AssistantTurnResult result = workbenchService.completeAssistantTurn(sessionId, modelMode);
                streamAssistantContent(emitter, assistantStreamId, result.assistantContent());
                send(emitter, "snapshot", Map.of(
                        "requestId", effectiveRequestId,
                        "snapshot", result.snapshot()
                ));
                send(emitter, "done", Map.of("requestId", effectiveRequestId));
                emitter.complete();
            } catch (CoffeeAgentException exception) {
                sendError(emitter, effectiveRequestId, exception.apiError());
            } catch (Exception exception) {
                ApiError error = ApiError.of("STREAM_FAILED", com.minyuwei.xhs.coffeeagent.shared.error.ErrorCategory.RETRYABLE, "流式提交失败，已保留当前输入。", true, "RETRY");
                sendError(emitter, effectiveRequestId, error);
            }
        });
        return emitter;
    }

    @PostMapping("/sessions/{sessionId}/clear")
    public ApiResponse<WebWorkbenchDtos.WorkbenchSnapshot> clearSession(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @PathVariable String sessionId,
            @RequestBody(required = false) WebWorkbenchDtos.ClearSessionRequest request
    ) {
        try {
            boolean confirmed = request != null && request.confirmed();
            return ApiResponse.success(requestId(requestId), workbenchService.clearSession(sessionId, confirmed));
        } catch (CoffeeAgentException exception) {
            return ApiResponse.failure(requestId(requestId), exception.apiError());
        }
    }

    private String requestId(String provided) {
        return provided == null || provided.isBlank() ? UUID.randomUUID().toString() : provided;
    }

    private void streamAssistantContent(SseEmitter emitter, String assistantStreamId, String content) throws IOException {
        if (content == null || content.isBlank()) {
            return;
        }
        for (String chunk : chunks(content)) {
            send(emitter, "assistant_delta", Map.of(
                    "id", assistantStreamId,
                    "delta", chunk
            ));
            try {
                Thread.sleep(18L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("stream interrupted", exception);
            }
        }
    }

    private java.util.List<String> chunks(String content) {
        java.util.List<String> chunks = new java.util.ArrayList<>();
        for (int index = 0; index < content.length(); index += 2) {
            chunks.add(content.substring(index, Math.min(index + 2, content.length())));
        }
        return chunks;
    }

    private void send(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data));
    }

    private void sendError(SseEmitter emitter, String requestId, ApiError error) {
        try {
            send(emitter, "error", Map.of(
                    "requestId", requestId,
                    "error", error
            ));
        } catch (IOException ignored) {
            // Client disconnected.
        } finally {
            emitter.complete();
        }
    }
}
