package com.minyuwei.xhs.coffeeagent.shared.infrastructure.redis;

public class RedisHealthConfig {
    private final String host;
    private final int port;

    public RedisHealthConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String healthSummary() {
        return "Redis reserved at " + host + ":" + port + "; core workflow does not depend on Redis.";
    }
}
