package com.minyuwei.xhs.coffeeagent.shared.config;

import com.minyuwei.xhs.coffeeagent.agent.application.AgentOrchestrator;
import com.minyuwei.xhs.coffeeagent.tasting.application.TastingSessionApplicationService;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSessionRepository;
import com.minyuwei.xhs.coffeeagent.tasting.infrastructure.TastingSessionRepositoryAdapter;
import com.minyuwei.xhs.coffeeagent.user.application.CurrentUserProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebWorkbenchConfiguration {
    @Bean
    public TastingSessionRepository tastingSessionRepository() {
        return new TastingSessionRepositoryAdapter();
    }

    @Bean
    public CurrentUserProvider currentUserProvider() {
        return new CurrentUserProvider();
    }

    @Bean
    public AgentOrchestrator agentOrchestrator() {
        return new AgentOrchestrator();
    }

    @Bean
    public TastingSessionApplicationService tastingSessionApplicationService(
            TastingSessionRepository repository,
            CurrentUserProvider currentUserProvider,
            AgentOrchestrator orchestrator
    ) {
        return new TastingSessionApplicationService(repository, currentUserProvider, orchestrator);
    }
}
