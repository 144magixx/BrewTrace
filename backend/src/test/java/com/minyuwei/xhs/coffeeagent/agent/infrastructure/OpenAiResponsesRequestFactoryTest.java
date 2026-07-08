package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMode;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiResponsesRequestFactoryTest {
    @Test
    void loadsPromptTemplateAndAppliesDynamicConstraints() {
        OpenAiResponsesRequestFactory factory = new OpenAiResponsesRequestFactory(new PromptTemplateLoader());
        String body = factory.createBody("gpt-5.5", new ModelContextPackage(
                "s1",
                ModelMode.OPENAI_GPT55,
                List.of(new ModelContextPackage.ContextEntry("ctx1", "我喝了杯可乐", "USER_CONFIRMED", "WILL_SEND", null)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("已确认事实可以进入文案依据。"),
                Instant.parse("2026-06-30T00:00:00Z")
        ));

        assertTrue(body.contains("输入字段含义"));
        assertTrue(body.contains("currentSession[].content"));
        assertTrue(body.contains("请根据当前上下文先判断应该继续追问还是生成小红书文案草稿"));
        assertTrue(body.contains("一次只追问一个最自然的下一步问题"));
        assertTrue(body.contains("输入与咖啡主题无关"));
        assertTrue(body.contains("询问用户是否现在生成文案"));
        assertTrue(body.contains("现在生成文案预览"));
        assertTrue(body.contains("\"messageType\""));
        assertTrue(body.contains("\"talk\""));
        assertTrue(body.contains("\"post\""));
        assertTrue(body.contains("\"conversation\""));
        assertTrue(body.contains("\"answerOptions\""));
        assertTrue(body.contains("备选回答"));
        assertTrue(body.contains("\"maxItems\" : 1"));
        assertTrue(body.contains("\"maxItems\" : 4"));
        assertTrue(body.contains("\"coffee_model_message\""));
        assertTrue(body.contains("生成 restrained / 中立克制风格"));
        assertTrue(body.contains("生成 exaggerated / 夸张菜单感咖啡文案"));
        assertTrue(body.contains("生成 sharp-review / 锐评风格咖啡文案"));
        assertTrue(body.contains("已确认事实可以进入文案依据。"));
        assertFalse(body.contains("{{additionalPromptConstraints}}"));
        assertFalse(body.contains("docs/research/xhs-style-prompts"));
    }
}
