package com.minyuwei.xhs.coffeeagent.tasting.application;

import com.minyuwei.xhs.coffeeagent.tasting.domain.BagImageAsset;

import java.util.Map;

public class BagImageExtractionService {
    public BagImageAsset extract(String sessionId, String filePath, String mimeType) {
        return BagImageAsset.pending(sessionId, filePath, mimeType, Map.of(
                "name", "候选豆名",
                "origin", "候选产区",
                "process", "候选处理法"
        ));
    }
}
