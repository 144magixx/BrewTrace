package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyuwei.xhs.coffeeagent.agent.application.*;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.advisor.ModelAdvisorContextKeys;
import com.minyuwei.xhs.coffeeagent.shared.error.SensitiveValueRedactor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 Spring AI {@link ChatClient} 的真实模型网关实现。
 *
 * <p>该类负责把应用层的 {@link ModelContextPackage} 组装为模型请求，执行受限深度的工具调用循环，
 * 将最终响应解析为应用层消息，并生成脱敏的请求/响应预览及可恢复错误。</p>
 */
public class SpringAiModelGateway implements ModelGateway {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ChatClient chatClient;
    private final OpenAiResponsesRequestFactory requestFactory;
    private final OpenAiResponsesParser parser;
    private final String modelName;
    private final List<ToolCallback> toolCallbacks;
    private final ToolCallingManager toolCallingManager;

    /**
     * 创建不暴露工具的模型网关。
     *
     * @param chatClient 执行模型请求的 Spring AI 客户端
     * @param requestFactory 将业务上下文转换为模型 Prompt 和预览数据的工厂
     * @param parser 将模型文本响应解析为应用层消息的解析器
     * @param modelName 实际调用及预览展示使用的模型名称
     */
    public SpringAiModelGateway(ChatClient chatClient, OpenAiResponsesRequestFactory requestFactory, OpenAiResponsesParser parser, String modelName) {
        this(chatClient, requestFactory, parser, modelName, List.of());
    }

    /**
     * 创建可向模型暴露指定工具的模型网关，并初始化对应的工具调用管理器。
     *
     * @param chatClient 执行模型请求的 Spring AI 客户端
     * @param requestFactory 将业务上下文转换为模型 Prompt 和预览数据的工厂
     * @param parser 将模型文本响应解析为应用层消息的解析器
     * @param modelName 实际调用及预览展示使用的模型名称
     * @param toolCallbacks 允许模型调用的工具回调；为 {@code null} 时按空集合处理
     */
    public SpringAiModelGateway(ChatClient chatClient, OpenAiResponsesRequestFactory requestFactory, OpenAiResponsesParser parser, String modelName, List<ToolCallback> toolCallbacks) {
        this.chatClient = chatClient;
        this.requestFactory = requestFactory;
        this.parser = parser;
        this.modelName = modelName;
        this.toolCallbacks = toolCallbacks == null ? List.of() : List.copyOf(toolCallbacks);
        this.toolCallingManager = DefaultToolCallingManager.builder()
                .toolCallbackResolver(new StaticToolCallbackResolver(this.toolCallbacks))
                .build();
    }

