package com.minyuwei.xhs.coffeeagent.agent.application;

import com.minyuwei.xhs.coffeeagent.copywriting.domain.DraftCopy;

import java.util.List;

public class ReviewAgent {
    public List<String> review(DraftCopy draft, List<String> unconfirmedFlavors) {
        if (draft.writesUnconfirmedFlavorAsFact(unconfirmedFlavors)) {
            return List.of("文案把未确认风味写成事实，需要修改。");
        }
        return List.of("事实边界通过：已确认事实与创作联想分开。");
    }
}
