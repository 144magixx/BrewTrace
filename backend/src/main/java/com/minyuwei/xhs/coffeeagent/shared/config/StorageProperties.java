package com.minyuwei.xhs.coffeeagent.shared.config;

public record StorageProperties(String localRoot, String redisHost, int redisPort) {
    public static StorageProperties defaults() {
        return new StorageProperties(".local-storage", "localhost", 6379);
    }
}
