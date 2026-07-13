package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import java.util.Optional;

/**
 * 保存当前同步模型调用真正交给 HTTP Client 的请求体。
 *
 * <p>捕获值按线程隔离，只用于生成脱敏请求预览；调用结束后必须清理，避免在线程池中残留会话内容。</p>
 */
public class ActualModelRequestCapture {
    private final ThreadLocal<String> latestRequestBody = new ThreadLocal<>();

    /**
     * 记录发送层已经完成序列化、即将交给 HTTP Client 的原始请求体。
     *
     * @param requestBody 实际发送的 JSON 请求体
     */
    public void record(String requestBody) {
        latestRequestBody.set(requestBody == null ? "" : requestBody);
    }

    /**
     * 返回当前线程最近一次实际发送的请求体，不执行重新序列化。
     *
     * @return 已捕获请求体；发送层尚未执行时为空
     */
    public Optional<String> latest() {
        return Optional.ofNullable(latestRequestBody.get()).filter(body -> !body.isBlank());
    }

    /**
     * 清理当前线程捕获的请求体，防止线程复用时泄漏到其他会话。
     */
    public void clear() {
        latestRequestBody.remove();
    }
}
