package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError;
import com.minyuwei.xhs.coffeeagent.shared.error.SensitiveValueRedactor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OpenAiResponsesLlmClient {
    private final HttpClient httpClient;

    public OpenAiResponsesLlmClient() {
        this(HttpClient.newHttpClient());
    }

    public OpenAiResponsesLlmClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public LlmResponse createResponse(String baseUrl, String apiKey, String body, int timeoutSeconds) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ModelGatewayException(RecoverableModelError.Code.MODEL_AUTH_FAILED, "缺少本地 OPENAI_API_KEY。");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trimTrailingSlash(baseUrl) + "/responses"))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 401 || status == 403) {
                throw new ModelGatewayException(RecoverableModelError.Code.MODEL_AUTH_FAILED, "真实模型鉴权失败，请检查本地 OPENAI_API_KEY。");
            }
            if (status == 429) {
                throw new ModelGatewayException(RecoverableModelError.Code.MODEL_RATE_LIMITED, "真实模型限流，请稍后重试。");
            }
            if (status >= 500) {
                throw new ModelGatewayException(RecoverableModelError.Code.MODEL_SERVICE_UNAVAILABLE, "真实模型服务暂时不可用。");
            }
            if (status < 200 || status >= 300) {
                throw new ModelGatewayException(RecoverableModelError.Code.MODEL_SERVICE_UNAVAILABLE, "真实模型请求失败：" + status);
            }
            return new LlmResponse(status, SensitiveValueRedactor.redact(response.body()));
        } catch (java.net.http.HttpTimeoutException exception) {
            throw new ModelGatewayException(RecoverableModelError.Code.MODEL_TIMEOUT, "真实模型响应超时，请重试。");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ModelGatewayException(RecoverableModelError.Code.MODEL_SERVICE_UNAVAILABLE, "真实模型请求被中断。");
        } catch (IOException exception) {
            throw new ModelGatewayException(RecoverableModelError.Code.MODEL_SERVICE_UNAVAILABLE, SensitiveValueRedactor.redact(exception.getMessage()));
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://saturday.sankuai.com/v1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public record LlmResponse(int status, String body) {
    }
}
