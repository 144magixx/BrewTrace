package com.minyuwei.xhs.coffeeagent.agent.application;

import com.minyuwei.xhs.coffeeagent.copywriting.domain.DraftCopy;

import java.util.List;

public class DraftAgent {
    public List<DraftCopy> generate(ContextAssembler.AgentContext context) {
        String facts = context.confirmedFacts().isEmpty() ? "这杯咖啡的已确认事实仍较少" : String.join("，", context.confirmedFacts());
        String boundary = "甜橙、青柠、葡萄柚仅作为待确认联想，不写成事实。";
        return List.of(
                DraftCopy.create(context.sessionId(), DraftCopy.Style.RESTRAINED, "一杯干净的水洗埃塞", facts + "。整体表达保持克制，" + boundary, List.of(boundary), List.of()),
                DraftCopy.create(context.sessionId(), DraftCopy.Style.EXAGGERATED, "柑橘光线落进红茶里", facts + "。可以联想到更明亮的柑橘光泽，但这仍是创作联想，" + boundary, List.of(boundary), List.of()),
                DraftCopy.create(context.sessionId(), DraftCopy.Style.SHARP_REVIEW, "好喝，但别急着神化", facts + "。锐评版保留个性表达，同时把未确认风味放在候选区，" + boundary, List.of(boundary), List.of())
        );
    }
}
