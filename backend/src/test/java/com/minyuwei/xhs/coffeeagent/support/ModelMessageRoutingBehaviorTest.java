package com.minyuwei.xhs.coffeeagent.support;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMessageType;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMode;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.OpenAiResponsesParser;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.OpenAiResponsesRequestFactory;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.fixtures.ModelResponseFixtures;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import com.minyuwei.xhs.coffeeagent.tasting.application.TastingSessionApplicationService;
import com.minyuwei.xhs.coffeeagent.tasting.infrastructure.TastingSessionRepositoryAdapter;
import com.minyuwei.xhs.coffeeagent.user.application.CurrentUserProvider;
import com.minyuwei.xhs.coffeeagent.workbench.application.AgentStateAssembler;
import com.minyuwei.xhs.coffeeagent.workbench.application.FactBoundaryChecker;
import com.minyuwei.xhs.coffeeagent.workbench.application.WebWorkbenchService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ModelMessageRoutingBehaviorTest {
    private ModelMessageRoutingBehaviorTest() {
    }

    @Test
    void routeModelBehaviorStillPasses() {
        run();
    }

    public static void run() {
        OpenAiResponsesParser parser = new OpenAiResponsesParser();
        assertEquals(ModelMessageType.CONVERSATION, parser.parseMessage(ModelResponseFixtures.conversation()).messageType());
        assertEquals(3, parser.parseMessage(ModelResponseFixtures.post()).variants().size());

        String requestBody = new OpenAiResponsesRequestFactory(new PromptTemplateLoader()).createBody("gpt-5.5", contextPackage());
        assertTrue(requestBody.contains("\"messageType\""));
        assertTrue(requestBody.contains("生成 restrained / 中立克制风格"));
        assertTrue(requestBody.contains("生成 sharp-review / 锐评风格咖啡文案"));
        assertFalse(requestBody.contains("docs/research/xhs-style-prompts"));

        WebWorkbenchService conversationService = service(ModelMessageType.CONVERSATION);
        var conversation = conversationService.createSession(com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode.EXPLICIT_WORKFLOW);
        var conversationResult = conversationService.submitMessage(conversation.sessionId(), "今天喝了一杯咖啡");
        assertTrue(conversationResult.draftTabs().isEmpty());
        assertEquals(ModelMessageType.CONVERSATION, conversationResult.agentState().modelOutput().messageType());

        WebWorkbenchService postService = service(ModelMessageType.POST);
        var post = postService.createSession(com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode.EXPLICIT_WORKFLOW);
        var postResult = postService.submitMessage(post.sessionId(), "今天喝了一支水洗埃塞，给我生成文案吧。");
        assertEquals(3, postResult.draftTabs().size());
        assertEquals(ModelMessageType.POST, postResult.agentState().modelOutput().messageType());
    }

    private static WebWorkbenchService service(ModelMessageType messageType) {
        return new WebWorkbenchService(
                new TastingSessionApplicationService(new TastingSessionRepositoryAdapter(), new CurrentUserProvider()),
                new AgentStateAssembler(new FactBoundaryChecker(), new FakeModelGateway(messageType))
        );
    }

    private static ModelContextPackage contextPackage() {
        return new ModelContextPackage(
                "s1",
                ModelMode.OPENAI_GPT55,
                List.of(new ModelContextPackage.ContextEntry("ctx1", "今天喝了一支水洗埃塞", "USER_CONFIRMED", "WILL_SEND", null)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("已确认事实可以进入文案依据。"),
                Instant.parse("2026-06-30T00:00:00Z")
        );
    }
}
