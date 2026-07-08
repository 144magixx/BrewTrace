package com.minyuwei.xhs.coffeeagent.trace.infrastructure;

import com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceRecorder;
import com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentTraceConfiguration {
    @Bean
    public AgentTraceRepositoryAdapter agentTraceRepositoryAdapter() {
        return new AgentTraceRepositoryAdapter();
    }

    @Bean
    public AgentTraceService agentTraceService(AgentTraceRepositoryAdapter repository) {
        return new AgentTraceService(repository);
    }

    @Bean
    public AgentTraceRecorder agentTraceRecorder(AgentTraceService traceService) {
        return new AgentTraceRecorder(traceService);
    }
}
