package com.minyuwei.xhs.coffeeagent.support;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelGateway;

import java.util.List;

public class FakeModelGateway implements ModelGateway {
    @Override
    public ModelResult complete(ModelRequest request) {
        return new ModelResult("FAKE:" + request.purpose() + ":" + String.join("|", request.facts()), List.of());
    }
}
