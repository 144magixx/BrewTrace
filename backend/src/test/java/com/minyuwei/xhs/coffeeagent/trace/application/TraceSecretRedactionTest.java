package com.minyuwei.xhs.coffeeagent.trace.application;

import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.trace.domain.AgentTraceStep;

import java.util.Map;

public class TraceSecretRedactionTest {
    public static void run() {
        Map<String, Object> safe = AgentTraceStep.redact(Map.of(
                "apiKey", "sk-secret",
                "Authorization", "Bearer token",
                "Cookie", "abc",
                "content", "正常内容"
        ));
        String rendered = safe.toString();
        ApiContractTestSupport.assertTrue(!rendered.contains("sk-secret") && !rendered.contains("Bearer token") && !rendered.contains("abc"), "轨迹快照不得包含完整密钥或凭证");
        ApiContractTestSupport.assertContains(rendered, "正常内容", "非敏感内容应保留");
    }
}
