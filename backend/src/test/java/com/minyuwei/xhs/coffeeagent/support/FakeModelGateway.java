package com.minyuwei.xhs.coffeeagent.support;

import com.minyuwei.xhs.coffeeagent.agent.application.CopyVariant;
import com.minyuwei.xhs.coffeeagent.agent.application.ConversationModelMessage;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelGateway;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMessageType;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMode;
import com.minyuwei.xhs.coffeeagent.agent.application.PostModelMessage;

import java.time.Instant;
import java.util.List;

public class FakeModelGateway implements ModelGateway {
    private final ModelMessageType messageType;

    public FakeModelGateway() {
        this(ModelMessageType.POST);
    }

    public FakeModelGateway(ModelMessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public ModelResult complete(ModelContextPackage request) {
        if (messageType == ModelMessageType.CONVERSATION) {
            return new ModelResult(
                    ModelMode.OPENAI_GPT55,
                    "REAL_MODEL",
                    "gpt-5.5",
                    ModelMode.OPENAI_GPT55.displayName(),
                    "测试替身返回，不调用外部模型。",
                    "听起来不错，你这杯喝到最明显的风味是什么？",
                    ModelMessageType.CONVERSATION,
                    "听起来不错，你这杯喝到最明显的风味是什么？",
                    null,
                    new ConversationModelMessage(
                            List.of("这杯你喝到最明显的风味是什么？"),
                            List.of(
                                    new ConversationModelMessage.AnswerOption("citrus", "柑橘感", "我喝到比较明显的柑橘感。"),
                                    new ConversationModelMessage.AnswerOption("black_tea", "红茶感", "我喝到一点红茶感。"),
                                    new ConversationModelMessage.AnswerOption("not_sure", "说不清", "我暂时说不太清楚，只觉得整体比较干净。")
                            ),
                            List.of(new CopyVariant.FactUsage("主要风味仍需用户补充", "PENDING_ASSOCIATION", "model.routing", "", "LOW")),
                            List.of("当前信息不足，不能生成真实咖啡品鉴文案。")
                    ),
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    null,
                    null,
                    Instant.now()
            );
        }
        List<CopyVariant> variants = List.of(
                variant(CopyVariant.Style.RESTRAINED),
                variant(CopyVariant.Style.EXAGGERATED),
                variant(CopyVariant.Style.SHARP_REVIEW)
        );
        return new ModelResult(
                ModelMode.OPENAI_GPT55,
                "REAL_MODEL",
                "gpt-5.5",
                ModelMode.OPENAI_GPT55.displayName(),
                "测试替身返回，不调用外部模型。",
                "测试模型已生成三版文案。",
                ModelMessageType.POST,
                "测试模型已生成三版文案。",
                new PostModelMessage(variants, List.of()),
                null,
                List.of(),
                List.of(),
                variants,
                null,
                null,
                null,
                Instant.now()
        );
    }

    private CopyVariant variant(CopyVariant.Style style) {
        return new CopyVariant(style, style.label(), style.label() + "标题", style.label() + "正文", List.of("咖啡"), List.of(), List.of(), List.of(), List.of());
    }
}
