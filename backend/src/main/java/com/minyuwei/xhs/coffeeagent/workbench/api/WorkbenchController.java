package com.minyuwei.xhs.coffeeagent.workbench.api;

import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.error.ApiError;
import com.minyuwei.xhs.coffeeagent.shared.error.CoffeeAgentException;
import com.minyuwei.xhs.coffeeagent.workbench.application.WebWorkbenchService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/workbench")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
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
            return ApiResponse.success(requestId(requestId), workbenchService.submitMessage(sessionId, content));
        } catch (CoffeeAgentException exception) {
            ApiError error = exception.apiError();
            return ApiResponse.failure(requestId(requestId), error);
        }
    }

    private String requestId(String provided) {
        return provided == null || provided.isBlank() ? UUID.randomUUID().toString() : provided;
    }
}
