package com.minyuwei.xhs.coffeeagent.tools.infrastructure;

import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionService;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolCallPolicy;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolCallRecorder;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolRegistry;
import com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceRecorder;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ToolConfiguration {
    @Bean
    public FlavorSuggestionService flavorSuggestionService() {
        return new FlavorSuggestionService();
    }

    @Bean
    public ToolRegistry toolRegistry(FlavorSuggestionService flavorSuggestionService) {
        ToolRegistry registry = new ToolRegistry();
        new FlavorSuggestionToolRegistrar().register(registry, flavorSuggestionService);
        return registry;
    }

    @Bean
    public ToolCallPolicy toolCallPolicy() {
        return new ToolCallPolicy();
    }

    @Bean
    public ToolCallRecorder toolCallRecorder(AgentTraceRecorder traceRecorder) {
        return new ToolCallRecorder(traceRecorder);
    }

    @Bean
    public ToolCallback flavorSuggestionToolCallback(ToolRegistry registry, ToolCallPolicy policy, ToolCallRecorder recorder) {
        return new SpringAiToolCallbackAdapter(registry, policy, recorder, FlavorSuggestionToolAdapter.TOOL_NAME);
    }

    @Bean
    public Advisor toolCallingAdvisor(List<ToolCallback> toolCallbacks) {
        return ToolCallingAdvisor.builder()
                .toolCallingManager(DefaultToolCallingManager.builder()
                        .toolCallbackResolver(new StaticToolCallbackResolver(toolCallbacks))
                        .build())
                .build();
    }
}
