package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelGateway;

import java.util.List;

public class SpringAiModelGateway implements ModelGateway {
    @Override
    public ModelResult complete(ModelRequest request) {
        String content = "模型适配器占位响应：" + request.purpose() + "；事实=" + String.join("、", request.facts());
        return new ModelResult(content, List.of("当前环境未接入真实 Spring AI，使用离线适配器。"));
    }
}