    /**
     * 根据当前模型上下文完成一次模型交互，并在模型请求工具时最多执行三轮工具调用。
     *
     * <p>成功时返回解析后的会话或文案消息及脱敏预览；模型或工具链异常时返回包含
     * {@link RecoverableModelError} 的可恢复失败结果，而不是向上抛出运行时异常。</p>
     *
     * @param contextPackage 当前会话、事实边界和模型消息等完整上下文包
     * @return 模型生成结果，包含业务消息、请求/响应预览或可恢复错误
     */
    @Override
    public ModelResult complete(ModelContextPackage contextPackage) {
        Map<String, Object> toolContext = Map.of(
                "sessionId", contextPackage.sessionId(),
                "purpose", "模型在咖啡品鉴对话中补充待确认风味联想",
                "confirmed", false
        );
        Prompt prompt = promptWithTools(requestFactory.createPrompt(contextPackage), toolContext);
        ModelPreview.ModelRequestPreview fallbackRequestPreview = fallbackRequestPreview(prompt);
        try {
            ChatClientResponse chatClientResponse = callModel(prompt, contextPackage, toolContext);
            int toolCallDepth = 0;
            while (chatClientResponse.chatResponse() != null && chatClientResponse.chatResponse().hasToolCalls() && toolCallDepth < 3) {
                ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatClientResponse.chatResponse());
                prompt = new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions());
                chatClientResponse = callModel(prompt, contextPackage, toolContext);
                toolCallDepth++;
            }
            String responseContent = responseContent(chatClientResponse.chatResponse());
            ModelAgentMessage message = parser.parseMessage(responseContent);
            Instant receivedAt = Instant.now();
            Map<String, Object> responsePreview = new LinkedHashMap<>();
            responsePreview.put("transport", "Spring AI ChatClient");
            responsePreview.put("messageType", message.messageType());
            responsePreview.put("talk", message.talk());
            responsePreview.put("post", message.post());
            responsePreview.put("conversation", message.conversation());
            responsePreview.put("warnings", message.warnings());
            responsePreview.put("advisorTraceRecorded", chatClientResponse.context().getOrDefault(ModelAdvisorContextKeys.TRACE_RECORDED, false));
            responsePreview.put("durationMs", chatClientResponse.context().getOrDefault(ModelAdvisorContextKeys.CALL_DURATION_MS, 0L));
            String responsePreviewBody = SensitiveValueRedactor.redact(previewJson(responsePreview));
            ModelPreview.ModelRequestPreview requestPreview = requestPreview(chatClientResponse.context(), fallbackRequestPreview);
            return new ModelResult(
                    ModelMode.OPENAI_GPT55,
                    "REAL_MODEL",
                    modelName,
                    "Spring AI 模型调用",
                    "由 Spring AI ChatClient 调用真实模型生成，事实边界仍需检查。",
                    message.talk(),
                    message.messageType(),
                    message.talk(),
                    message.post(),
                    message.conversation(),
                    message.warnings(),
                    message.variants(),
                    requestPreview,
                    new ModelPreview.ModelResponsePreview("Spring AI 模型返回", modelName, ModelMode.OPENAI_GPT55.code(), responsePreviewBody, "SAFE_TO_DISPLAY", receivedAt),
                    null,
                    receivedAt
            );
        } catch (ModelGatewayException exception) {
            RecoverableModelError error = toRecoverableError(exception, contextPackage.sessionId());
            return errorResult(fallbackRequestPreview, error);
        } catch (Exception exception) {
            RecoverableModelError error = RecoverableModelError.of(
                    RecoverableModelError.Code.MODEL_SERVICE_UNAVAILABLE,
                    SensitiveValueRedactor.redact("Spring AI 模型调用失败，请稍后重试。"),
                    contextPackage.sessionId(),
                    "RETRY"
            );
            return errorResult(fallbackRequestPreview, error);
        }
    }

    /**
     * 使用给定 Prompt 调用一次模型，并把业务上下文交给 Advisor、把工具上下文交给工具回调。
     *
     * @param prompt 本轮发送给模型的 Prompt，可能包含此前工具调用的对话历史
     * @param contextPackage Advisor 记录请求预览和调用轨迹所需的业务上下文包
     * @param toolContext 工具执行所需的会话、调用目的和确认状态上下文
     * @return 包含模型响应和 Advisor 上下文的 Spring AI 客户端响应
     */
    private ChatClientResponse callModel(Prompt prompt, ModelContextPackage contextPackage, Map<String, Object> toolContext) {
        return chatClient.prompt(prompt)
                .advisors(advisor -> advisor.params(Map.of(
                        ModelAdvisorContextKeys.MODEL_CONTEXT_PACKAGE, contextPackage,
                        ModelAdvisorContextKeys.MODEL_NAME, modelName
                )))
                .toolContext(toolContext)
                .call()
                .chatClientResponse();
    }

    /**
     * 在存在已注册工具时，将工具回调及工具上下文绑定到 Prompt 的 ChatOptions。
     *
     * @param prompt 请求工厂生成的原始 Prompt
     * @param toolContext 工具执行时可读取的上下文
     * @return 无工具时返回原 Prompt，否则返回携带工具调用选项的新 Prompt
     */
    private Prompt promptWithTools(Prompt prompt, Map<String, Object> toolContext) {
        if (toolCallbacks.isEmpty()) {
            return prompt;
        }
        return prompt.mutate()
                .chatOptions(ToolCallingChatOptions.builder()
                        .toolCallbacks(toolCallbacks)
                        .toolContext(toolContext)
                        .build())
                .build();
    }

    /**
     * 在 Advisor 尚未产出真实请求预览时，根据当前 Prompt 创建安全的兜底请求预览。
     *
     * @param prompt 即将发送或已经尝试发送的模型 Prompt
     * @return 可在工作台展示的脱敏请求预览
     */
    private ModelPreview.ModelRequestPreview fallbackRequestPreview(Prompt prompt) {
        return new ModelPreview.ModelRequestPreview(
                "已通过 Spring AI 发送给大模型",
                modelName,
                ModelMode.OPENAI_GPT55.code(),
                "Spring AI ChatClient -> Responses API",
                requestFactory.createPreviewBody(modelName, prompt),
                "SAFE_TO_DISPLAY",
                Instant.now()
        );
    }

    /**
     * 优先从 Advisor 上下文读取真实请求预览，缺失或类型不匹配时使用兜底预览。
     *
     * @param context Spring AI Advisor 返回的调用上下文
     * @param fallback 预先构造的兜底请求预览
     * @return Advisor 记录的请求预览或兜底预览
     */
    private ModelPreview.ModelRequestPreview requestPreview(Map<String, Object> context, ModelPreview.ModelRequestPreview fallback) {
        Object value = context.get(ModelAdvisorContextKeys.REQUEST_PREVIEW);
        return value instanceof ModelPreview.ModelRequestPreview preview ? preview : fallback;
    }

    /**
     * 从 Spring AI 响应中安全提取最终文本，兼容响应、结果或输出为空的情况。
     *
     * @param chatResponse Spring AI 原始聊天响应，可为 {@code null}
     * @return 最终输出文本；无法取得输出时返回空字符串
     */
    private String responseContent(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            return "";
        }
        return chatResponse.getResult().getOutput().getText();
    }

    /**
     * 将可恢复模型错误转换为统一的失败结果，并保留请求预览供用户排查。
     *
     * @param requestPreview 本次调用的安全请求预览
     * @param error 已分类并脱敏的可恢复模型错误
     * @return 不包含业务消息、但包含错误响应预览和恢复建议的模型结果
     */
    private ModelResult errorResult(ModelPreview.ModelRequestPreview requestPreview, RecoverableModelError error) {
        return new ModelResult(
                ModelMode.OPENAI_GPT55,
                "ERROR",
                modelName,
                "Spring AI 模型不可用",
                "Spring AI 模型调用失败，已保留当前会话和上下文预览。",
                "",
                null,
                "",
                null,
                null,
                java.util.List.of(),
                java.util.List.of(),
                requestPreview,
                new ModelPreview.ModelResponsePreview("Spring AI 模型返回", modelName, ModelMode.OPENAI_GPT55.code(), previewJson(Map.of("error", error.message(), "code", error.code().name())), "SAFE_TO_DISPLAY", Instant.now()),
                error,
                Instant.now()
        );
    }

    /**
     * 把模型网关异常映射为面向用户的脱敏错误文案和可执行恢复动作。
     *
     * @param exception 带有稳定错误码的模型网关异常
     * @param sessionId 发生异常的会话标识，用于错误关联和状态恢复
     * @return 可安全返回给工作台的可恢复错误
     */
    private RecoverableModelError toRecoverableError(ModelGatewayException exception, String sessionId) {
        String message = SensitiveValueRedactor.redact(switch (exception.code()) {
            case MODEL_TIMEOUT -> "Spring AI 模型响应超时，请重试。";
            case MODEL_AUTH_FAILED -> "Spring AI 模型鉴权失败，请检查本地 OPENAI_API_KEY。";
            case MODEL_RATE_LIMITED -> "Spring AI 模型当前限流，请稍后重试。";
            case MODEL_FORMAT_INVALID -> "Spring AI 模型返回格式异常，请重试。";
            case MODEL_SERVICE_UNAVAILABLE -> "Spring AI 模型服务暂时不可用，请稍后重试。";
        });
        String[] actions = switch (exception.code()) {
            case MODEL_AUTH_FAILED -> new String[]{"CHECK_LOCAL_ENV", "RETRY"};
            case MODEL_RATE_LIMITED -> new String[]{"TRY_LATER", "RETRY"};
            default -> new String[]{"RETRY"};
        };
        return RecoverableModelError.of(exception.code(), message, sessionId, actions);
    }

    /**
     * 将预览对象序列化为格式化 JSON 并执行敏感信息脱敏。
     *
     * @param value 待写入请求或响应预览的对象
     * @return 脱敏后的 JSON；序列化失败时返回空 JSON 对象
     */
    private String previewJson(Object value) {
        try {
            return SensitiveValueRedactor.redact(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value));
        } catch (JsonProcessingException exception) {
            return OBJECT_MAPPER.createObjectNode().toString();
        }
    }
}
