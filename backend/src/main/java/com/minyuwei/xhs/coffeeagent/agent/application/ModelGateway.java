package com.minyuwei.xhs.coffeeagent.agent.application;

import java.util.List;

public interface ModelGateway {
    ModelResult complete(ModelRequest request);

    record ModelRequest(String purpose, String prompt, List<String> facts, List<String> boundaries) {
    }

    record ModelResult(String content, List<String> warnings) {
    }
}
