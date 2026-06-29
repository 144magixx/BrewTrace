package com.minyuwei.xhs.coffeeagent.shared.infrastructure.kafka;

import java.util.List;

public class KafkaTopicConfig {
    public List<String> topics() {
        return List.of(
                "coffee.record.archived",
                "draft.set.generated",
                "tool.call.completed",
                "publishing.package.confirmed",
                "agent.trace.completed"
        );
    }
}
