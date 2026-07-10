package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMode;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionService;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolCallPolicy;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolCallRecorder;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolRegistry;
import com.minyuwei.xhs.coffeeagent.tools.infrastructure.FlavorSuggestionToolAdapter;
import com.minyuwei.xhs.coffeeagent.tools.infrastructure.FlavorSuggestionToolRegistrar;
import com.minyuwei.xhs.coffeeagent.tools.infrastructure.SpringAiToolCallbackAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiResponsesToolRequestFactoryTest {
    @Test
    void serializesFlavorSuggestionToolSchemaIntoResponsesBody() {
        OpenAiResponsesRequestFactory requestFactory = new OpenAiResponsesRequestFactory(new PromptTemplateLoader());
        Prompt prompt = requestFactory.createPrompt(contextPackage()).mutate()
                .chatOptions(ToolCallingChatOptions.builder()
                        .toolCallbacks(List.of(flavorSuggestionCallback()))
                        .build())
                .build();

        String body = requestFactory.createBody("gpt-5.5", prompt);

        assertTrue(body.contains("\"tools\""));
        assertTrue(body.contains("\"name\" : \"flavor_suggestion\""));
        assertTrue(body.contains("\"tool_choice\" : \"auto\""));
        assertTrue(body.contains("\"inputTerm\""));
        assertTrue(body.contains("待用户确认的具体风味联想候选"));
    }

    private SpringAiToolCallbackAdapter flavorSuggestionCallback() {
        ToolRegistry registry = new ToolRegistry();
        new FlavorSuggestionToolRegistrar().register(registry, new FlavorSuggestionService());
        return new SpringAiToolCallbackAdapter(registry, new ToolCallPolicy(), new ToolCallRecorder(), FlavorSuggestionToolAdapter.TOOL_NAME);
    }

    private ModelContextPackage contextPackage() {
        return new ModelContextPackage(
                "s1",
                ModelMode.OPENAI_GPT55,
                List.of(new ModelContextPackage.ContextEntry("ctx1", "今天喝了一支水洗埃塞，有一点柑橘", "USER_CONFIRMED", "WILL_SEND", null)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("工具返回的风味联想只能作为待确认项。"),
                Instant.parse("2026-07-08T00:00:00Z")
        );
    }
}
